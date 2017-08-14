/*
 * Copyright © 2016 - 2017 Copyright (c) Sprint, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.fpc.impl;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.NotificationPublishService;
import org.opendaylight.fpc.activation.cache.OpCache;
import org.opendaylight.fpc.activation.cache.StorageCacheUtils;
import org.opendaylight.fpc.activation.cache.transaction.EmptyBodyException;
import org.opendaylight.fpc.activation.cache.transaction.Transaction;
import org.opendaylight.fpc.activation.cache.transaction.Transaction.OperationStatus;
import org.opendaylight.fpc.activation.workers.ActivationThreadPool;
import org.opendaylight.fpc.activation.workers.ConfigureWorker;
import org.opendaylight.fpc.activation.workers.MonitorWorker;
import org.opendaylight.fpc.assignment.AssignmentManager;
import org.opendaylight.fpc.tenant.TenantManager;
import org.opendaylight.fpc.utils.ErrorTypeIndex;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.ConfigureBundlesInput;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.ConfigureBundlesInputBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.ConfigureBundlesOutput;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.ConfigureBundlesOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.ConfigureDpnInput;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.ConfigureDpnOutput;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.ConfigureInput;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.ConfigureInputBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.ConfigureOutput;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.ConfigureOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.ErrorTypeId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.OpHeader.OpType;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.OpInput;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.Result;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.configure.bundles.input.Bundles;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.configure.bundles.output.BundlesBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.configure.bundles.output.BundlesKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.instructions.Instructions;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.op.input.op_body.CreateOrUpdate;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.op.input.op_body.CreateOrUpdateBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.op.input.op_body.DeleteOrQuery;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.payload.Contexts;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.result.body.ResultType;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.result.body.result.type.CommonSuccess;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.result.body.result.type.CommonSuccessBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.result.body.result.type.DeleteSuccessBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.result.body.result.type.Err;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.result.body.result.type.ErrBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.fpc.config.rev160927.FpcConfig;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.Futures;

/**
 * Assignment Phase Implementation.
 */
public class FpcAssignmentPhaseImpl extends FpcagentServiceBase {
    private static final Logger LOG = LoggerFactory.getLogger(FpcAssignmentPhaseImpl.class);

    private static AtomicLong entrants = new AtomicLong(0L);
    private static AtomicLong enqueues = new AtomicLong(0L);

    /**
     * Constructor initializing the primary services.
     *
     * @param db - Data Broker
     * @param activationService - Activation Service
     * @param monitorService - Monitoring Service
     * @param notificationService - Notification Service
     * @param conf - Fpc Configuration
     */
    public FpcAssignmentPhaseImpl(DataBroker db, ActivationThreadPool activationService,
            MonitorWorker monitorService, NotificationPublishService notificationService, FpcConfig conf) {
        super(db, activationService, monitorService, notificationService, conf);
        LOG.info("FpcAssignmentPhaseImpl has been initialized");
    }

    @Override
    public Future<RpcResult<ConfigureOutput>> configure(ConfigureInput input) {
        LOG.debug("Configure has been called");
        LOG.debug("notification service = " + notificationService);
        long startTime = System.currentTimeMillis();
        long entries = entrants.incrementAndGet();
        long enqueueVal = 0;
        ResultType rt = null;
        Result res  = null;
        try {
            Transaction tx  = Transaction.newTransaction((OpInput) input, startTime);
            Err err = immediateChecks((OpInput) input, tx);
            rt = (err != null) ? err : executeAssignmentOperation((OpInput) input, tx);
            res = (rt != null) ? ((rt instanceof Err) ? Result.Err : Result.OkNotifyFollows) :
                            Result.OkNotifyFollows;

            if ((tx.getStatus() != OperationStatus.COMPLETED) &&
                    (tx.getStatus() != OperationStatus.FAILED)) {
                switch (input.getOpType()) {
                    case Create:
                    case Update:
                        try {
                            activationService.getWorker().getQueue()
                                .put(new AbstractMap.SimpleEntry<Transaction,Object>(tx,
                                    new ConfigureInputBuilder(input)
                                        .setOpBody(new CreateOrUpdateBuilder((CreateOrUpdate)input.getOpBody())
                                                .setContexts(((CommonSuccess)rt).getContexts())
                                                .build())
                                        .build()));
                            enqueueVal = enqueues.incrementAndGet();
                        } catch (InterruptedException e) {
                            rt = activationServiceInterrupted(e,tx, System.currentTimeMillis() - startTime);
                        }
                        break;
                    default: // Delete
                        try {
                            activationService.getWorker().getQueue().put(
                                    new AbstractMap.SimpleEntry<Transaction,Object>(tx,input));
                        } catch (InterruptedException e) {
                            rt = activationServiceInterrupted(e,tx, System.currentTimeMillis() - startTime);
                        }
                }
            } else if (input.getOpType() == OpType.Query) {
                res = Result.Ok;
            }

        } catch (EmptyBodyException  e) {
            res = Result.Err;
            rt = new ErrBuilder()
                    .setErrorTypeId(new ErrorTypeId(ErrorTypeIndex.MESSAGE_WITH_NO_BODY))
                    .setErrorInfo("SYSTEM - operation failed - No Body was sent with this message.")
                    .build();
        } catch (IllegalArgumentException  ee) {
            res = Result.Err;
            rt = new ErrBuilder()
                    .setErrorTypeId(new ErrorTypeId(ErrorTypeIndex.CLIENT_ID_NOT_REGISTERED))
                    .setErrorInfo("SYSTEM - operation failed - Could not find tenant for Client Id " + input.getClientId())
                    .build();
        }

        if ((entries % 100) == 0) {
            LOG.info("Entries = {} and enqueues = {}", entries, enqueueVal);
        }
        return Futures.immediateFuture(RpcResultBuilder.<ConfigureOutput>success(new ConfigureOutputBuilder()
                .setOpId(input.getOpId())
                .setResult(res)
                .setResultType(rt)).build());
    }

