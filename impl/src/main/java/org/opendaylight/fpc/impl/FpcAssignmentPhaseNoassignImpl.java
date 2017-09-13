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
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Future;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.NotificationPublishService;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.fpc.activation.cache.transaction.EmptyBodyException;
import org.opendaylight.fpc.activation.cache.transaction.Transaction;
import org.opendaylight.fpc.activation.cache.transaction.Transaction.OperationStatus;
import org.opendaylight.fpc.activation.workers.ActivationThreadPool;
import org.opendaylight.fpc.activation.workers.ConfigureWorker;
import org.opendaylight.fpc.activation.workers.MonitorWorker;
import org.opendaylight.fpc.policy.BasePolicyManager;
import org.opendaylight.fpc.utils.ErrorLog;
import org.opendaylight.fpc.utils.ErrorTypeIndex;
import org.opendaylight.fpc.utils.NameResolver;
import org.opendaylight.fpc.utils.NameResolver.FixedType;
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
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.Payload;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.Result;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.Tenants;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.configure.bundles.input.Bundles;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.configure.bundles.output.BundlesBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.configure.bundles.output.BundlesKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.op.input.OpBody;
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
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.tenants.Tenant;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.tenants.TenantKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.tenants.tenant.FpcMobility;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.tenants.tenant.FpcTopology;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.tenants.tenant.fpc.mobility.Ports;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.tenants.tenant.fpc.mobility.PortsBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.tenants.tenant.fpc.mobility.PortsKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.tenants.tenant.fpc.topology.Dpns;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.tenants.tenant.fpc.topology.DpnsBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.tenants.tenant.fpc.topology.DpnsKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcbase.rev160803.FpcIdentity;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcbase.rev160803.targets.value.Targets;
import org.opendaylight.yang.gen.v1.urn.opendaylight.fpc.config.rev160927.FpcConfig;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;

/**
 * FPC Assignment Phase implementation that does not perform assignments (support for non-assigning Clients).
 *
 */
public class FpcAssignmentPhaseNoassignImpl extends FpcagentServiceBase {
    private static final Logger LOG = LoggerFactory.getLogger(FpcAssignmentPhaseNoassignImpl.class);

