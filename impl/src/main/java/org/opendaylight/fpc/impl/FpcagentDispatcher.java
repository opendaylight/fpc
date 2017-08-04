/*
 * Copyright © 2016 Copyright (c) Sprint, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.fpc.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.fpc.activation.cache.StorageCache;
import org.opendaylight.fpc.activation.cache.transaction.Transaction;
import org.opendaylight.fpc.dpn.DpnHolder;
import org.opendaylight.fpc.tenant.TenantManager;
import org.opendaylight.fpc.utils.ErrorLog;
import org.opendaylight.fpc.utils.ErrorTypeIndex;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.ClientIdentifier;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.ConfigureBundlesInput;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.ConfigureBundlesOutput;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.ConfigureDpnInput;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.ConfigureDpnOutput;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.ConfigureDpnOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.ConfigureInput;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.ConfigureOutput;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.DpnOperation;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.ErrorTypeId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.EventDeregisterInput;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.EventDeregisterOutput;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.EventRegisterInput;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.EventRegisterOutput;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.IetfDmmFpcagentService;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.OpInput;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.ProbeInput;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.ProbeOutput;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.Result;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.Tenants;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.payload.Contexts;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.result.body.dpn.ResultType;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.tenants.Tenant;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.tenants.TenantKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.tenants.tenant.FpcTopology;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.tenants.tenant.fpc.topology.Dpns;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.tenants.tenant.fpc.topology.DpnsBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.tenants.tenant.fpc.topology.DpnsKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcbase.rev160803.FpcContext;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcbase.rev160803.FpcContextId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcbase.rev160803.FpcDpnId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcbase.rev160803.FpcIdentity;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcbase.rev160803.targets.value.Targets;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcError;
import org.opendaylight.yangtools.yang.common.RpcError.ErrorType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;

/**
 * Primary Service that receives the requests and dispatches to assigned handlers.
 *
 * Handlers are assigned by Client bindings.
 */
public class FpcagentDispatcher implements IetfDmmFpcagentService {
	private static final Logger LOG = LoggerFactory.getLogger(FpcagentDispatcher.class);
	private static DataBroker dataBroker;
	
    private static final Map<String, IetfDmmFpcagentService> clientHandlerMap = new HashMap<String, IetfDmmFpcagentService>();
    private static IetfDmmFpcagentService defaultService = null;

