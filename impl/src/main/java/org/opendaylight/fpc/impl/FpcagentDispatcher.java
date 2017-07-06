/*
 * Copyright © 2016 Copyright (c) Sprint, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.fpc.impl;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Future;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.NotificationPublishService;
import org.opendaylight.fpc.activation.workers.ActivationThreadPool;
import org.opendaylight.fpc.activation.workers.MonitorThreadPool;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.ClientIdentifier;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.ConfigureBundlesInput;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.ConfigureBundlesOutput;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.ConfigureDpnInput;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.ConfigureDpnOutput;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.ConfigureDpnOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.ConfigureDpnInput;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.ConfigureDpnOutput;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.ConfigureInput;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.ConfigureOutput;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.ConfigureOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.DpnOperation;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.EventDeregisterInput;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.EventDeregisterOutput;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.EventRegisterInput;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.EventRegisterOutput;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.IetfDmmFpcagentService;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.ProbeInput;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.ProbeOutput;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.Result;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.tenants.tenant.fpc.topology.Dpns;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcbase.rev160803.FpcDpn;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcbase.rev160803.FpcDpnId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.fpc.config.rev160927.FpcConfig;
import org.opendaylight.yangtools.yang.common.RpcError;
import org.opendaylight.yangtools.yang.common.RpcError.ErrorType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;

import com.google.common.util.concurrent.Futures;

/**
 * Primary Service that receives the requests and dispatches to assigned handlers.
 *
 * Handlers are assigned by Client bindings.
 */
public class FpcagentDispatcher implements IetfDmmFpcagentService {
	private static final Logger LOG = LoggerFactory.getLogger(FpcServiceImpl.class);

    private static final Map<String, IetfDmmFpcagentService> clientHandlerMap = new HashMap<String, IetfDmmFpcagentService>();
    private static IetfDmmFpcagentService defaultService = null;

    private static final RpcResult<ConfigureOutput> configUnknownClientErr;
    private static final RpcResult<ConfigureBundlesOutput> configBundlesUnknownClientErr;
    private static final RpcResult<EventRegisterOutput> eventRegUnknownClientErr;
    private static final RpcResult<ProbeOutput> probeUnknownClientErr;
    private static final RpcResult<EventDeregisterOutput> eventDeregUnknownClientErr;
    private static final RpcResult<ConfigureDpnOutput> configDpnUnknownDpnErr;
    private static final RpcResult<ConfigureOutput> configMissingBodyErr;
    private static final RpcResult<ConfigureBundlesOutput> configBundlesMissingBodyErr;
    private static final RpcResult<EventRegisterOutput> eventRegMissingBodyErr;
    private static final RpcResult<ProbeOutput> probeMissingBodyErr;
    private static final RpcResult<EventDeregisterOutput> eventDeregMissingBodyErr;
    private static final RpcResult<ConfigureDpnOutput> configDpnMissingBodyErr;

    static {
        RpcError missingClientError = RpcResultBuilder.newError(ErrorType.PROTOCOL,
                "invalid-value",
                "The provided Client ID is NOT registered with a tenant.");
        RpcError missingDpnError = RpcResultBuilder.newError(ErrorType.PROTOCOL,
                "invalid-value",
                "The provided DPN ID is NOT registered with a tenant.");

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
        configDpnUnknownDpnErr = RpcResultBuilder.<ConfigureDpnOutput>failed()
                .withRpcError(missingDpnError).build();

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

	@Override
	public Future<RpcResult<ConfigureDpnOutput>> configureDpn(ConfigureDpnInput input) {
		if (input == null) {
	        return Futures.immediateFuture(configDpnMissingBodyErr);
	    }
		//reach into db (cp tenantmgr) grab dpn obj tenants/tenant/default/fpc-topology/dpns/dpnX
		//2 different reads(abstract via absid & concrete via dpnid)
		//when solved create addDpn and removeDpn methods
//		Dpns vDpn1 = new Dpns();
//		FpcDpn vDpn = new FpcDpn();//FpcDpn has getDpnIds(), but its an interface no classes impl
//        //TODO set strategy (check operation then check ids)
//	    if (input.getOperation() == DpnOperation.Add) {
//	    	if(getDpnIds[0] == dpn1.getId() && getDpnIds[1] == dpn2.getId()){
//	    		LOG.info("Dpn"+dpn2.getId()+ " successfully added");
//		    	return Futures.immediateFuture(RpcResultBuilder.<ConfigureDpnOutput>success(new ConfigureDpnOutputBuilder()
//		    		.setResult(Result.Ok)
//		    		.setResultType(rt)).build());//rt = DPN obj (abstract?)
//	    	}
//	    }else if(input.getOperation() == DpnOperation.Remove){
//	    	if(getDpnIds[0] == dpn1.getId() && getDpnIds[1] == dpn2.getId()){
//	    		LOG.info("Dpn"+dpn2.getId()+ " successfully removed");
//		    	return Futures.immediateFuture(RpcResultBuilder.<ConfigureDpnOutput>success(new ConfigureDpnOutputBuilder()
//		    		.setResult(Result.Ok)
//		    		.setResultType(rt)).build());//rt = DPN obj (abstract?)
//	    	}
//	    }
	    return Futures.immediateFuture(configDpnUnknownDpnErr);
	}
}
