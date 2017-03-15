/*
 * Copyright © 2016 Copyright (c) Sprint, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.fpc.notification;

import java.util.AbstractMap;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.fpc.utils.AbstractThreadPool;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Uri;
import org.opendaylight.yangtools.yang.binding.Notification;

import com.google.common.base.Supplier;

/**
 * Client Pool Manager for HTTP Northbound notifications.
 */
public class HTTPClientPool extends AbstractThreadPool<HTTPNotifier> {
    public static HTTPClientPool instance;

    /**
     * Singleton Initialization call.
     * 
     * @param poolsize - thread pool size
     * @return HTTP Northbound Client Pool
     */
    public static HTTPClientPool init(int poolsize) {
        if (instance == null) {
            instance = new HTTPClientPool(poolsize);
        }
        return instance;
    }

    /**
     * Singleton retrieval creation.
     * @return HTTP Northbound Client Pool
     */
    public static HTTPClientPool instance() {
        return instance;
    }

    /**
     * Constructor.
     *  
     * @param poolsize - thread pool size
     */
    private HTTPClientPool(int poolSize) {
        super(null,poolSize);
    }

    @Override
    protected Supplier<? extends HTTPNotifier> getPoolFactory(DataBroker db) {
        return new HTTPNotifierFactory(startSignal);
    }

    /**
     * Internal Factory Class
     */
    protected class HTTPNotifierFactory implements Supplier<HTTPNotifier> {
        protected BlockingQueue<Object> unifiedBlockingQueue;
        protected CountDownLatch startSignal;

        /**
         * Constructor
         * @param startSignal - countdown latch start signal
         */
        public HTTPNotifierFactory(CountDownLatch startSignal) {
            this.unifiedBlockingQueue = new LinkedBlockingQueue<Object>();
            this.startSignal = startSignal;
        }

        @Override
        public HTTPNotifier get() {
            return new HTTPNotifier(startSignal, new
                    LinkedBlockingQueue<AbstractMap.SimpleEntry<Uri,Notification>>());
        }
    }
}