    private static final RpcResult<ConfigureOutput> configUnknownClientErr;
    private static final RpcResult<ConfigureBundlesOutput> configBundlesUnknownClientErr;
    private static final RpcResult<EventRegisterOutput> eventRegUnknownClientErr;
    private static final RpcResult<ProbeOutput> probeUnknownClientErr;
    private static final RpcResult<EventDeregisterOutput> eventDeregUnknownClientErr;
    private static final RpcResult<ConfigureOutput> configMissingBodyErr;
    private static final RpcResult<ConfigureBundlesOutput> configBundlesMissingBodyErr;
    private static final RpcResult<EventRegisterOutput> eventRegMissingBodyErr;
    private static final RpcResult<ProbeOutput> probeMissingBodyErr;
    private static final RpcResult<EventDeregisterOutput> eventDeregMissingBodyErr;
    private static final RpcResult<ConfigureDpnOutput> configDpnMissingBodyErr;
    private static final RpcResult<ConfigureDpnOutput> configDpnMissingDpnErr;
    private static final RpcResult<ConfigureDpnOutput> configDpnVdpnNotAbstractErr;
    private static final RpcResult<ConfigureDpnOutput> configDpnDpnNotRealErr;
    private static final RpcResult<ConfigureDpnOutput> configDpnTooManyDpnsErr;
    private static final RpcResult<ConfigureDpnOutput> configDpnNotEnoughDpnsError;


    
    static {
        RpcError missingClientError = RpcResultBuilder.newError(ErrorType.PROTOCOL,
                "invalid-value",
                "The provided Client ID is NOT registered with a tenant.");
        RpcError missingDpnError = RpcResultBuilder.newError(ErrorType.PROTOCOL,
                "invalid-value", "The provided DPN ID is NOT registered with the default tenant.");
        RpcError vdpnNotAbstractError = RpcResultBuilder.newError(ErrorType.PROTOCOL, 
        		"invalid-value", "The provided Abstract DPN ID does NOT refer to an Abstract DPN");
        RpcError dpnNotRealError = RpcResultBuilder.newError(ErrorType.PROTOCOL,
        		"invalid-value", "The provided DPN ID does NOT refer to a \"Real\" DPN");
        RpcError tooManyDpnsError = RpcResultBuilder.newError(ErrorType.PROTOCOL,
        		"invalid-value", "The provided Operation cannot be completed, the Abstract DPN already contains the maximum DPN's");
        RpcError notEnoughDpnsError = RpcResultBuilder.newError(ErrorType.PROTOCOL,
        		"invalid-value", "The provided Operation cannot be completed, the Abstract DPN does NOT contain any DPN's");
        

        configUnknownClientErr = RpcResultBuilder.<ConfigureOutput>failed()
                .withRpcError(missingClientError).build();
        configBundlesUnknownClientErr = RpcResultBuilder.<ConfigureBundlesOutput>failed()
                .withRpcError(missingClientError).build();
        eventRegUnknownClientErr = RpcResultBuilder.<EventRegisterOutput>failed()
                .withRpcError(missingClientError).build();
        probeUnknownClientErr =  RpcResultBuilder.<ProbeOutput>failed()
                .withRpcError(missingClientError).build();
        eventDeregUnknownClientErr = RpcResultBuilder.<EventDeregisterOutput>failed()
                .withRpcError(missingClientError).build();
        configDpnMissingDpnErr = RpcResultBuilder.<ConfigureDpnOutput>failed()
                .withRpcError(missingDpnError).build();
        configDpnVdpnNotAbstractErr = RpcResultBuilder.<ConfigureDpnOutput>failed()
                .withRpcError(vdpnNotAbstractError).build();
        configDpnDpnNotRealErr = RpcResultBuilder.<ConfigureDpnOutput>failed()
                .withRpcError(dpnNotRealError).build();
        configDpnTooManyDpnsErr = RpcResultBuilder.<ConfigureDpnOutput>failed()
                .withRpcError(tooManyDpnsError).build();
        configDpnNotEnoughDpnsError = RpcResultBuilder.<ConfigureDpnOutput>failed()
                .withRpcError(notEnoughDpnsError).build();

        RpcError missingBodyError = RpcResultBuilder.newError(ErrorType.PROTOCOL,
                "invalid-value",
                "No JSON body was provided");

        configMissingBodyErr = RpcResultBuilder.<ConfigureOutput>failed()
                .withRpcError(missingBodyError).build();
        configBundlesMissingBodyErr = RpcResultBuilder.<ConfigureBundlesOutput>failed()
                .withRpcError(missingBodyError).build();
        eventRegMissingBodyErr = RpcResultBuilder.<EventRegisterOutput>failed()
                .withRpcError(missingBodyError).build();
        probeMissingBodyErr =  RpcResultBuilder.<ProbeOutput>failed()
                .withRpcError(missingBodyError).build();
        eventDeregMissingBodyErr = RpcResultBuilder.<EventDeregisterOutput>failed()
                .withRpcError(missingBodyError).build();
        configDpnMissingBodyErr = RpcResultBuilder.<ConfigureDpnOutput>failed()
                .withRpcError(missingBodyError).build();
    }

    /**
     * Retrieves a Strategy (Service) for a specific Client Identifier.
     *
     * @param id - Connection ClientIdentifier
     * @return IetfDmmFpcagentService or null if not present
     */
    protected IetfDmmFpcagentService getStrategy(ClientIdentifier id) {
        IetfDmmFpcagentService retVal = clientHandlerMap.get(id.toString());
        return (retVal != null) ? retVal : defaultService;
    }

    /**
     * Adds a Strategy (Service) for a specific Client Identifier.
     *
     * @param id - Connection ClientIdentifier
     * @param strategy - IetfDmmFpcagentService
     */
    static public void addStrategy(ClientIdentifier id, IetfDmmFpcagentService strategy) {
        clientHandlerMap.put(id.toString(), strategy);
    }

    /**
     * Removes a Strategy (Service) for a specific Client Identifier.
     *
     * @param id - Connection ClientIdentifier
     */
    static public void removeStrategy(ClientIdentifier id) {
        clientHandlerMap.remove(id.toString());
    }

