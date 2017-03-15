/*
 * Copyright © 2016 Copyright (c) Sprint, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.fpc.impl.zeromq;

import java.util.Map;

import org.opendaylight.fpc.activation.impl.dpdkdpn.DpnAPIListener;
import org.opendaylight.fpc.dpn.DPNStatusIndication;
import org.opendaylight.fpc.monitor.EventMonitorMgr;
import org.opendaylight.fpc.utils.ErrorLog;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.notify.value.DownlinkDataNotification;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcbase.rev160803.FpcDpnId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;
import org.zeromq.ZMQ.Socket;

/**
 * ZMQ Northbound Interface Listener - untested.
 */
public class ZMQSBListener implements AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(ZMQSBListener.class);

    ZMQSBWorker broadcastSubscriber;
    ZMQSBWorker subscriber;
    private final String address;
    private final String subscriberId;
    private Thread broadcastWorker;
    private Thread generalWorker;

    /**
     * Constructor.
     * @param address - Address
     * @param subscriberId - ZMQ Subscription Id
     */
    public ZMQSBListener(String address,
            String subscriberId) {
        this.address = address;
        this.subscriberId = subscriberId;
        this.broadcastWorker = null;
        this.generalWorker = null;
    }

    @Override
    public void close() throws Exception {
        stop();
    }

    /**
     * Stops the Listener.
     */
    public void stop() {
        if (subscriber != null) {
            subscriber.stop();
        }
        if (broadcastSubscriber != null) {
            broadcastSubscriber.stop();
        }
    }

    /**
     * Opens the Listener resources.
     */
    public void open() {
        try {
            subscriber = new ZMQSBWorker(address, subscriberId);
            generalWorker = new Thread(subscriber);
            generalWorker.start();
            broadcastSubscriber = new ZMQSBWorker(address, DpnAPIListener.BROADCAST_TOPIC);
            broadcastWorker = new Thread(subscriber);
            broadcastWorker.start();
        } catch (Exception e) {
            ErrorLog.logError(e.getStackTrace());
        }
    }

    /**
     * Worker Class.
     */
    protected class ZMQSBWorker implements Runnable {
        private final ZContext ctx;
        private final String address;
        private final String subscriberId;
        private final DpnAPIListener dpnApi;
        private boolean run;

        /**
         * Constructor.
         * @param address - Address
         * @param subscriberId - ZMQ Subscription Id
         */
        public ZMQSBWorker(String address,
                String subscriberId) {
            ctx = new ZContext();
            run = true;
            this.address = address;
            this.subscriberId = subscriberId;
            dpnApi = new DpnAPIListener();
        }

        @Override
        public void run() {
            Socket subscriber = ctx.createSocket(ZMQ.SUB);
            subscriber.connect(address);
            subscriber.subscribe(subscriberId.getBytes());
            while ((!Thread.currentThread ().isInterrupted ()) &&
                    run) {
                String addr = subscriber.recvStr();
                String contents = subscriber.recvStr ();
                LOG.info(addr + " : " + contents);
                Map.Entry<FpcDpnId, Object> entry = dpnApi.decode(contents.getBytes());
                if (entry.getValue() instanceof DownlinkDataNotification) {
                    EventMonitorMgr.processEvent(entry.getKey(),(DownlinkDataNotification)entry.getValue());
                } else if (entry.getValue()  instanceof DPNStatusIndication) {
                    EventMonitorMgr.processEvent(entry.getKey(),(DPNStatusIndication)entry.getValue());
                    LOG.info("DPN Status Indication Change.\n Stats = {}", entry.toString());
                }
            }
            subscriber.disconnect(address);
            subscriber.close();
            ctx.destroySocket(subscriber);
            try {
                Thread.currentThread().join();
            } catch (InterruptedException e) {
            	ErrorLog.logError(e.getStackTrace());
            }
        }

        public void stop() {
            run = false;
        }
    }
}
