/*
 * Copyright © 2016 - 2017 Copyright (c) Sprint, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.fpc.activation.workers;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.fpc.activation.cache.HierarchicalCache;
import org.opendaylight.fpc.activation.cache.OpCache;
import org.opendaylight.fpc.activation.cache.PayloadCache;
import org.opendaylight.fpc.activation.cache.StorageCache;
import org.opendaylight.fpc.activation.cache.StorageCacheUtils;
import org.opendaylight.fpc.activation.cache.transaction.Transaction;
import org.opendaylight.fpc.activation.cache.transaction.Transaction.OperationStatus;
import org.opendaylight.fpc.dpn.DpnHolder;
import org.opendaylight.fpc.impl.FpcProvider;
import org.opendaylight.fpc.impl.FpcagentDispatcher;
import org.opendaylight.fpc.impl.FpcagentServiceBase;
import org.opendaylight.fpc.impl.memcached.MemcachedThreadPool;
import org.opendaylight.fpc.tenant.TenantManager;
import org.opendaylight.fpc.utils.ErrorLog;
import org.opendaylight.fpc.utils.ErrorTypeIndex;
import org.opendaylight.fpc.utils.NameResolver;
import org.opendaylight.fpc.utils.NameResolver.FixedType;
import org.opendaylight.fpc.utils.Worker;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.ConfigureBundlesInput;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.ConfigureInput;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.ConfigureOutput;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.ConfigureOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.ErrorTypeId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.OpInput;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.Payload;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.RefScope;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.Tenants;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.OpHeader.OpType;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.configure.bundles.input.Bundles;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.op.input.op_body.CreateOrUpdate;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.op.input.op_body.DeleteOrQuery;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.payload.Contexts;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.result.body.ResultType;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.result.body.result.type.CommonSuccessBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.result.body.result.type.Err;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.result.body.result.type.ErrBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.tenants.Tenant;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.tenants.TenantKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.tenants.tenant.FpcMobility;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.tenants.tenant.FpcTopology;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.tenants.tenant.fpc.mobility.ContextsBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.tenants.tenant.fpc.mobility.ContextsKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.tenants.tenant.fpc.mobility.Ports;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.tenants.tenant.fpc.mobility.PortsBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.tenants.tenant.fpc.mobility.PortsKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.tenants.tenant.fpc.topology.DpnsKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcbase.rev160803.FpcContext;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcbase.rev160803.FpcContextId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcbase.rev160803.FpcDpnId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcbase.rev160803.FpcIdentity;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcbase.rev160803.FpcPort;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcbase.rev160803.fpc.context.Dpns;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcbase.rev160803.targets.value.Targets;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;

/**
 * Primary worker for CONF and CONF_BUNDLES activation.
 */
