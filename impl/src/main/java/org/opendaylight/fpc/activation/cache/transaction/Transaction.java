/*
 * Copyright © 2016 Copyright (c) Sprint, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.fpc.activation.cache.transaction;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

import org.opendaylight.fpc.activation.cache.PayloadCache;
import org.opendaylight.fpc.activation.cache.StorageCache;
import org.opendaylight.fpc.notification.Notifier;
import org.opendaylight.fpc.tenant.TenantManager;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.ClientIdentifier;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.OpIdentifier;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.OpInput;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.OpStatusValue.OpStatus;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.Payload;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.op.input.op_body.DeleteOrQuery;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.payload.Contexts;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.result.body.ResultType;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.result.body.result.type.DeleteSuccessBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcbase.rev160803.targets.value.Targets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Basic Transaction that wraps the OpInput information and provides transaction life cycle support.
 */
public class Transaction {
    private static final Logger LOG = LoggerFactory.getLogger(Transaction.class);
    private static Map<String, Transaction> transactions = new HashMap<String, Transaction>();

    private static Map<Long, Map.Entry<LongAdder,List<Transaction>>> bundles =
            new HashMap<Long, Map.Entry<LongAdder,List<Transaction>>>();
    private static AtomicLong bundleLinkId = new AtomicLong(0L);

    /**
     * Provides a unique Bundle Identifier
     * @return Long - Unique Bundle Identifier
     */
    public static Long getBundleLinkId() {
        if (bundleLinkId.longValue() == Long.MAX_VALUE) {
            bundleLinkId.set(0L);
        }
        return bundleLinkId.incrementAndGet();
    }

    /**
     * Creates a Transaction for given Client / Operation Identity
     * @param clientId - Client Identifier
     * @param opId - Operation Identifier
     * @return Transaction
     */
    public static Transaction get(ClientIdentifier clientId,
            OpIdentifier opId) {
        return get(clientId.toString() + "/" + opId.toString());
    }

    /**
     * Creates a Transaction for the given Client / Operation information that is part of a
     * bundle.
     * @param clientId - Client Identifier
     * @param bundleId - Bundle Unique Identifier
     * @param opId - Operation Identifier
     * @return Transaction
     */
    public static Transaction get(ClientIdentifier clientId,
            long bundleId,
            OpIdentifier opId) {
        return transactions.get(clientId.toString() + "/" + bundleId + "/" + opId.toString());
    }

    /**
     * Provides Transaction based upon provided key.
     * Keys should be
     * - {@code <client id> + <operation id>} for simple Transactions
     * - {@code <client id> + <bundle id> + <operation id>} for Bundle operations
     * @param key - String key to identify the Transaction
     * @return Transaction or null if not present
     */
    public static Transaction get(String key) {
        return transactions.get(key);
    }

    /**
     * Purges an operation from the system.
     * @param clientId - Client Identifier
     * @param opId - Operation Identifier
     */
    public static void purgeOperation(ClientIdentifier clientId, OpIdentifier opId) {
        purgeOperation(clientId.toString() + "/" + opId.toString());
    }

    /**
     * Purge an Operation.
     * The key follows the same convention as key in {@link #get(String)}.
     * @param key - Key of the operation to purge
     */
    private static void purgeOperation(String key) {
            transactions.remove(key);
    }

    /**
     * Purges a Bundle from the system.
     * @param bundleid - Bundle Identifier
     */
    public static void purgeBundle(Long bundleid) {
        if (bundleid == null) {
            return;
        }
        Map.Entry<LongAdder,List<Transaction>> bundleInfo =  bundles.get(bundleid);
        if (bundleInfo == null) {
            return;
        }
        long ts = System.currentTimeMillis();
        for(Transaction t : bundleInfo.getValue()) {
            t.setStatusTs(OperationStatus.FAILED, ts);
            purgeOperation(t.getClientId().toString() + "/" + bundleid + "/" + t.getOpId().toString());
        }
        bundles.remove(bundleid);
    }

    /**
     * An enumeration representing the various stages of a transaction life cycle.
     */
    public enum OperationStatus {
        PENDING_ASSIGNMENT,
        PENDING_ACTIVATION,
        AWAITING_RESPONSES,
        AWAITING_CACHE_WRITE,
        DISPATCHING_NOTIFICATION,
        COMPLETED,
        FAILED
    };

    private OpInput input;
    private PayloadCache pc;
    private ResultType rt;
    private OperationStatus status;
    private Long bundleLink;
    private LongAdder countDown;
    private long firstTs;
    private long lastTs;
    private TenantManager tenantMgr;

