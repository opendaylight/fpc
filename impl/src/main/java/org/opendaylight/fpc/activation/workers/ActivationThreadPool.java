/*
 * Copyright © 2016 - 2017 Copyright (c) Sprint, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.fpc.activation.workers;

import java.util.AbstractMap;
import java.util.Comparator;
import java.util.concurrent.PriorityBlockingQueue;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.fpc.activation.cache.transaction.Transaction;
import org.opendaylight.fpc.utils.AbstractThreadPool;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.OpHeader.OpType;

import com.google.common.base.Supplier;

/**
 * Activation Threadpool Manager / Factory.
 */
public class ActivationThreadPool extends AbstractThreadPool<ConfigureWorker> {
    static private int DEFAULT_Q_SIZE = 1024;

    /**
     * Constructor.
     * @param db - DataBroker
     * @param poolSize - Thread pool size
     */
    public ActivationThreadPool(DataBroker db, int poolSize) {
        super(db,poolSize);
    }

    @Override
    protected Supplier<? extends ConfigureWorker> getPoolFactory(DataBroker db) {
        return new ConfigureFactory(db);
    }

    /**
     * Internal factory class.
     */
    private class ConfigureFactory implements Supplier<ConfigureWorker> {
        protected DataBroker db;

        /**
         * Constructor.
         * @param db - DataBroker
         */
        private ConfigureFactory(DataBroker db) {
            this.db = db;
        }

        @Override
        public ConfigureWorker get() {
            return new ConfigureWorker(db, new PriorityBlockingQueue<Object>(DEFAULT_Q_SIZE, new QComparator()));
        }
    }

    /**
     * Queue comparator class for work prioritization.
     */
    private class QComparator implements Comparator<Object> {
        @SuppressWarnings("unchecked")
        @Override
        public int compare(Object o1, Object o2) {
            //TODO - This won't fly for CONF_BUNDLES
            Transaction o1t = (Transaction) ((AbstractMap.SimpleEntry<Object,Object>) o1).getKey();
            Transaction o2t = (Transaction) ((AbstractMap.SimpleEntry<Object,Object>) o2).getKey();

            if ((o1t.getOpInput().getOpType() == OpType.Create) &&
                    (o2t.getOpInput().getOpType() != OpType.Create))
                return 1;

            if ((o1t.getOpInput().getOpType() != OpType.Create) &&
                    (o2t.getOpInput().getOpType() == OpType.Create))
                return -1;

            return 0;
        }
    }
}