    @Override
    public Future<RpcResult<ConfigureBundlesOutput>> configureBundles(ConfigureBundlesInput input) {
        LOG.info("Configure-Bundles has been called");
        long startTime = System.currentTimeMillis();
        Long l = Transaction.getBundleLinkId();
        List<Transaction> txs = new ArrayList<Transaction>();
        ArrayList<org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.configure.bundles.output.Bundles> outputBundles =
                new ArrayList<org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.configure.bundles.output.Bundles>();
        ArrayList<org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.configure.bundles.input.Bundles> activationBundles =
                new ArrayList<org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.configure.bundles.input.Bundles>();
        // TODO - We really need a partial failure result
        for (Bundles op : (input.getBundles() != null) ? input.getBundles() : Collections.<Bundles>emptyList()) {
            long opStartTime = System.currentTimeMillis();
            ResultType rt = null;
            // TODO - Convert this to a bundleLink
            try {
                Transaction tx = Transaction.newTransaction((OpInput) input, l, opStartTime);
                txs.add(tx);
                Err err = immediateChecks((OpInput) input, tx);
                rt = (err != null) ? err : executeAssignmentOperation((OpInput) input, tx);

                if ((tx.getStatus() != OperationStatus.COMPLETED) &&
                        (tx.getStatus() != OperationStatus.FAILED)) {
                    switch (op.getOpType()) {
                        case Create:
                        case Update:
                                activationBundles.add(
                                        new org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.configure.bundles.input.BundlesBuilder(op)
                                        .setOpBody((new CreateOrUpdateBuilder((CreateOrUpdate)op.getOpBody())
                                                .setContexts(((CommonSuccess)rt).getContexts())
                                                .build()))
                                        .build());
                            break;
                        default: // Delete Or Query
                            activationBundles.add(
                                    new org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.configure.bundles.input.BundlesBuilder(op)
                                    .build());
                    }
                }

                BundlesBuilder bob = new BundlesBuilder()
                        .setResultType(rt)
                        .setOpId(op.getOpId())
                        .setKey(new BundlesKey(op.getOpId()));
                outputBundles.add(bob.build());
            } catch (EmptyBodyException  e) {
                rt = new ErrBuilder()
                        .setErrorTypeId(new ErrorTypeId(ErrorTypeIndex.MESSAGE_WITH_NO_BODY))
                        .setErrorInfo("SYSTEM - operation failed - No Body was sent with this message.")
                        .build();
            } catch (IllegalArgumentException  ee) {
                rt = new ErrBuilder()
                        .setErrorTypeId(new ErrorTypeId(ErrorTypeIndex.CLIENT_ID_NOT_REGISTERED))
                        .setErrorInfo("SYSTEM - operation failed - Could not find tenant for Client Id " + input.getClientId())
                        .build();
            }
            if (rt instanceof Err) {
                break;
            }
        }
        try {
            activationService.getWorker().getQueue().put(new AbstractMap.SimpleEntry<List<Transaction>,Object>(txs,
                    new ConfigureBundlesInputBuilder()
                        .setBundles(activationBundles)
                        .setHighestOpRefScope(input.getHighestOpRefScope())
                        .build()) );
        } catch (InterruptedException e) {
            activationServiceInterrupted(e, txs, System.currentTimeMillis() - startTime);
        }
        return Futures.immediateFuture(RpcResultBuilder.<ConfigureBundlesOutput>success(
                new ConfigureBundlesOutputBuilder()
                .setBundles(outputBundles)
                .build()).build());
    }