    private static AtomicLong entrants = new AtomicLong(0L);
    private static AtomicLong enqueues = new AtomicLong(0L);
    private static final Pattern pattern = Pattern.compile("/ietf-dmm-fpcagent:tenants/tenant/[^ /]+/fpc-mobility/contexts/([^ /]+)");
    /**
     * Constructor initializing the primary services.
     *
     * @param db - Data Broker
     * @param activationService - Activation Service
     * @param monitorService - Monitoring Service
     * @param notificationService - Notification Service
     * @param conf - Fpc Configuration
     */
    public FpcAssignmentPhaseNoassignImpl(DataBroker db, ActivationThreadPool activationService,
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
        ConfigureWorker worker = null;
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
                        	OpBody opBody = ((OpInput)input).getOpBody();
                        	if( opBody instanceof CreateOrUpdate){
                        		//IF port - write port to db (TODO move this to a better location)
                        		if(((CreateOrUpdate) opBody).getPorts() != null){
                        			String defaultTenant = FpcProvider.getInstance().getConfig().getDefaultTenantId();
                        			FpcIdentity defaultIdentity = (defaultTenant == null) ?  new FpcIdentity(0L) :  new FpcIdentity(defaultTenant);
                        			DataBroker dataBroker = FpcProvider.getInstance().getDataBroker();
                        			WriteTransaction writetx = dataBroker.newWriteOnlyTransaction();
                        			org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.payload.Ports payloadPort = ((CreateOrUpdate) opBody).getPorts().get(((CreateOrUpdate) opBody).getPorts().size()-1);
                        			Ports port = new PortsBuilder().setPortId(payloadPort.getPortId())
                        					.setPolicyGroups(payloadPort.getPolicyGroups())
                        					.setKey(new PortsKey(payloadPort.getPortId())).build();
                        			
                        			if(dataBroker != null){
                        				LOG.info("Creating Port...");
                        				writetx.put(LogicalDatastoreType.OPERATIONAL,
                        					InstanceIdentifier.builder(Tenants.class)
                        					.child(Tenant.class, new TenantKey(defaultIdentity))
                        					.child(FpcMobility.class)
                        						.child(Ports.class, new PortsKey(port.getPortId()))
                        						.build(),
                        					new PortsBuilder(port).build());
                        				
                        				CheckedFuture<Void,TransactionCommitFailedException> submitFuture = writetx.submit();
                                        rt = new CommonSuccessBuilder( ((Payload)input.getOpBody()) ).build();

                        				Futures.addCallback(submitFuture, new FutureCallback<Void>() {
                        					@Override
                        					public void onFailure(Throwable arg0) {
                        						LOG.warn("Port create failed");
                        					}
                        					@Override
                        					public void onSuccess(Void arg0) {
                        						// Do nothing
                        					}
                        				});
                        			}
                                    break;
                        		}
                        		for(Contexts context : ((CreateOrUpdate)opBody).getContexts()){
                        			Entry<ConfigureWorker, ArrayList<Contexts>> entry = sessionMap.get(NameResolver.extractString(context.getContextId()));
                        			if(entry != null){
                        				if(entry.getKey() != null && input.getOpType().equals(OpType.Update)){
                        					entry.getKey().getQueue().put(new AbstractMap.SimpleEntry<Transaction,Object>(tx,
                                                input));
                            				tx.setStatusTs(OperationStatus.UPDATE, System.currentTimeMillis());
                            				tx.setStatusTs(OperationStatus.ACTIVATION_ENQUEUE, System.currentTimeMillis());
                                            enqueueVal = enqueues.incrementAndGet();
                        				} else if(entry.getKey() != null && input.getOpType().equals(OpType.Create)) {
                        					ErrorLog.logError("Create session received for a session that was already created. Session id - "+context.getContextId().toString());
                        					tx.setStatusTs(OperationStatus.ERRORED_CREATE, System.currentTimeMillis());
                        				}
                        			}
//                        			Check with Jacob - Is CP ready to receive an error on creates
//                        			else if(workerQueue != null && input.getOpType().equals(OpType.Create)) {
                        				//tx.fail(System.currentTimeMillis());
                        				//res = Result.Err;
//                        	            rt = new ErrBuilder()
//                        	                    .setErrorTypeId(new ErrorTypeId(ErrorTypeIndex.SESSION_ALREADY_EXISTS))
//                        	                    .setErrorInfo("SYSTEM - operation failed - This session has already been created.")
//                        	                    .build();
                        	            //break;
//                        			}
                        			else {
                        				if(input.getOpType().equals(OpType.Create)){
                        					worker = activationService.getWorker();
                        					sessionMap.put(NameResolver.extractString(context.getContextId()), new AbstractMap.SimpleEntry<ConfigureWorker, ArrayList<Contexts>>(worker,null));
                        					worker.getQueue().put(new AbstractMap.SimpleEntry<Transaction,Object>(tx,
                        							input));
                        					tx.setStatusTs(OperationStatus.CREATE, System.currentTimeMillis());
                        					tx.setStatusTs(OperationStatus.ACTIVATION_ENQUEUE, System.currentTimeMillis());
                                            enqueueVal = enqueues.incrementAndGet();
                        				}
                        				else if(input.getOpType().equals(OpType.Update)){
                        					tx.setStatusTs(OperationStatus.ERRORED_UPDATE, System.currentTimeMillis());
                        					ErrorLog.logError("Update received for a session which hasn't been created yet. Session Id - "+context.getContextId().toString());
                        				}
                        			}
                        		}
                        	}
//                            activationService.getWorker().getQueue()
//                                .put(new AbstractMap.SimpleEntry<Transaction,Object>(tx,
//                                    input));
                            rt = new CommonSuccessBuilder( ((Payload)input.getOpBody()) ).build();
                        } catch (Exception e) {
                            rt = activationServiceInterrupted(e,tx, System.currentTimeMillis() - startTime);
                        }
                        break;
                    default: // Delete
                        try {
                        	if(((OpInput)input).getOpType().equals(OpType.Delete)){
                        		for(Targets target: ((DeleteOrQuery)((OpInput)input).getOpBody()).getTargets()){
                        			Entry<FixedType, String> entry = extractTypeAndId(NameResolver.extractString(target.getTarget()));
                        			if(entry.getKey().equals(FixedType.CONTEXT))
                        			if(sessionMap.get(entry.getValue()) != null){
                        				sessionMap.get(entry.getValue()).getKey().getQueue().put(
                                                new AbstractMap.SimpleEntry<Transaction,Object>(tx,input));
                        				tx.setStatusTs(OperationStatus.ACTIVATION_ENQUEUE, System.currentTimeMillis());
                        				tx.setStatusTs(OperationStatus.DELETE, System.currentTimeMillis());
                        			}
                        		}
                        	}
//                            activationService.getWorker().getQueue().put(
//                                    new AbstractMap.SimpleEntry<Transaction,Object>(tx,input));

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

    	LOG.info("Configure Stop: "+System.currentTimeMillis());
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

	@Override
	public Future<RpcResult<ConfigureDpnOutput>> configureDpn(ConfigureDpnInput input) {
		// unused
		return null;
	}
    public Map.Entry<FixedType, String> extractTypeAndId(String restconfPath) {
        for (Map.Entry<FixedType, Map.Entry<Pattern,Integer>> p : NameResolver.entityPatterns.entrySet()) {
            Matcher m = p.getValue().getKey().matcher(restconfPath);
            if (m.matches()) {
                return new AbstractMap.SimpleEntry<FixedType, String>(p.getKey(), m.group(1));
            }
        }
        return null;
    }
}
