/*
 * Copyright © 2016 - 2017 Copyright (c) Sprint, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.fpc.activation.cache;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.Payload;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.payload.Contexts;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.payload.ContextsBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.payload.Ports;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.payload.PortsBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.result.body.result.type.CommonSuccess;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.result.body.result.type.CommonSuccessBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcbase.rev160803.FpcContext;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcbase.rev160803.FpcPort;

/**
 * Basic Operation Cache that loads the information model from the Payload upon construction.
 */
public class OpCache extends BaseCache implements PayloadCache {
    List<Ports> payloadPorts;
    List<Contexts> payloadContexts;

    /**
     * Default Constructor
     * @param model - Payload Model
     */
    public OpCache(Payload model) {
        super();
        payloadContexts = model.getContexts();
        for (Contexts context : (model.getContexts() == null) ? Collections.<Contexts>emptyList() : model.getContexts()) {
            contexts.put(context.getContextId(), context);
        }
        payloadPorts =  model.getPorts();
        for (Ports port : (model.getPorts() == null) ? Collections.<Ports>emptyList() : model.getPorts()) {
            ports.put(port.getPortId(), port);
        }

        maincache = null;
    }

    /**
     * Default initializer.
     */
    public OpCache() {
        payloadPorts = new ArrayList<Ports>();
        payloadContexts = new ArrayList<Contexts>();
    }

    /**
     * Adds an FpcPort to the Cache
     * @param port - Port to be added.
     */
    public void addPort(FpcPort port) {
        ports.put(port.getPortId(), port);
        if (port instanceof Ports)
            payloadPorts.add((Ports) port);
        else
            payloadPorts.add(new PortsBuilder(port).build());
    }

    /**
     * Adds a Context to the Cache.
     * @param context - Context to be added.
     */
    public void addContext(FpcContext context) {
        contexts.put(context.getContextId(), context);
        if (context instanceof Contexts)
            payloadContexts.add((Contexts) context);
        else
            payloadContexts.add(new ContextsBuilder(context).build());
    }

    /**
     * Generates a Common Success Object from the Cache.
     * @return - CommonSuccess Structure containing all Contexts and Ports in this cache.
     */
    public CommonSuccess getConfigSuccess() {
        return new CommonSuccessBuilder()
            .setPorts(this.payloadPorts)
            .setContexts(this.payloadContexts)
            .build();
    }

    @Override
    public List<Ports> getPayloadPorts() {
        return payloadPorts;
    }

    @Override
    public List<Contexts> getPayloadContexts() {
        return payloadContexts;
    }
}
