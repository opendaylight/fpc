/*
 * Copyright © 2016 - 2017 Copyright (c) Sprint, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.fpc.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.PriorityBlockingQueue;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.NotificationPublishService;
import org.opendaylight.fpc.activation.cache.StorageCache;
import org.opendaylight.fpc.activation.cache.transaction.Transaction;
import org.opendaylight.fpc.activation.workers.ActivationThreadPool;
import org.opendaylight.fpc.activation.workers.ConfigureWorker;
import org.opendaylight.fpc.activation.workers.MonitorWorker;
import org.opendaylight.fpc.utils.ErrorLog;
import org.opendaylight.fpc.utils.ErrorTypeIndex;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.ErrorTypeId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.EventDeregisterInput;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.EventDeregisterOutput;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.EventDeregisterOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.EventRegisterInput;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.EventRegisterOutput;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.EventRegisterOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.IetfDmmFpcagentService;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.OpInput;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.ProbeInput;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.ProbeOutput;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.ProbeOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.Result;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.OpHeader.OpType;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.op.input.op_body.CreateOrUpdate;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.op.input.op_body.DeleteOrQuery;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.payload.Contexts;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.result.body.result.type.Err;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.result.body.result.type.ErrBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcbase.rev160803.FpcContextId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.fpc.config.rev160927.FpcConfig;
import org.opendaylight.yangtools.yang.common.RpcError;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;

import com.google.common.util.concurrent.Futures;

/**
 * Abstract Base Class for Common IetfDmmFpcagentService functions.
 */
abstract public class FpcagentServiceBase implements IetfDmmFpcagentService, ConfigUpdateListener {
    protected final DataBroker db;
    protected ActivationThreadPool activationService;
    protected MonitorWorker monitorService;
    protected NotificationPublishService notificationService;
    protected int TARGET_READ_LIMIT = 10; // Default
    protected boolean assignmentManagerRequired = true;
    public static ConcurrentHashMap<String,Map.Entry<ConfigureWorker, ArrayList<Contexts>>> sessionMap = new ConcurrentHashMap<String,Map.Entry<ConfigureWorker, ArrayList<Contexts>>>();

    /**
     * Primary Constructor which initializes the common services of the plugin.
     *
     * @param db - Data Broker
     * @param activationService - Activation Service
     * @param monitorService - Monitoring Service
     * @param notificationService - Notification Service
     * @param conf - Fpc Configuration
     */
    public FpcagentServiceBase(DataBroker db,
    		ActivationThreadPool activationService,
            MonitorWorker monitorService,
            NotificationPublishService notificationService,
            FpcConfig conf) {
        this.db = db;
        this.activationService = activationService;
        this.monitorService = monitorService;
        this.notificationService = notificationService;
        this.TARGET_READ_LIMIT = conf.getTargetReadLimit().intValue();
    }

    @Override
    public void updateConf(FpcConfig conf) {
        this.TARGET_READ_LIMIT = conf.getTargetReadLimit().intValue();
    }

    /**
     * Indicates if an Assignment Manager is required.
     *
     * @return true if this service requires an Assignment Manager; folse otherwise
     */
    public boolean requiresAssignmentManager() {
        return assignmentManagerRequired;
    }

    /**
     * Generic Error Handler dealing with the Err Return value Generation and Transaction Management.
     * @param id - Error Type Identifier
     * @param message - String message
     * @param tx - Associated Transaction
     * @param duration - length of time passed since last timestamp
     * @return - An Err object
     */
    protected Err processAssignmentError(ErrorTypeId id,
            String message,
            Transaction tx,
            long duration) {
        tx.fail(duration);
        return new ErrBuilder()
                .setErrorTypeId(id)
                .setErrorInfo(message)
                .build();
    }

    /**
     * Generic Error Handler dealing with the Err Return value Generation, exception printing and Transaction
     * Management.
     * @param id - Error Type Identifier
     * @param e - Exception that occured
     * @param message - String message
     * @param tx - Associated Transaction
     * @param duration - length of time passed since last timestamp
     * @return An Err object
     */
    protected Err processAssignmentError(ErrorTypeId id,
            Exception e,
            String message,
            Transaction tx,
            long duration) {
        tx.fail(duration);
        ErrorLog.logError(e.getStackTrace());
        return new ErrBuilder()
                .setErrorTypeId(id)
                .setErrorInfo(message + e.getMessage())
                .build();
    }

    /**
     * ALL pre-checks from the RPC prior to process SHOULD Be done here.
     * @param input - Operation Input
     * @param tx - Associated Transaction
     * @return null if there are no issues OR a ResultType
     */
    protected Err immediateChecks(OpInput input, Transaction tx) {
        switch (input.getOpType()) {
        case Create:
            if (!(input.getOpBody() instanceof CreateOrUpdate)) {
                return processAssignmentError(new ErrorTypeId(ErrorTypeIndex.CREATE_UPDATE_WO_PAYLOAD),
                        "PROTOCOL - operation failed - A Create Request was sent without a Create/Update payload.",
                        tx, 0L);
            }
            break;
        case Update:
            if (!(input.getOpBody() instanceof CreateOrUpdate)) {
                return processAssignmentError(new ErrorTypeId(ErrorTypeIndex.CREATE_UPDATE_WO_PAYLOAD),
                        "PROTOCOL - operation failed - A Create Request was sent without a Create/Update payload.",
                        tx, 0L);
            }
            break;
        case Query:
            if (!(input.getOpBody() instanceof DeleteOrQuery)) {
                return processAssignmentError(new ErrorTypeId(ErrorTypeIndex.QUERY_WO_PAYLOAD),
                        "PROTOCOL - operation failed - A Query Request was sent without a DeleteOrQuery payload",
                        tx, 0L);
            }
            break;
        case Delete:
            if (!(input.getOpBody() instanceof DeleteOrQuery)) {
                return processAssignmentError(new ErrorTypeId(ErrorTypeIndex.DELETE_WO_PAYLOAD),
                        "PROTOCOL - operation failed - A Delete Request was sent without a DeleteOrQuery payload",
                        tx, 0L);
            }
            break;
        default:
            return processAssignmentError(new ErrorTypeId(ErrorTypeIndex.UNKNOWN_OP_TYPE),
                    "PROTOCOL - operation failed - A Delete Request was sent without a DeleteOrQuery payload",
                    tx, 0L);
        }
        return null;
    }

