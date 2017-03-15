/*
 * Copyright © 2016 Copyright (c) Sprint, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.fpc.activation.cache;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.fpc.utils.ErrorLog;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.tenants.tenant.FpcMobility;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility Class to Write StorageCache from the memory cache to storage.
 */
public class StorageWriter implements AutoCloseable {
    static private StorageWriter _instance;
    static private Thread _writerThread;
    static private StorageWriterThread _writer;

    /**
     * Initialization.
     * @param dataBroker - DataBroker
     * @param sleepTimer - Interval between checks for writes
     */
    static public void init(DataBroker dataBroker, long sleepTimer) {
        _writer = new StorageWriterThread(dataBroker, sleepTimer);
        _writerThread = new Thread(_writer);
        _writerThread.setDaemon(true);
        _writerThread.start();
        _instance = new StorageWriter();
    }

    /**
     * Provides the Class Instance
     * @return StorageWriter instance
     */
    static public StorageWriter getInstance() {
        return _instance;
    }

    /**
     * Removes all Storage Caches from the instance.
     */
    static public void clear() {
        _instance.init();
    }

    /**
     * Adds a Storage Cache
     * @param sc - Storage Cache
     * @return true if this set did not already contain the specified element
     */
    public boolean addCache(StorageCache sc) {
        return _instance.caches.add(sc);
    }

    /**
     * Removes a Storage Cache
     * @param sc - Storage Cache
     * @return true if this set contained the specified element
     */
    public boolean removeCache(StorageCache sc) {
        return _instance.caches.remove(sc);
    }

    private Set<StorageCache> caches;

    /**
     * Initializes cache.
     */
    private void init() {
        caches = new HashSet<StorageCache>();
    }

    /**
     * Default Constructor.
     */
    protected StorageWriter() {
        init();
    }

    @Override
    public void close() throws Exception {
        if (_writer != null) {
            _writer.halt();
        }
    }

    /**
     * Provides the Sleep Timer
     * @param duration - duration to be applied for the write check interval
     */
    public void setSleepTimer(long duration) {
        _writer.setSleepTimer(duration);
    }

    /**
     * Thread that performs the check after each interval on all Caches.  If any Cace is dirty it is written
     * to Storage.
     */
    static private class StorageWriterThread implements Runnable {
        private static final Logger LOG = LoggerFactory.getLogger(StorageWriterThread.class);
        private DataBroker dataBroker;
        private boolean run;
        private long sleepTimer;

        /**
         * Halts the thread.
         */
        private void halt() {
            this.run = false;
        }
        /**
         * Constructor
         * @param dataBroker - Data Broker
         * @param sleepTimer - Sleep Interval between Write checks.
         */
        private StorageWriterThread(DataBroker dataBroker, long sleepTimer) {
            this.dataBroker = dataBroker;
            this.sleepTimer = sleepTimer;
            this.run = true;
        }

        /**
         * Provides the Sleep Timer
         * @param timer - duration to be applied for the write check interval
         */
        private void setSleepTimer(long timer) {
            this.sleepTimer = timer;
        }

        @Override
        public void run() {
            this.run = true;
            LOG.info("StorageWriter RUN started");
            while((run) && (sleepTimer > 0)) {
                try {
                    StorageWriter sw = StorageWriter.getInstance();
                    if (sw != null) {
                        for (StorageCache sc : sw.caches) {
                            if (sc.isDirty) {
                                // Write to Mobility Tree to OPERATIONAL
                                LOG.info("Writing cache update for tenant");
                                WriteTransaction wtrans0 = dataBroker.newWriteOnlyTransaction();
                                Map.Entry<InstanceIdentifier<FpcMobility>, FpcMobility> mobTree = sc.getMobilityTree();
                                if (mobTree != null) {
                                    sc.snapshotComplete();
                                    wtrans0.put(LogicalDatastoreType.OPERATIONAL, mobTree.getKey(),
                                            mobTree.getValue(), true);
                                    wtrans0.submit();
                                }
                            }
                        }
                    }
                    Thread.sleep(sleepTimer);
                } catch (InterruptedException e) {
                    ErrorLog.logError(e.getLocalizedMessage(),e.getStackTrace());
                }
            }
        }
    }
}
