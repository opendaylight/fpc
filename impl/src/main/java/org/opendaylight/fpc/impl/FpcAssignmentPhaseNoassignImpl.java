/*
 * Copyright © 2016 Copyright (c) Sprint, Inc. and others.  All rights reserved.
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
import org.opendaylight.fpc.activation.cache.transaction.EmptyBodyException;
import org.opendaylight.fpc.activation.cache.transaction.Transaction;
import org.opendaylight.fpc.activation.cache.transaction.Transaction.OperationStatus;
import org.opendaylight.fpc.activation.workers.ConfigureWorker;
import org.opendaylight.fpc.activation.workers.MonitorWorker;
import org.opendaylight.fpc.utils.ErrorTypeIndex;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.ConfigureBundlesInput;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.ConfigureBundlesInputBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.ConfigureBundlesOutput;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.ConfigureBundlesOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.ConfigureInput;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.ConfigureOutput;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.ConfigureOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.ErrorTypeId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.OpHeader.OpType;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.OpInput;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.Payload;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.Result;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.configure.bundles.input.Bundles;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.configure.bundles.output.BundlesBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.configure.bundles.output.BundlesKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.op.input.op_body.DeleteOrQuery;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.result.body.ResultType;
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
 * FPC Assignment Phase implementation that does not perform assignments (support for non-assigning Clients).
 *
 */
public class FpcAssignmentPhaseNoassignImpl extends FpcagentServiceBase {
    private static final Logger LOG = LoggerFactory.getLogger(FpcAssignmentPhaseNoassignImpl.class);

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
    public FpcAssignmentPhaseNoassignImpl(DataBroker db, ConfigureWorker activationService,
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
            rt = immediateChecks((OpInput) input, tx);
            res = (rt != null) ? ((rt instanceof Err) ? Result.Err : Result.OkNotifyFollows) :
                            Result.OkNotifyFollows;

            if ((tx.getStatus() != OperationStatus.COMPLETED) &&
                    (tx.getStatus() != OperationStatus.FAILED)) {
                switch (input.getOpType()) {
                    case Create:
                    case Update:
                        try {
                            activationService.getQueue()
                                .put(new AbstractMap.SimpleEntry<Transaction,Object>(tx,
                                    input));
                            enqueueVal = enqueues.incrementAndGet();
                            rt = new CommonSuccessBuilder( ((Payload)input.getOpBody()) ).build();
                        } catch (InterruptedException e) {
                            rt = activationServiceInterrupted(e,tx, System.currentTimeMillis() - startTime);
                        }
                        break;
                    default: // Delete
                        try {
                            activationService.getQueue().put(
                                    new AbstractMap.SimpleEntry<Transaction,Object>(tx,input));
                            rt = new DeleteSuccessBuilder((DeleteOrQuery) input.getOpBody()).build();
                        } catch (InterruptedException e) {
                            rt = activationServiceInterrupted(e,tx, System.currentTimeMillis() - startTime);
                        }
                }
            } else if (input.getOpType() == OpType.Query) {
                res = Result.Ok;
                rt = new DeleteSuccessBuilder((DeleteOrQuery) input.getOpBody()).build();
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
                rt = immediateChecks((OpInput) input, tx);

                if ((tx.getStatus() != OperationStatus.COMPLETED) &&
                        (tx.getStatus() != OperationStatus.FAILED)) {
                    switch (op.getOpType()) {
                        case Create:
                        case Update:
                                activationBundles.add(op);
                            break;
                        default: // Delete Or Query
                            activationBundles.add(op);
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
            activationService.getQueue().put(new AbstractMap.SimpleEntry<List<Transaction>,Object>(txs,
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
}
