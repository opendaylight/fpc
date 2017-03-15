/*
 * Copyright © 2016 Copyright (c) Sprint, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.fpc.activation.cache.transaction;

import static java.lang.Double.doubleToLongBits;
import static java.lang.Double.longBitsToDouble;

import java.math.BigInteger;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.fpc.dpn.DpnHolder;
import org.opendaylight.fpc.tenant.TenantManager;
import org.opendaylight.fpc.utils.ErrorLog;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.fpc.rev150105.TxStats;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.fpc.rev150105.TxStatsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.fpc.rev150105.tx.stats.States;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.fpc.rev150105.tx.stats.StatesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.fpc.rev150105.tx.stats.StatesKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.fpc.rev150105.tx.stats.states.AverageTimeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.fpc.rev150105.tx.stats.states.TotalTimeBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility that stores Metrics and writes them to Storage.
 */
public class Metrics implements AutoCloseable {
    static private Metrics _instance;
    static private Thread _writerThread;
    static private MetricsWriter _writer;

    /**
     * Initilizer.
     * @param dataBroker - Data Broker
     * @param sleepTimer - Thread Sleep Interval
     */
    static public void init(DataBroker dataBroker, long sleepTimer) {
        _writer = new MetricsWriter(dataBroker, sleepTimer);
        _writerThread = new Thread(_writer);
        _writerThread.setDaemon(true);
        _writerThread.start();
        _instance = new Metrics();
    }

    /**
     * Returns Class Instance
     * @return Metrics Class instance
     */
    static public Metrics getInstance() {
        return _instance;
    }

    /**
     * Resets all metrics.
     */
    static public void clear() {
        _instance.init();
    }

    AtomicLong numTxs;
    Map<String,AtomicInteger> stampTotals;
    Map<Transaction.OperationStatus, AtomicLong> stateEntrants;
    Map<Transaction.OperationStatus, AtomicLong> stateRuntimes;
    AtomicLong completionRuntimes;

    /**
     * Intializes metrics.
     */
    protected void init() {
        numTxs = new AtomicLong(0);
        stampTotals = new ConcurrentHashMap<String,AtomicInteger>();
        stateEntrants = new ConcurrentHashMap<Transaction.OperationStatus, AtomicLong>();
        stateRuntimes = new ConcurrentHashMap<Transaction.OperationStatus, AtomicLong>();
        for(Transaction.OperationStatus stat : Transaction.OperationStatus.values()) {
            stateEntrants.put(stat, new AtomicLong(0));
            stateRuntimes.put(stat, new AtomicLong(0));
        }
        completionRuntimes = new AtomicLong(0);
    }

    /**
     * Default Constructor.
     */
    protected Metrics() {
        init();
    }

    /**
     * Set Metrics for a newly Created Transaction.
     */
    public void newTransactionCreated() {
        numTxs.incrementAndGet();
        stateEntrants.get(Transaction.OperationStatus.PENDING_ASSIGNMENT).incrementAndGet();
    }

    /**
     * Adds Metric Information.
     * @param newState - New State
     * @param oldState - Old State
     * @param runtime - Runtime between old and new state
     * @param totalRutime - Total Transaction Runtime
     */
    public void addData(Transaction.OperationStatus newState, Transaction.OperationStatus oldState, double runtime,
            long totalRutime) {
        stateEntrants.get(newState).incrementAndGet();
        stateRuntimes.get(oldState).set(doubleToLongBits (
                    longBitsToDouble(stateRuntimes.get(oldState).get()) + runtime)
                );
        if (newState == Transaction.OperationStatus.COMPLETED) {
            this.completionRuntimes.addAndGet(totalRutime);
        }
    }