    /**
     * Executes Assignment Activities for the Transaction.  The ResultType is one of
     * an Error or for a
     * <ul>
     * <li>Create/Update a Success with updated Contexts, if any</li>
     * <li>Query the result of the operation if number of targets < TARGET_READ_LIMIT or</li>
     * <li>a Success with the original input otherwise</li>
     * </ul>
     *
     * @param input - Operation Input
     * @param tx - Transaction
     * @return ResultType including an Error or a Success with updated Contexts for a Create/Update, if any were updated.
     */
    private ResultType executeAssignmentOperation(
            OpInput input,
            Transaction tx) {
        long sysTime = System.currentTimeMillis();
        DeleteOrQuery doq = null;
        ResultType rt = null;
        switch (input.getOpType()) {
        case Create:
        case Update:
            // TODO - CLONING Will Be placed here when it is supported
             CreateOrUpdate cou = (CreateOrUpdate) input.getOpBody();

             TenantManager tenantMgr = TenantManager.getTenantManagerForClient(input.getClientId());
            AssignmentManager assignmentMgr = tenantMgr.getAssignmentManager();

            List<Contexts> inputs = (cou != null)  ?
                        ((cou.getContexts() != null) ? cou.getContexts() : null) :
                        Collections.<Contexts>emptyList();
            List<Contexts> returnVals = new ArrayList<Contexts>();

            for (Contexts context : inputs) {
                try {
                    Instructions instr = (context.getInstructions() != null) ?
                            context.getInstructions() : input.getInstructions();
                    if (instr != null) {
                        returnVals.add(assignmentMgr.assign(instr, context));
                    } else {
                        return processAssignmentError(new ErrorTypeId(ErrorTypeIndex.CONTEXT_ACTIVATION_FAIL),
                                "AGENT - operation failed - ERROR - Context Activation cannot occur on this Agent w/o Instructions present - ",
                                tx, System.currentTimeMillis() - sysTime);
                    }
                } catch (Exception e) {
                    return processAssignmentError(new ErrorTypeId(ErrorTypeIndex.CONTEXT_ACTIVATION_FAIL),
                            e,
                            "PROTOCOL - operation failed - ERROR - Context Activation - ",
                            tx, System.currentTimeMillis() - sysTime);
                }
            }

            rt = new CommonSuccessBuilder()
                    .setContexts(returnVals)
                    .build();
            tx.setStatus(OperationStatus.PENDING_ACTIVATION, System.currentTimeMillis() - sysTime);
            return rt;
        case Query:
            doq = (DeleteOrQuery) input.getOpBody();
            try {
                if (doq.getTargets().size() > TARGET_READ_LIMIT) {
                    rt = new CommonSuccessBuilder()
                            .build();
                    tx.setStatus(OperationStatus.PENDING_ACTIVATION, System.currentTimeMillis() - sysTime);
                    return rt;
                } else {
                    OpCache resultCache = StorageCacheUtils.read(doq.getTargets(), tx.getTenantContext());
                    tx.completeAndClose(System.currentTimeMillis() - sysTime);
                    return resultCache.getConfigSuccess();
                }
            } catch (Exception e) {
                return processAssignmentError(new ErrorTypeId(ErrorTypeIndex.QUERY_FAILURE),
                        e,
                        "PROTOCOL - operation failed - ERROR - Query Failed -",
                        tx, System.currentTimeMillis() - sysTime);
            }
        case Delete:
            tx.setStatus(OperationStatus.PENDING_ACTIVATION, System.currentTimeMillis() - sysTime);
            return new DeleteSuccessBuilder((DeleteOrQuery) input.getOpBody()).build();
        default:
            return processAssignmentError(new ErrorTypeId(ErrorTypeIndex.DELETE_WO_PAYLOAD),
                    "PROTOCOL - operation failed - An unknown / unsuported OpType was sent.  " +
                            "Code MUST use pre-check and did not.",
                    tx, System.currentTimeMillis() - sysTime);
        }
    }

	@Override
	public Future<RpcResult<ConfigureDpnOutput>> configureDpn(ConfigureDpnInput input) {
		// unused
		return null;
	}
}