    /**
     * Creates a new Operation.
     * @param input - Operation Input
     * @param startTime - The start time of the transaction
     * @return Transaction representing a simple Operation
     * @throws EmptyBodyException - when input is null
     */
    static public Transaction newTransaction(OpInput input, long startTime)
            throws EmptyBodyException {
        return new Transaction(input, startTime);
    }

    /**
     * Creates an operation that is part of a bundle.
     * @param input - Operation Input
     * @param bundleLink - Bundle Identifier
     * @param startTime - The start time of the transaction
     * @return Transaction that is part of a Bundle Operation
     * @throws EmptyBodyException - when input is null
     */
    static public Transaction newTransaction(OpInput input, Long bundleLink, long startTime)
            throws EmptyBodyException {
        Map.Entry<LongAdder,List<Transaction>> bundleInfo =  (bundleLink != null) ? bundles.get(bundleLink) : null;
        if (bundleInfo == null) {
            bundleInfo = new AbstractMap.SimpleEntry<LongAdder,List<Transaction>>(new LongAdder(),
                    new ArrayList<Transaction>());
            bundles.put(bundleLink, bundleInfo);
        }
        return (bundleLink != null) ? new Transaction(input, bundleLink, startTime, bundleInfo) :
                    new Transaction(input, startTime);
    }

    /**
     * Initializes object.
     * @param input - Operation Input
     * @param bundleLink - Bundle Identifier
     * @param startTime - The start time of the transaction
     * @throws EmptyBodyException - when input is null
     */
    protected void init(OpInput input, Long bundleLink, long startTime) throws EmptyBodyException {
        if (input == null) {
            throw new EmptyBodyException("No Body provided for transaction");
        }
        tenantMgr = TenantManager.getTenantManagerForClient(input.getClientId());
        if (tenantMgr == null) {
            throw new IllegalArgumentException("Tenant Manager could not be found for " + input.getClientId());
        }
        this.input = input;
        this.bundleLink = bundleLink;
        this.status = OperationStatus.PENDING_ASSIGNMENT;
        int elementCount = (bundleLink == null) ? 0 : 1;
        if (this.input.getOpBody() instanceof Payload) {
            Payload p = (Payload)input.getOpBody();
            for (Contexts c: (p.getContexts() == null) ? Collections.<Contexts>emptyList() : p.getContexts()) {
                elementCount += (c.getDpns() == null) ? 0 : c.getDpns().size();
            }
        } else if (input.getOpBody() instanceof DeleteOrQuery) {
            elementCount = ((DeleteOrQuery)input.getOpBody()).getTargets().size();
        }
        this.countDown = new LongAdder();
        countDown.add(elementCount);
        this.firstTs = this.lastTs = startTime;
        Metrics.getInstance().newTransactionCreated();
    }

    /**
     * Bundle operation constructor.
     * @param input - Operation Input
     * @param startTime - The start time of the transaction
     * @throws EmptyBodyException - when input is null
     */
    protected Transaction(OpInput input, long startTime) throws EmptyBodyException {
        init(input, null, startTime);
        transactions.put(input.getClientId() + "/" + input.getOpId().toString(), this);
    }

    /**
     * Constructor.
     * @param input - Operation Input
     * @param bundleLink - Bundle Identifier
     * @param startTime - The start time of the transaction
     * @param bundleInfo - Associated Bundle information.
     * @throws EmptyBodyException - when input is null
     */
    protected Transaction(OpInput input, Long bundleLink, long startTime,
            Map.Entry<LongAdder,List<Transaction>> bundleInfo) throws EmptyBodyException {
        init(input, bundleLink, startTime);
        transactions.put(input.getClientId() + "/" + input.getOpId().toString(), this);
        bundleInfo.getKey().add(1);;
        ((ArrayList<Transaction>)bundleInfo.getValue()).add(this);
    }

    /**
     * Returns the Tenant Manager associated with the Transaction.
     * @return TenantManager
     */
    public TenantManager getTenantContext() {
        return tenantMgr;
    }

    /**
     * Returns the Client Identifier associated with the Transaction.
     * @return ClientIdentifier
     */
    public ClientIdentifier getClientId() {
        return input.getClientId();
    }

    /**
     * Returns the Operation Identifier associated with the Transaction.
     * @return OpIdentifier
     */
    public OpIdentifier getOpId() {
        return input.getOpId();
    }

    /**
     * Returns the Status associated with the Transaction.
     * @return OperationStatus
     */
    public OperationStatus getStatus() {
        return this.status;
    }

    /**
     * Returns the Opeation Input associated with the Transaction.
     * @return OpInput
     */
    public OpInput getOpInput() {
        return this.input;
    }

