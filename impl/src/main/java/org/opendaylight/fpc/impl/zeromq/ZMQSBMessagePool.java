/*
 * Copyright © 2016 - 2017 Copyright (c) Sprint, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.fpc.impl.zeromq;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.fpc.utils.AbstractThreadPool;
import com.google.common.base.Supplier;

/**
 * Creates a thread pool of ZMQSBMessageWorker
 */
public class ZMQSBMessagePool extends AbstractThreadPool<ZMQSBMessageWorker> {
	static private ZMQSBMessagePool _instance;
	protected String nodeId;
	protected String networkId;
    /**
     * Singleton Initialization call.
     * @param poolSize - thread pool size
     * @param nodeId - Controller Node Id
     * @param networkId - Controller Network Id
     */
    public static void createInstance(int poolSize, String nodeId, String networkId) {
        _instance = new ZMQSBMessagePool(poolSize,nodeId,networkId);
    }

    /**
     * Singleton retrieval creation.
     * @return ZMQ SB Message Pool
     */
    public static ZMQSBMessagePool getInstance() {
        return _instance;
    }

	/**
	 * Constructor
	 * @param poolSize - Number of threads in the pool
	 * @param nodeId - Node Id of the controller
	 * @param networkId - Network Id of the controller
	 */
	protected ZMQSBMessagePool(int poolSize, String nodeId, String networkId) {
		super(null, poolSize);
		this.nodeId = nodeId;
		this.networkId = networkId;
	}

	@Override
	protected Supplier<? extends ZMQSBMessageWorker> getPoolFactory(DataBroker db) {
		return new ZMQSBMessageWorkerFactory(startSignal);
	}

	/**
     * ZMQSBMessageWorker Factory Supplier
     */
    protected class ZMQSBMessageWorkerFactory implements Supplier<ZMQSBMessageWorker> {
        protected final CountDownLatch startSignal;

        /**
         * Constructor.
         * @param startSignal - threadpool start signal
         */
        public ZMQSBMessageWorkerFactory(CountDownLatch startSignal) {
            this.startSignal = startSignal;
        }

        @Override
        public ZMQSBMessageWorker get() {
            return new ZMQSBMessageWorker(startSignal,new LinkedBlockingQueue<byte[]>(),nodeId,networkId);
        }
    }

}