    @Override
    public Future<RpcResult<EventDeregisterOutput>> eventDeregister(EventDeregisterInput input) {
        if (input == null) {
            return Futures.immediateFuture(eventDeregMissingBodyErr);
        }
        IetfDmmFpcagentService ifc = getStrategy(input.getClientId());
        if (ifc != null) {
            return ifc.eventDeregister(input);
        }
        return Futures.immediateFuture(eventDeregUnknownClientErr);
    }

    @Override
    public Future<RpcResult<ProbeOutput>> probe(ProbeInput input) {
        if (input == null) {
            return Futures.immediateFuture(probeMissingBodyErr);
        }
        IetfDmmFpcagentService ifc = getStrategy(input.getClientId());
        if (ifc != null) {
            return ifc.probe(input);
        }
        return Futures.immediateFuture(probeUnknownClientErr);
    }

    @Override
    public Future<RpcResult<EventRegisterOutput>> eventRegister(EventRegisterInput input) {
        if (input == null) {
            return Futures.immediateFuture(eventRegMissingBodyErr);
        }
        IetfDmmFpcagentService ifc = getStrategy(input.getClientId());
        if (ifc != null) {
            return ifc.eventRegister(input);
        }
        return Futures.immediateFuture(eventRegUnknownClientErr);
    }

    @Override
    public Future<RpcResult<ConfigureOutput>> configure(ConfigureInput input) {
        if (input == null) {
            return Futures.immediateFuture(configMissingBodyErr);
        }
        IetfDmmFpcagentService ifc = getStrategy(input.getClientId());
        if (ifc != null) {
            return ifc.configure(input);
        }
        return Futures.immediateFuture(configUnknownClientErr);
    }

    @Override
    public Future<RpcResult<ConfigureBundlesOutput>> configureBundles(ConfigureBundlesInput input) {

        if (input == null) {
            return Futures.immediateFuture(configBundlesMissingBodyErr);
        }
        IetfDmmFpcagentService ifc = getStrategy(input.getClientId());
        if (ifc != null) {
            return ifc.configureBundles(input);
        }
        return Futures.immediateFuture(configBundlesUnknownClientErr);
    }
    
    public Dpns getDpnById(FpcDpnId dpnid) {
    	if(dataBroker != null){
    		ReadOnlyTransaction readtx = dataBroker.newReadOnlyTransaction();
    		String defaultTenant = FpcProvider.getInstance().getConfig().getDefaultTenantId();
    		FpcIdentity defaultIdentity = (defaultTenant == null) ?  new FpcIdentity(0L) :  new FpcIdentity(defaultTenant);
	    	Optional<Dpns> dsDpn;
	    	
	    	try {
	        	dsDpn = readtx.read(LogicalDatastoreType.CONFIGURATION,
	                    InstanceIdentifier.create(Tenants.class)
	                        .child(Tenant.class, new TenantKey(defaultIdentity))
	                        .child(FpcTopology.class)
	                        .child(Dpns.class, new DpnsKey(dpnid))).get();
	        	return (dsDpn.isPresent()) ? dsDpn.get() : null;
	    	} catch (InterruptedException e) {
	        	LOG.warn("Dpn retrieval interrupted");
	        	ErrorLog.logError(e.getStackTrace());
	    	} catch (ExecutionException e) {
	    		LOG.warn("Dpn retrieval interrupted");
	        	ErrorLog.logError(e.getStackTrace());
	    	}
    	}
    	LOG.info("Databroker couldn't be initialized");
    	return null;
    }
    