    /**
     * Retrieves all State values
     * @return List of States
     */
    public List<States> getStates() {
        List<States> retVal = new ArrayList<States>();

        // General Transaction States
        for (Transaction.OperationStatus status : stateEntrants.keySet()) {
            if (stateEntrants.get(status) != null) {
                States someState  = createState(new StatesBuilder(), "Total Runtime for Completed Transactions",
                    stateEntrants.get(status).get(), (stateRuntimes.get(status) != null) ? stateRuntimes.get(status)
                        .get() : null);
                if (someState != null) {
                    retVal.add(someState);
                }
            }
        }

        // Completion Runtimes
        AtomicLong completions = stateEntrants.get(Transaction.OperationStatus.COMPLETED);
        if (completions != null) {
            States completionState  = createState(new StatesBuilder(), "Total Runtime for Completed Transactions",
                    completions.longValue(), completionRuntimes.get());
            if (completionState != null) {
                retVal.add(completionState);
            }
        }

        // Activation Statistics
        Long rx = 0L, tx = 0L;
        for (TenantManager tmgrs : ((TenantManager.getTenantsState() != null) ? TenantManager.getTenantsState().values() :
                Collections.<TenantManager>emptyList()) ) {
            for (Entry<String, DpnHolder> item : ((tmgrs.getDpnInfo() != null) ? tmgrs.getDpnInfo().entrySet() :
                Collections.<String, DpnHolder>emptyMap().entrySet())) {
                DpnHolder holder = item.getValue();
                if (holder != null) {
                    rx += holder.activator.rxMessages();
                    tx += holder.activator.txMessages();
                }
            }
        }
        States activatorRx  = createState(new StatesBuilder(), "Total Activator Rx Transactions",
                rx, 0L);
        if (activatorRx != null) {
            retVal.add(activatorRx);
        }
        States activatorTx  = createState(new StatesBuilder(), "Total Activator Tx Transactions",
                rx, 0L);
        if (activatorTx != null) {
            retVal.add(activatorTx);
        }

        return retVal;
    }

    /**
     * Creates a State Object
     * @param sb - Builder
     * @param stateName - State Name
     * @param entries - Number of Entries
     * @param runtime - Runtime
     * @return State
     */
    private States createState(StatesBuilder sb, String stateName, Long entries, Long runtime) {
        sb.setState(stateName)
          .setKey(new StatesKey(stateName))
          .setEntries(BigInteger.valueOf(entries));

        if (runtime != null) {
            double val = Double.longBitsToDouble(runtime);
            int exponent = Math.getExponent(val);
            double mantissa = val / Math.scalb(1.0, exponent);
            if (mantissa < 0) {
                mantissa = 0;
            }
            sb.setTotalTime(new TotalTimeBuilder()
                    .setExponent(exponent)
                    .setMantissa(BigInteger.valueOf((long)mantissa))
                    .build());
            val = (entries > 0) ? runtime / entries: 0;
            exponent = Math.getExponent(val);
            mantissa = val / Math.scalb(1.0, exponent);
            if (mantissa < 0) {
                mantissa = 0;
            }
            sb.setAverageTime(new AverageTimeBuilder()
                    .setExponent(exponent)
                    .setMantissa(BigInteger.valueOf((long)mantissa))
                    .build());
        }
        return sb.build();
    }

    @Override
    public void close() throws Exception {
        if (_writer != null) {
            _writer.halt();
        }
    }

    public void setSleepTimer(long duration) {
        _writer.setSleepTimer(duration);
    }

    /**
     * Class used to write datat to Storage.
     */
    static protected class MetricsWriter implements Runnable {
        private static final Logger LOG = LoggerFactory.getLogger(MetricsWriter.class);
        private DataBroker dataBroker;
        private boolean run;
        private long sleepTimer;
        private DateFormat df;

        /**
         * Halts the thread
         */
        public void halt() {
            this.run = false;
        }

        /**
         * Constructor.
         * @param dataBroker - Data Broker
         * @param sleepTimer - Sleep Interval
         */
        public MetricsWriter(DataBroker dataBroker, long sleepTimer) {
            this.dataBroker = dataBroker;
            this.sleepTimer = sleepTimer;
            df = new SimpleDateFormat("dd/MM/yy HH:mm:ss");
        }

        /**
         * Sets the sleep interval.
         * @param timer - Sleep Interval
         */
        public void setSleepTimer(long timer) {
            this.sleepTimer = timer;
        }

        @Override
        public void run() {
            this.run = true;
            LOG.info("MetricsWriter RUN started");
            while(run) {
                try {
                    Metrics m = Metrics.getInstance();
                    if (m != null) {
                        WriteTransaction wtrans = dataBroker.newWriteOnlyTransaction();
                        wtrans.put(LogicalDatastoreType.OPERATIONAL,
                                InstanceIdentifier.create(TxStats.class),
                                new TxStatsBuilder()
                                    .setLastTs(df.format(new Date(System.currentTimeMillis())))
                                    .setTotalTxs(BigInteger.valueOf(m.numTxs.get()))
                                    .setStates(m.getStates())
                                    .build());
                        wtrans.submit();
                    }
                    Thread.sleep(sleepTimer);
                } catch (InterruptedException e) {
                	ErrorLog.logError(e.getLocalizedMessage(),e.getStackTrace());
                }
            }
        }
    }

}