public class ConfigureWorker
implements Worker {
	private static final Logger LOG = LoggerFactory.getLogger(ConfigureWorker.class);
	private static final AtomicLong entrants = new AtomicLong(0L);
	private boolean run;
	private final BlockingQueue<Object> blockingConfigureQueue;

	//private final DataBroker db;

	/**
	 * Constructor.
	 * @param db - DataBroker
	 * @param blockingConfigureQueue - Work queue
	 */
	protected ConfigureWorker(DataBroker db, BlockingQueue<Object> blockingConfigureQueue) {
		//this.db = db;
		this.blockingConfigureQueue = blockingConfigureQueue;
		LOG.info("ConfigureWorker has been initialized");
	}

	/**
	 * Retrieves the work queue.
	 * @return - the work queue for this instance
	 */
	public BlockingQueue<Object> getQueue() {
		return blockingConfigureQueue;
	}

	/**
	 * Generic Error Handler dealing with the Err Return value Generation, exception printing and Transaction
	 * Management.
	 * @param id - Error Type Identifier
	 * @param e - Exception that occurred
	 * @param message - String message
	 * @param tx - Associated Transaction
	 * @param duration - length of time passed since last time stamp
	 * @return An Err object
	 */
	private Err processActivationError(ErrorTypeId id,
			Exception e,
			String message,
			Transaction tx,
			long duration) {
		String mess = (e != null) ? message + e.getMessage() : message;
		Err rt = new ErrBuilder()
				.setErrorTypeId(id)
				.setErrorInfo(mess)
				.build();
		tx.setResultType(rt);
		tx.fail(duration);
		if (e != null) {
			ErrorLog.logError(e.getStackTrace());
		}
		return rt;
	}
	
	/**
	 * Primary execution method for individual operations.
	 * @param oCache - Object Cache
	 * @param tx - Transaction
	 * @return a ResultType if an error occurs otherwise null
	 */
	private ResultType executeOperation(PayloadCache oCache,
			Transaction tx) {
		LOG.info("Configure Worker - Execute Operation");
		long sysTime = System.currentTimeMillis();
		OpInput input = tx.getOpInput();
		DpnHolder dpnInfo = null;

		DeleteOrQuery doq = null;
		switch (input.getOpType()) {
		case Create:
			for (Contexts context : (oCache.getPayloadContexts() == null) ? Collections.<Contexts>emptyList() : oCache.getPayloadContexts()) {
				for (Dpns dpn : (context.getDpns() == null) ? Collections.<Dpns>emptyList() : context.getDpns() ) {
					if(TenantManager.vdpnContextsMap.get(dpn.getDpnId()) != null)
						TenantManager.vdpnContextsMap.get(dpn.getDpnId()).put(context, tx);
					if(TenantManager.vdpnDpnsMap.get(dpn.getDpnId()) != null){
						if(TenantManager.vdpnDpnsMap.get(dpn.getDpnId()).size() <= 1){
							ErrorLog.logError("Not ready - "+dpn.getDpnId()+" must contain 2 DPNS", null);
							return processActivationError(new ErrorTypeId(ErrorTypeIndex.CONTEXT_ACTIVATION_FAIL),
									null,
									"PROTOCOL - operation failed - ERROR - Context Activation - ",
									tx,
									System.currentTimeMillis() - sysTime);
						}
					}
				}
			}
		case Update:
			for (Contexts context : (oCache.getPayloadContexts() == null) ? Collections.<Contexts>emptyList() : oCache.getPayloadContexts()) {
				try{
					if ((input.getOpType().equals(OpType.Create))) {
						try {

							if(MemcachedThreadPool.getInstance() != null){
								MemcachedThreadPool.getInstance().getWorker().getBlockingQueue().put(
										new AbstractMap.SimpleEntry<FpcContext,OpType>(context, OpType.Create));
							}
						} catch (Exception e) {
							ErrorLog.logError(e.getStackTrace());
						}
					}
					if(FpcagentServiceBase.sessionMap.get(NameResolver.extractString(context.getContextId())).getValue() != null) {
						FpcagentServiceBase.sessionMap.get(NameResolver.extractString(context.getContextId())).getValue().add(context);
					} else {
						ArrayList<Contexts> contextsArray = new ArrayList<Contexts>();
						contextsArray.add(context);
						FpcagentServiceBase.sessionMap.get(NameResolver.extractString(context.getContextId())).setValue(contextsArray);
					}
					for (Dpns dpn : (context.getDpns() == null) ? Collections.<Dpns>emptyList() : context.getDpns() ) {
						// if vdn, then you send the context to both actual dpn
						if(TenantManager.vdpnDpnsMap.get(dpn.getDpnId()) != null){
							if(TenantManager.vdpnDpnsMap.get(dpn.getDpnId()).size() == 1){
								LOG.warn("Only one Dpn in "+dpn.getDpnId());
							}else if(TenantManager.vdpnDpnsMap.get(dpn.getDpnId()).size() == 0){
								ErrorLog.logError("No DPN's found in "+dpn.getDpnId(), null);
								return processActivationError(new ErrorTypeId(ErrorTypeIndex.CONTEXT_ACTIVATION_FAIL),
										null,
										"PROTOCOL - operation failed - ERROR - Context Activation - ",
										tx,
										System.currentTimeMillis() - sysTime);
							}

							tx.setStatus(OperationStatus.AWAITING_RESPONSES, System.currentTimeMillis() - sysTime);
							for(FpcDpnId dpnId : TenantManager.vdpnDpnsMap.get(dpn.getDpnId())){
								dpnInfo = tx.getTenantContext().getDpnInfo().get(dpnId.toString());
								if (dpnInfo.activator != null) {
									try {
										dpnInfo.activator.activate(input.getClientId(), input.getOpId(), input.getOpType(), (context.getInstructions() != null) ?
												context.getInstructions() : input.getInstructions(), context, oCache);										
										//dpnInfo.activator.getResponseManager().enqueueChange(context, oCache, tx);
									} catch (Exception e) {
										return processActivationError(new ErrorTypeId(ErrorTypeIndex.CONTEXT_ACTIVATION_FAIL),
												e,
												"PROTOCOL - operation failed - ERROR - Context Activation - ",
												tx,
												System.currentTimeMillis() - sysTime);
									}
								} else {
									LOG.info("No activator found for DPN" + dpn.getDpnId().toString());
								}
							}

						}else {
							dpnInfo = tx.getTenantContext().getDpnInfo().get(dpn.getDpnId().toString());
							if (dpnInfo!=null && dpnInfo.activator != null) {
								try {
									dpnInfo.activator.activate(input.getClientId(), input.getOpId(), input.getOpType(), (context.getInstructions() != null) ?
											context.getInstructions() : input.getInstructions(), context, oCache);
									tx.setStatus(OperationStatus.AWAITING_RESPONSES, System.currentTimeMillis() - sysTime);
									//dpnInfo.activator.getResponseManager().enqueueChange(context, oCache, tx);
								} catch (Exception e) {
									return processActivationError(new ErrorTypeId(ErrorTypeIndex.CONTEXT_ACTIVATION_FAIL),
											e,
											"PROTOCOL - operation failed - ERROR - Context Activation - ",
											tx,
											System.currentTimeMillis() - sysTime);
								}
							} else {
								LOG.info("No activator found for DPN" + dpn.getDpnId().toString());
							}
						}
					}
				} catch (Exception e){
					ErrorLog.logError("Context - "+context.toString());
					ErrorLog.logError("dpnInfo map - "+tx.getTenantContext().getDpnInfo().toString());
					ErrorLog.logError(e.getMessage(),e.getStackTrace());
				}

			}

			return null;
			case Query:
				doq = (DeleteOrQuery) input.getOpBody();
				try {
					OpCache result = StorageCacheUtils.read(doq.getTargets(), tx.getTenantContext());
					tx.setResultType(result.getConfigSuccess());
					tx.complete(System.currentTimeMillis(),false);
					//tx.publish(false);
					return null;
				} catch (Exception e) {
					return processActivationError(new ErrorTypeId(ErrorTypeIndex.QUERY_FAILURE),
							e,
							"PROTOCOL - operation failed - ERROR - Query Failed - ",
							tx,
							System.currentTimeMillis() - sysTime);
				}
			case Delete:
				doq = (DeleteOrQuery) input.getOpBody();
				for (Targets target : (doq.getTargets() != null) ? doq.getTargets() :
					Collections.<Targets>emptyList()) {
					FpcDpnId ident = null;
					Entry<FixedType, String> entry = extractTypeAndId(NameResolver.extractString(target.getTarget()));
					Contexts context = null;
					ArrayList<Contexts> cList = FpcagentServiceBase.sessionMap.get(entry.getValue()).getValue();
					if(!cList.isEmpty()){
						context = cList.get(cList.size()-1);
					}

					if (context != null) {
						try {
							if(MemcachedThreadPool.getInstance() != null){
								MemcachedThreadPool.getInstance().getWorker().getBlockingQueue().put(
										new AbstractMap.SimpleEntry<FpcContext,OpType>(context, OpType.Delete));
							}
						} catch (Exception e) {
							ErrorLog.logError(e.getStackTrace());
						}
						if (context.getDpns() != null) {
							if (context.getDpns().size() > 1) {
								tx.addTaskCount(context.getDpns().size()-1);
							}
							for (Dpns dpn : context.getDpns()) {
								// if vdn, then you send the context to both actual dpn
								try{
									if(TenantManager.vdpnDpnsMap.get(dpn.getDpnId()) != null){
										if(TenantManager.vdpnDpnsMap.get(dpn.getDpnId()).size() == 1){
											LOG.warn("Only one Dpn in "+dpn.getDpnId());
										}else if(TenantManager.vdpnDpnsMap.get(dpn.getDpnId()).size() == 0){
											ErrorLog.logError("No DPN's found in "+dpn.getDpnId(), null);
											return processActivationError(new ErrorTypeId(ErrorTypeIndex.CONTEXT_ACTIVATION_FAIL),
													null,
													"PROTOCOL - operation failed - ERROR - Context Activation - ",
													tx,
													System.currentTimeMillis() - sysTime);
										}
										TenantManager.vdpnContextsMap.get(dpn.getDpnId()).remove(cList.get(0));
										tx.setStatus(OperationStatus.AWAITING_RESPONSES, System.currentTimeMillis() - sysTime);
										for(FpcDpnId dpnId : TenantManager.vdpnDpnsMap.get(dpn.getDpnId())){
											ident = dpnId;

											if (ident != null) {
												dpnInfo = tx.getTenantContext().getDpnInfo().get(dpnId.toString());
												if (dpnInfo.activator != null) {
													try {
														dpnInfo.activator.delete(input.getClientId(),input.getOpId(),input.getInstructions(), target, context);
														FpcagentServiceBase.sessionMap.remove(NameResolver.extractString(context.getContextId()));
														//dpnInfo.activator.getResponseManager().enqueueDelete(target, tx);
													} catch (Exception e) {
														return processActivationError(new ErrorTypeId(ErrorTypeIndex.DELETE_FAILURE),
																e,
																"PROTOCOL - operation failed - ERROR - Delete Failed - ",
																tx,
																System.currentTimeMillis() - sysTime);
													}
												}  else {
													LOG.info("No activator found for DPN" + dpnId.toString());
												}
											}
										}
									}else{
										ident = dpn.getDpnId();

										if (ident != null) {
											dpnInfo = tx.getTenantContext().getDpnInfo().get(dpn.getDpnId().toString());
											if (dpnInfo!=null && dpnInfo.activator != null) {
												try {
													dpnInfo.activator.delete(input.getClientId(),input.getOpId(),input.getInstructions(), target, context);
													tx.setStatus(OperationStatus.AWAITING_RESPONSES, System.currentTimeMillis() - sysTime);
													FpcagentServiceBase.sessionMap.remove(NameResolver.extractString(context.getContextId()));
													//dpnInfo.activator.getResponseManager().enqueueDelete(target, tx);
												} catch (Exception e) {
													return processActivationError(new ErrorTypeId(ErrorTypeIndex.DELETE_FAILURE),
															e,
															"PROTOCOL - operation failed - ERROR - Delete Failed - ",
															tx,
															System.currentTimeMillis() - sysTime);
												}
											}  else {
												LOG.info("No activator found for DPN" + dpn.getDpnId().toString());
											}
										}
									}
								} catch (Exception e){
									ErrorLog.logError("Context - "+context.toString());
									ErrorLog.logError("dpnInfo map - "+tx.getTenantContext().getDpnInfo().toString());
									ErrorLog.logError(e.getMessage(),e.getStackTrace());
								}
							}
						}
					} else {
						ErrorLog.logError("Context for delete not found. Target - "+target.getTarget().toString());
					}
				}

				return null;

			default:
				return processActivationError(new ErrorTypeId(ErrorTypeIndex.DELETE_WO_PAYLOAD),
						null,
						"PROTOCOL - operation failed - An unknown / unsuported OpType was sent.  " +
								"Code MUST use pre-check and did not.",
								tx,
								System.currentTimeMillis() - sysTime);
	}
}

/**
 * CONF request processor.
 * @param baseTx - Transaction
 * @param input - Client request
 */
private void configure(Transaction baseTx,ConfigureInput input) {
	Transaction t = Transaction.get(input.getClientId(), input.getOpId());
	LOG.debug("Configure has been called");
	if (t == null) {
		t = baseTx;
	}
	t.setStatusTs(OperationStatus.ACTIVATION_DEQUEUE, System.currentTimeMillis());
	HierarchicalCache oCache = new HierarchicalCache((input.getOpRefScope() != null) ?
			input.getOpRefScope() : RefScope.Unknown,
			t.getTenantContext().getSc(),
			true);
	if (input.getOpBody() instanceof Payload) {
		oCache.newOpCache((Payload)input.getOpBody());
		t.setPayloadCache(oCache.getOpCache());
	}
	if(input.getOpBody() instanceof CreateOrUpdate){
		for(Contexts context : ((CreateOrUpdate)input.getOpBody()).getContexts()){
			if(context.getPorts()!=null){
				t.completeAndClose(System.currentTimeMillis());
				String defaultTenant = FpcProvider.getInstance().getConfig().getDefaultTenantId();
    			FpcIdentity defaultIdentity = (defaultTenant == null) ?  new FpcIdentity(0L) :  new FpcIdentity(defaultTenant);
    			DataBroker dataBroker = FpcProvider.getInstance().getDataBroker();
                WriteTransaction writetx = dataBroker.newWriteOnlyTransaction();
                org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.tenants.tenant.fpc.mobility.Contexts mobilityContext = new ContextsBuilder()
                		.setContextId(context.getContextId())
                		.setDpns(context.getDpns())
                		.setPorts(context.getPorts()).build();
                writetx.put(LogicalDatastoreType.OPERATIONAL,
    					InstanceIdentifier.builder(Tenants.class)
    					.child(Tenant.class, new TenantKey(defaultIdentity))
    					.child(FpcMobility.class)
    						.child(org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.tenants.tenant.fpc.mobility.Contexts.class, new ContextsKey(context.getContextId()))
    						.build(),
    					mobilityContext);
    				
    				CheckedFuture<Void,TransactionCommitFailedException> submitFuture = writetx.submit();

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
    				return;
			}
		}
	}
	executeOperation(oCache, t);
}

/**
 * CONF_BUNDLES request processing
 * @param txs - List of Transactions
 * @param input - Client request
 */
private void configureBundles(List<Transaction> txs, ConfigureBundlesInput input) {
	LOG.info("Configure-Bundles has been called");
	HierarchicalCache bundleCache;
	PayloadCache workingOpCache;
	boolean usingGlobal = false;

	TenantManager tenant = TenantManager.getTenantManager(input.getClientId());
	if (tenant == null) {
		LOG.warn("No tenant found for bundle. How did this happen? client-id is {}", input.getClientId());
		return;
	}

	switch (input.getHighestOpRefScope()) {
	case None :
	case Op :
		bundleCache = new HierarchicalCache(RefScope.Op, tenant.getSc(), false);
		break;
	case Bundle :
		bundleCache = new HierarchicalCache(RefScope.Bundle, tenant.getSc(), false);
		break;
	default:
		bundleCache = new HierarchicalCache(RefScope.Bundle, tenant.getSc(),true);
		usingGlobal = true;
	}

	Iterator<Transaction> txIt = txs.iterator();
	for (Bundles op : (input.getBundles() != null) ? input.getBundles() : Collections.<Bundles>emptyList()) {
		Transaction t = Transaction.get(op.getClientId(), op.getOpId());
		Transaction u = txIt.next();
		if (t == null) {
			t = u;
		}
		PayloadCache pc = (op.getOpBody() instanceof Payload)? bundleCache.newOpCache((Payload)op.getOpBody()):
			null;
		if (op.getOpRefScope() == null) {
			workingOpCache = bundleCache;
		} else {
			switch (op.getOpRefScope() ) {
			case None:
				workingOpCache = null;
				break;
			case Op :
				workingOpCache = pc;
				break;
			case Bundle :
				bundleCache.setGlobalUse(false);
				workingOpCache = bundleCache;
				break;
			default :
				bundleCache.setGlobalUse(usingGlobal);
				workingOpCache = bundleCache;
				break;
			}
		}
		ResultType rt = executeOperation(workingOpCache, t);

		if ((rt != null) && (rt instanceof Err)) {
			// TODO - Flag a cleanup call here
		}
		bundleCache.mergeOpCache();
	}
}

@Override
public void stop() {
	this.run = false;
}

@Override
public void close() throws Exception {
}

@SuppressWarnings("unchecked")
@Override
public void run() {
	this.run = true;
	LOG.info("ActivationWorker RUN started");
	try {
		while(run) {
			entrants.incrementAndGet();
			if ((entrants.get() % 100) == 0) {
				LOG.info("Configure Entries = {}", entrants.get());
			}
			AbstractMap.SimpleEntry<Object,Object> obj =
					(AbstractMap.SimpleEntry<Object,Object>) blockingConfigureQueue.take();
			if (obj.getValue() instanceof ConfigureInput) {
				configure((Transaction) obj.getKey(),(ConfigureInput)obj.getValue());
			} else if (obj.getValue() instanceof ConfigureBundlesInput) {
				configureBundles((List<Transaction>)obj.getKey(),(ConfigureBundlesInput)obj.getValue());
			}
		}
	} catch (InterruptedException e) {
		ErrorLog.logError(e.getStackTrace());
	}
}

@Override
public void open() {
	// Does nothing
}

@Override
public boolean isOpen() {
	return true;
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