    /**
     * Marks a Transaction as complete
     * @param ts - Timestamp of completion
     * @param markings - number of subtasks completed
     */
    public void complete(long ts, long markings) {
        countDown.add(-1*markings);
        if (countDown.longValue() == 0) {
            publish();
        }
    }

    /**
     * Marks a Transaction as complete
     * @param ts - Timestamp of completion
     */
    public void complete(long ts) {
        countDown.increment();
        if (countDown.longValue() == 0) {
            publish();
        }
    }

    /**
     * Marks a Transaction as complete and closes it.
     * @param ts - Timestamp of completion
     */
    public void completeAndClose(long ts) {
        setStatusTs(OperationStatus.COMPLETED, ts);
        close();
    }

    /**
     * Publishes a Transaction.
     */
    public void publish() {;
        publish(true);
    }

    /**
     * Publishes a Transaction.
     * @param write - indicates if a cache write should occur.
     */
    public void publish(boolean write) {
        if (write) {
            writeToCache();
        }
        setStatusTs(OperationStatus.DISPATCHING_NOTIFICATION, System.currentTimeMillis());
        Notifier.issueConfigResult(this.getClientId(),
            this.getOpId(),
            OpStatus.Ok,
            rt,
            true); // TOOD - Make this a Config Variable
        completeAndClose(System.currentTimeMillis());
    }

    /**
     * Writes the Transaction to Cache.
     */
    protected void writeToCache() {
        setStatusTs(OperationStatus.AWAITING_CACHE_WRITE, System.currentTimeMillis());
        switch (input.getOpType()) {
            case Create:
            case Update:
                rt = tenantMgr.getSc().addToCache(pc).getConfigSuccess();
                break;
            case Delete:
                StorageCache sc = tenantMgr.getSc();

                DeleteOrQuery doq = (DeleteOrQuery) input.getOpBody();
                for (Targets target : (doq.getTargets() != null) ? doq.getTargets() :
                    Collections.<Targets>emptyList()) {
                    if (target.getTarget().getInstanceIdentifier() != null) {
                            sc.remove(target.getTarget().getInstanceIdentifier().toString());
                    } else {
                            sc.remove(((target.getTarget().getString() != null) ? target.getTarget().getString() :
                                target.getTarget().getUint32().toString()));
                    }
                }
                rt = new DeleteSuccessBuilder().setTargets(doq.getTargets()).build();
                break;
            default:
                break;
        }
    }

    /**
     * Sets the payload cache for the Transaction.
     * @param oCache - payload cache
     */
    public void setPayloadCache(PayloadCache oCache) {
        pc = oCache;
    }

    /**
     * Returns the ReadCace
     * @return PayloadCache
     */
    public PayloadCache getReadCache() {
        return pc;
    }

    /**
     * Sets the status of the transaction
     * @param nextState - The new transaction state
     * @param duration - duration spent in the previous state
     */
    public void setStatus(OperationStatus nextState, long duration) {
        Metrics.getInstance().addData(nextState, this.status, duration, lastTs - firstTs);
        this.status = nextState;
    }

    /**
     * Sets the status of the transaction
     * @param nextState - The new transaction state
     * @param ts - Timestamp of the state transition
     */
    public void setStatusTs(OperationStatus nextState, long ts) {
        Metrics.getInstance().addData(nextState, this.status, ts - lastTs, lastTs - firstTs);
        this.status = nextState;
        this.lastTs = ts;
    }

    /**
     * Fails the transaction.
     */
    public void fail() {
        setStatusTs(OperationStatus.FAILED, System.currentTimeMillis());
        Notifier.issueConfigResult(this.getClientId(),
                this.getOpId(),
                OpStatus.Ok,
                rt,
                true);
        close();
    }

    /**
     * Fails the transaction.
     * @param duration - Time spent in the previous state.
     */
    public void fail(long duration) {
        setStatus(OperationStatus.FAILED, duration);
        close();
    }

    /**
     * Closes the Transaction.
     */
    private void close() {
        if (bundleLink == null) {
            LOG.debug("Closing Transaction: " +  this.getOpId());
            purgeOperation(this.getClientId(), this.getOpId());
        } else {
            LOG.debug("Closing Transaction: " +  this.bundleLink + "/" + this.getOpId());
            purgeOperation(this.getClientId().toString() + "/" + this.bundleLink + "/" + this.getOpId().toString());
        }
    }

    /**
     * Increases the amount of tasks that must be finished prior to the Transaction being completed.
     * @param tasks - the number of tasks to increase
     */
    public void addTaskCount(int tasks) {
        countDown.add(tasks);
    }

    /**
     * Sets the Result Type.  This is used for Query operations
     * @param rt - ResultType value
     */
    public void setResultType (ResultType rt) {
        this.rt = rt;
    }
}
