/*
 * Copyright © 2016 - 2017 Copyright (c) Sprint, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.fpc.impl.memcached;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.fpc.impl.zeromq.ZMQSBMessagePool;
import org.opendaylight.fpc.utils.AbstractThreadPool;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.OpHeader.OpType;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcbase.rev160803.FpcContext;

import com.google.common.base.Supplier;

public class MemcachedThreadPool extends AbstractThreadPool<MemcachedWorker>{
	static private MemcachedThreadPool _instance;
	protected String memcachedUri;
	public MemcachedThreadPool(int poolSize, String memcachedUri) {
		super(null, poolSize);
		this.memcachedUri = memcachedUri;
	}

	public static void createInstance(int poolSize, String memcachedUri) {
		_instance = new MemcachedThreadPool(poolSize,memcachedUri);
	}

	public static MemcachedThreadPool getInstance() {
		return _instance;
	}

	@Override
	protected Supplier<? extends MemcachedWorker> getPoolFactory(DataBroker db) {
		return new MemcachedWorkerFactory(startSignal);
	}

	protected class MemcachedWorkerFactory implements Supplier<MemcachedWorker> {
        protected final CountDownLatch startSignal;

        /**
         * Constructor.
         * @param startSignal - threadpool start signal
         */
        public MemcachedWorkerFactory(CountDownLatch startSignal) {
            this.startSignal = startSignal;
        }

        @Override
        public MemcachedWorker get() {
            return new MemcachedWorker(startSignal,new LinkedBlockingQueue<>(),memcachedUri);
        }
    }

}