    /**
     * Generates and Error when the thread receives an InterruptedException for a CONF request.
     *
     * @param e - InterruptedException
     * @param tx - Transaction
     * @param duration - Duration of the activity (used for statistics)
     * @return - An Error (Result Type Error)
     */
    protected Err activationServiceInterrupted(Exception e,
            Transaction tx,
            long duration) {
        tx.fail(duration);
        ErrorLog.logError(e.getStackTrace());
        return new ErrBuilder()
            .setErrorTypeId(new ErrorTypeId(ErrorTypeIndex.CONTEXT_ACTIVATION_FAIL))
            .setErrorInfo("SYSTEM - operation failed - The downstream system's Activation has been interrupted.")
            .build();
    }

    /**
     * Generates and Error when the thread receives an InterruptedException for a CONF_BUNDLES request.
     *
     * @param e - InterruptedException
     * @param txs - List of Transactions
     * @param duration - Duration of the activity (used for statistics)
     * @return - An Error (Result Type Error)
     */
    protected Err activationServiceInterrupted(InterruptedException e,
            List<Transaction> txs,
            long duration) {
        for (Transaction tx : txs) {
            tx.fail(duration);
        }
        ErrorLog.logError(e.getStackTrace());
        return new ErrBuilder()
            .setErrorTypeId(new ErrorTypeId(ErrorTypeIndex.CONTEXT_ACTIVATION_FAIL))
            .setErrorInfo("SYSTEM - operation failed - The downstream system's Activation has been interrupted.")
            .build();
    }

    // Pre-defined, rpc specific responses.
    private static Future<RpcResult<EventDeregisterOutput>> eventDeregisterErr =
            Futures.immediateFuture(RpcResultBuilder.<EventDeregisterOutput>failed()
                    .withError(RpcError.ErrorType.PROTOCOL,
                            "protocol",
                            "No notificiation URI present for Client Id, please re-register")
                    .build());
    private static Future<RpcResult<EventDeregisterOutput>> eventDeregisterOk =
            Futures.immediateFuture(RpcResultBuilder.<EventDeregisterOutput>success()
                    .withResult(new EventDeregisterOutputBuilder().setMonitorResult(Result.Ok).build())
                    .build());

    private static Future<RpcResult<ProbeOutput>> eventProbeOk =
            Futures.immediateFuture(RpcResultBuilder.<ProbeOutput>success()
                    .withResult(new ProbeOutputBuilder().setMonitorResult(Result.Ok).build())
                    .build());
    private static Future<RpcResult<ProbeOutput>> eventProbeErr =
            Futures.immediateFuture(RpcResultBuilder.<ProbeOutput>failed()
                    .withError(RpcError.ErrorType.PROTOCOL,
                            "protocol",
                            "No notificiation URI present for Client Id, please re-register")
                    .build());

    private static Future<RpcResult<EventRegisterOutput>> eventRegisterOk =
            Futures.immediateFuture(RpcResultBuilder.<EventRegisterOutput>success()
                    .withResult(new EventRegisterOutputBuilder().setMonitorResult(Result.Ok).build())
                    .build());
    private static Future<RpcResult<EventRegisterOutput>> eventRegisterErr =
            Futures.immediateFuture(RpcResultBuilder.<EventRegisterOutput>failed()
                    .withError(RpcError.ErrorType.PROTOCOL,
                            "protocol",
                            "No notificiation URI present for Client Id, please re-register")
                    .build());

    @Override
    public Future<RpcResult<EventDeregisterOutput>> eventDeregister(EventDeregisterInput input) {
        if (FpcServiceImpl.getNotificationUri(input.getClientId()) == null) {
            return eventDeregisterErr;
        } else {
            monitorService.getQueue().add(input);
            return eventDeregisterOk;
        }
    }

    @Override
    public Future<RpcResult<ProbeOutput>> probe(ProbeInput input) {
        if (FpcServiceImpl.getNotificationUri(input.getClientId()) == null) {
            return eventProbeErr;
        } else {
            monitorService.getQueue().add(input);
            return eventProbeOk;
        }
    }

    @Override
    public Future<RpcResult<EventRegisterOutput>> eventRegister(EventRegisterInput input) {
        if (FpcServiceImpl.getNotificationUri(input.getClientId()) == null) {
            return eventRegisterErr;
        } else {
            monitorService.getQueue().add(input);
            return eventRegisterOk;
        }
    }
}
