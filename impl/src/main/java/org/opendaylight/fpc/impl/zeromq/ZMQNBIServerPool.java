/*
 * Copyright © 2016 - 2017 Copyright (c) Sprint, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.fpc.impl.zeromq;

import org.opendaylight.fpc.utils.FpcCodecUtils;
import org.opendaylight.netconf.sal.restconf.api.JSONRestconfService;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.ConfigureInput;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.OperationFailedException;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.common.RpcError;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeromq.ZContext;
import org.zeromq.ZFrame;
import org.zeromq.ZMQ;
import org.zeromq.ZMQ.Socket;
import org.zeromq.ZMsg;

import com.google.common.base.Optional;
import com.google.common.base.Throwables;

/**
 * Zero MQ Northbound Service Thread Pool - untested.
 */
public class ZMQNBIServerPool {
    private static final Logger LOG = LoggerFactory.getLogger(ZMQNBIServerPool.class);
    /**
     * QName
     */
    public static final QName TOP_ODL_FPC_QNAME =
            QName.create("urn:ietf:params:xml:ns:yang:fpcagent", "2016-08-03","config-result-notification").intern();
    static final YangInstanceIdentifier configResultNotificationYII =
            YangInstanceIdentifier.of(TOP_ODL_FPC_QNAME);
    static final InstanceIdentifier<ConfigureInput> configResultNotificationII =
            InstanceIdentifier.create(ConfigureInput.class);
    static final FpcCodecUtils fpcCodecUtils;
    static {
        try {
            fpcCodecUtils = FpcCodecUtils.get(ConfigureInput.class, configResultNotificationYII);
        } catch (Exception e) {
            LOG.error("Exception occured during FpcCodecUtilsInitialization");
            throw Throwables.propagate(e);
        }
    }

    private server_task server = null;

    /**
     * Primary Constructor.
     *
     * @param serverUri - Server URI
     * @param inprocUri - Intra-process URI
     * @param serverPoolsize - Server Pool Size
     * @param requestHanlderPoolSize - Request Handler Pool Size
     */
    public ZMQNBIServerPool(
            String serverUri,
            String inprocUri,
            int serverPoolsize,
            int requestHanlderPoolSize) {
        server = new server_task(requestHanlderPoolSize);
        new Thread(server).start();
    }

    /**
     * Server Task.
     */
    private static class server_task implements Runnable {
        int requestHanlderPoolSize;

        /**
         * Constructor.
         *
         * @param requestHanlderPoolSize - Request Handler Pool Size
         */
        public server_task(int requestHanlderPoolSize) {
            this.requestHanlderPoolSize = requestHanlderPoolSize;
        }

        @Override
        public void run() {
            ZContext ctx = new ZContext();

            //  Frontend socket talks to clients over TCP
            Socket frontend = ctx.createSocket(ZMQ.ROUTER);
            frontend.bind("tcp://*:5570");

            //  Backend socket talks to workers over inproc
            Socket backend = ctx.createSocket(ZMQ.DEALER);
            backend.bind("inproc://backend");

            //  Launch pool of worker threads, precise number is not critical
            for (int threadNbr = 0; threadNbr < requestHanlderPoolSize; threadNbr++) {
                try {
                    new Thread(new server_worker(ctx)).start();
                } catch (Exception e) {
                    // Does nothing as a warning was already issued.
                }
            }

            //  Connect backend to frontend via a proxy
            ZMQ.proxy(frontend, backend, null);

            LOG.info("ZMQNBIServerPool - Exiting MAIN server thread");
            frontend.unbind("tcp://*:5570");
            backend.unbind("inproc://backend");
            ctx.destroySocket(frontend);
            ctx.destroySocket(backend);
            ctx.destroy();
        }
    }

    /**
     * Worker Class.
     */
    private static class server_worker implements Runnable {
        private ZContext ctx;
        private JSONRestconfService service;

        /**
         * Constructor.
         *
         * @param ctx - ZMQ Context
         * @throws Exception - Throws Exception
         */
        public server_worker(ZContext ctx) throws Exception {
            this.ctx = ctx;
            Object[] instances =  FpcCodecUtils.getGlobalInstances(JSONRestconfService.class, this);
            service = (instances != null) ? (JSONRestconfService) instances[0] : null;
            if (service == null) {
                LOG.error("JSONRestconfService was NOT available at the moment of the worker's constructor");
                throw new Exception();
            }
        }

        @Override
        public void run() {
            Socket worker = ctx.createSocket(ZMQ.DEALER);
            worker.connect("inproc://backend");

            while (!Thread.currentThread().isInterrupted()) {
                //  A DEALER socket gives us the address envelope and message
                ZMsg msg = ZMsg.recvMsg(worker);
                long ts1 = 0, ts0 = System.currentTimeMillis();
                ZFrame address = msg.pop();
                ZFrame content = msg.pop();
                ZFrame response = null;
                assert (content != null);
                msg.destroy();

                long ts2 = System.currentTimeMillis();
                String request = new String(content.getData());
                content.destroy();
                long ts3 = System.currentTimeMillis();
                assert (request != null);
                long ts4 = System.currentTimeMillis();
                int index = request.indexOf(' ');
                long ts5 = System.currentTimeMillis();
                try {
                    Optional<String> output = service.invokeRpc(request.substring(0, index),
                            Optional.of(request.substring(index+1)));

                    ts1 = System.currentTimeMillis();

                    if (output.isPresent()) {
                        response = new ZFrame(output.get());
                    }
                } catch (OperationFailedException e) {
                    if (e.getErrorList().get(0) instanceof RpcError) {
                        RpcError rpcError = (RpcError) e.getErrorList().get(0);
                        String errVal = "{\"errors\":{\"error\":[{\"error-type\":\"" + rpcError.getErrorType().toString() +
                                "\",\"error-tag\":\"" + rpcError.getTag() +
                                "\",\"error-message\":\"" + rpcError.getMessage() +
                                "\",\"error-info\":\"<severity>" + rpcError.getSeverity().toString() + "</severity>\"}]}}";
                        response = new ZFrame(errVal);
                    }
                }
                if (response != null) {
                    address.send(worker, ZFrame.REUSE + ZFrame.MORE);
                    response.send(worker, ZFrame.DONTWAIT);
                }
                // TODO - Turn ZNQ NBI invocation time into a metric
                LOG.info("Total Invocation Time = {}\n", (ts1-ts0));
                LOG.info("Invocation Times = {}\n, \t{}\n, \t{}\n, \t{}\t, \t{}\n", (ts2-ts0), (ts3-ts2),
                        (ts4-ts3), (ts5-ts4), (ts1-ts5));

            }
            worker.disconnect("inproc://backend");
            worker.close();
            ctx.destroySocket(worker);
        }
    }
}
