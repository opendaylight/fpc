/*
 * Copyright © 2016 - 2017 Copyright (c) Sprint, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.fpc.utils.eventStream;

import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.fpc.utils.AbstractThreadPool;
import com.google.common.base.Supplier;

/**
 * Creates a thread pool of NBEventWorker
 */
public class NBEventPool extends AbstractThreadPool<NBEventWorker> {
	static private NBEventPool _instance;

    public static void createInstance(int poolSize) {
        _instance = new NBEventPool(poolSize);
    }


    public static NBEventPool getInstance() {
        return _instance;
    }


	protected NBEventPool(int poolSize) {
		super(null, poolSize);
	}

	@Override
	protected Supplier<? extends NBEventWorker> getPoolFactory(DataBroker db) {
		return new NBEventWorkerFactory(startSignal);
	}

	/**
     * NBEventWorkerFactory Factory Supplier
     */
    protected class NBEventWorkerFactory implements Supplier<NBEventWorker> {
        protected final CountDownLatch startSignal;

        /**
         * Constructor.
         * @param startSignal - threadpool start signal
         */
        public NBEventWorkerFactory(CountDownLatch startSignal) {
            this.startSignal = startSignal;
        }

        @Override
        public NBEventWorker get() {
            return new NBEventWorker(startSignal,new LinkedBlockingQueue<Map.Entry<String,Map.Entry<String,String>>>());
        }
    }

}
