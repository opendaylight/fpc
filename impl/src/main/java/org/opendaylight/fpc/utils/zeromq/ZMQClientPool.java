/*
 * Copyright © 2016 Copyright (c) Sprint, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.fpc.utils.zeromq;

import java.nio.ByteBuffer;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.fpc.utils.AbstractThreadPool;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;

import com.google.common.base.Supplier;

/**
 * Client Pool Manager for ZMQ Client notifications.
 */
public class ZMQClientPool extends AbstractThreadPool<ZMQClientSocket> {
    static private ZMQClientPool _instance;

    /**
     * Singleton Initialization call.
     * 
     * @param context - ZConext
     * @param address - ZMQ Address
     * @param poolSize - thread pool size
     */
    public static void createInstance(ZContext context, String address, int poolSize) {
        _instance = new ZMQClientPool(context, address, poolSize);
    }

    /**
     * Singleton retrieval creation.
     * @return ZMQ Client Pool
     */
    public static ZMQClientPool getInstance() {
        return _instance;
    }

    private final ZContext context;
    private String address;

    /**
     * Constructor.
     * 
     * @param context - ZConext
     * @param address - ZMQ Address
     * @param poolSize - thread pool size
     */
    protected ZMQClientPool(ZContext context, String address, int poolSize) {
        super(null, poolSize);
        this.context = context;
        this.address = address;
    }

    /**
     * Returns Address of the ZMQ Client.
     * 
     * @return string representing the ZMQ address the Clients bind to.
     */
    public String getAddress() {
        return address;
    }

    /**
     * Returns Context of the ZMQ Client.
     * 
     * @return ZMQ Context
     */
    public ZContext getContext() {
        return context;
    }

    /**
     * Starts the threads in the Client pool.
     */
    public void start() throws Exception {
        startUp();
    }

    @Override
    protected Supplier<? extends ZMQClientSocket> getPoolFactory(DataBroker db) {
        return new ZMQClientSocketFactory(context, address, ZMQ.PUB, startSignal);
    }

    /**
     * ZMQ Client Factory Supplier
     */
    protected class ZMQClientSocketFactory implements Supplier<ZMQClientSocket> {
        protected final ZContext context;
        protected final String address;
        protected final int type;
        protected final CountDownLatch startSignal;
        
        /**
         * Constructor.
         * 
         * @param context - ZConext
         * @param address - ZMQ Address
         * @param type - ZMQ Socket Type
         * @param startSignal - threadpool start signal
         */
        public ZMQClientSocketFactory(ZContext context, String address, int type, CountDownLatch startSignal) {
            this.context = context;
            this.address = address;
            this.type = type;
            this.startSignal = startSignal;
        }

        @Override
        public ZMQClientSocket get() {
            // NOTE that Queues are NOT shared amongst sockets
            return new ZMQClientSocket(context, address, type, startSignal, new LinkedBlockingQueue<ByteBuffer>());
        }
    }
}
