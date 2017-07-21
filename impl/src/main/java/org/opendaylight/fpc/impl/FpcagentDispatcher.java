/*
 * Copyright © 2016 - 2017 Copyright (c) Sprint, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.fpc.impl;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Future;

import org.opendaylight.fpc.activation.cache.transaction.Transaction;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.ClientIdentifier;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.ConfigureBundlesInput;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.ConfigureBundlesOutput;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.ConfigureInput;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.ConfigureOutput;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.EventDeregisterInput;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.EventDeregisterOutput;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.EventRegisterInput;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.EventRegisterOutput;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.IetfDmmFpcagentService;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.ProbeInput;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.ProbeOutput;
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
    private static final Logger LOG = LoggerFactory.getLogger(FpcagentDispatcher.class);

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

    static {
        RpcError missingClientError = RpcResultBuilder.newError(ErrorType.PROTOCOL,
                "invalid-value",
                "The provided Client ID is NOT registered with a tenant.");

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
    	LOG.info("Configure Start: "+System.currentTimeMillis());
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
}
