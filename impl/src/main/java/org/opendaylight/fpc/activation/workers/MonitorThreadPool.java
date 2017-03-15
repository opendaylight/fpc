/*
 * Copyright © 2016 Copyright (c) Sprint, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.fpc.activation.workers;

import java.util.concurrent.PriorityBlockingQueue;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.fpc.utils.AbstractThreadPool;

import com.google.common.base.Supplier;

/**
 * Monitor Worker Threadpool Manager / Factory.
 */
public class MonitorThreadPool extends AbstractThreadPool<MonitorWorker> {
    static private int DEFAULT_Q_SIZE = 1024;

    /**
     * Constructor.
     * @param db - DataBroker
     * @param poolSize - Thread pool size
     */
    public  MonitorThreadPool(DataBroker db, int poolSize) {
        super(db,poolSize);
    }

    @Override
    protected Supplier<? extends MonitorWorker> getPoolFactory(DataBroker db) {
        return new MonitorFactory(db);
    }

    private class MonitorFactory implements Supplier<MonitorWorker> {
        private DataBroker db;

        /**
         * Constructor.
         * @param db - DataBroker
         */
        private MonitorFactory(DataBroker db) {
            this.db = db;
        }

        @Override
        public MonitorWorker get() {
            return new MonitorWorker(db, new PriorityBlockingQueue<Object>(DEFAULT_Q_SIZE));
        }
    }

}