    /**
     * Add/Remove a DPN onto a VDPN (abstract DPN) based on a certain strategy
     *
     * @param input - DPN config input
     */
	@Override
	public Future<RpcResult<ConfigureDpnOutput>> configureDpn(ConfigureDpnInput input) {
		if (input == null)
	        return Futures.immediateFuture(configDpnMissingBodyErr);
		
		String defaultTenant = FpcProvider.getInstance().getConfig().getDefaultTenantId();
		FpcIdentity defaultIdentity = (defaultTenant == null) ?  new FpcIdentity(0L) :  new FpcIdentity(defaultTenant);
		dataBroker = FpcProvider.getInstance().getDataBroker();
		WriteTransaction writetx = dataBroker.newWriteOnlyTransaction();
		Dpns vdpn, dpn;
		ResultType rt;
		List<FpcDpnId> vdpnDpns;
		
		if(input.getDpnId() != null && input.getAbstractDpnId() != null){
			dpn = getDpnById(input.getDpnId());
			vdpn = getDpnById(input.getAbstractDpnId());
			if(vdpn.getDpnIds() != null){
				vdpnDpns = vdpn.getDpnIds();
			}else{
				LOG.info(vdpn.getDpnId()+" is empty");
				vdpnDpns = new ArrayList<FpcDpnId>();
			}
		}else
			return Futures.immediateFuture(configDpnMissingDpnErr);
		
		if(!vdpn.isAbstract())
			return Futures.immediateFuture(configDpnVdpnNotAbstractErr);
		
		if(dpn.isAbstract())
			return Futures.immediateFuture(configDpnDpnNotRealErr);
		
    	int threadCount = 0;
		
		if(input.getOperation() == DpnOperation.Add) {
			if(vdpn.getDpnIds()!=null && !vdpn.getDpnIds().contains(dpn.getDpnId())){
				new Thread(new SessionThread(vdpn, dpn, input.getOperation(), false), ("sessionThread"+ ++threadCount)).start();
				LOG.info("sessionThread"+threadCount+" started");
			}
			if(vdpnDpns.size() == 2)
				return Futures.immediateFuture(configDpnTooManyDpnsErr);
			vdpnDpns.add(dpn.getDpnId());
			TenantManager.vdpnDpnsMap.get(vdpn.getDpnId()).add(dpn.getDpnId());
		}
		
		if(input.getOperation() == DpnOperation.Remove){
			Boolean deleteFlag = false;
			if(vdpn.getDpnIds().size()==1)
				deleteFlag = true;
			new Thread(new SessionThread(vdpn, dpn, input.getOperation(),deleteFlag), ("sessionThread"+threadCount++)).start();
			LOG.info("sessionThread"+threadCount+" started");
			if(vdpnDpns.size() == 0)
				return Futures.immediateFuture(configDpnNotEnoughDpnsError);
			else{
				if(vdpnDpns.contains(dpn.getDpnId())){
					TenantManager.vdpnDpnsMap.get(vdpn.getDpnId()).remove(dpn.getDpnId());
					vdpnDpns.remove(dpn.getDpnId());
				}else{
					LOG.info(dpn.getDpnId()+" is unrelated to "+vdpn.getDpnId());
					rt = new org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.result.body.dpn.result.type.CommonSuccessBuilder(vdpn).build();
					return Futures.immediateFuture(RpcResultBuilder.<ConfigureDpnOutput>success(new ConfigureDpnOutputBuilder()
						.setResult(Result.Ok)
						.setResultType(rt)).build());
				}
			}
		}

		if(dataBroker != null){
			LOG.info("Updating DPN ids...");
			writetx.put(LogicalDatastoreType.CONFIGURATION,
				InstanceIdentifier.builder(Tenants.class)
				.child(Tenant.class, new TenantKey(defaultIdentity))
				.child(FpcTopology.class)
					.child(Dpns.class, new DpnsKey(vdpn.getKey()))
					.build(),
				new DpnsBuilder(vdpn).setDpnIds(vdpnDpns).build());
			
			CheckedFuture<Void,TransactionCommitFailedException> submitFuture = writetx.submit();
			Futures.addCallback(submitFuture, new FutureCallback<Void>() {
				@Override
				public void onFailure(Throwable arg0) {
					LOG.warn("Update failed");
				}
				@Override
				public void onSuccess(Void arg0) {
					// Do nothing
				}
			});
		}else{
	    	LOG.info("Databroker couldn't be initialized");
		}
		
		LOG.info("Strategy met, returning RPC...");
		rt = new org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.result.body.dpn.result.type.CommonSuccessBuilder(vdpn).build();
		return Futures.immediateFuture(RpcResultBuilder.<ConfigureDpnOutput>success(new ConfigureDpnOutputBuilder()
			.setResult(Result.Ok)
			.setResultType(rt)).build());
	}
}
