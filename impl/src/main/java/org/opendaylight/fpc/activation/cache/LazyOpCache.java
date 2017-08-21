/*
 * Copyright © 2016 - 2017 Copyright (c) Sprint, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.fpc.activation.cache;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.Payload;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.payload.Contexts;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.payload.Ports;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcbase.rev160803.FpcContext;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcbase.rev160803.FpcIdentity;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcbase.rev160803.FpcPort;

/**
 * A lazy (load only on demand) Operation Cache.
 */
class LazyOpCache extends OpCache {
    Payload model;

    /**
     * Constructor
     * @param model - Payload to be used as the initial Cache value.
     */
    protected LazyOpCache(Payload model) {
        super();
        this.model = model;
        contexts = null;
        ports = null;
        maincache = null;
    }

    /**
     * Generates the unified (Identity to Object) hashmap used for {@link #genUnifiedHashmap() genUnifiedHashmap()}.
     */
    public void genUnifiedHashmap() {
        maincache.putAll(getPorts());
        maincache.putAll(getContexts());
    }

    /**
     * Adds an FpcPort to the Cache
     * @param port - Port to be added.
     */
    public void addPort(FpcPort port) {
        if (ports == null) {
            ports = new ConcurrentHashMap<FpcIdentity, FpcPort>();
        }
        ports.put(port.getPortId(), port);
    }

    /**
     * Adds a Context to the Cache.
     * @param context - Context to be added.
     */
    public void addContext(FpcContext context) {
        if (contexts == null) {
            contexts = new ConcurrentHashMap<FpcIdentity, FpcContext>();
        }
        contexts.put(context.getContextId(), context);
    }

    @Override
    public Map<FpcIdentity, FpcContext> getContexts() {
        if (contexts == null) {
            contexts = new ConcurrentHashMap<FpcIdentity, FpcContext>();
            for (Contexts context : (model.getContexts() == null) ? Collections.<Contexts>emptyList() : model.getContexts()) {
                contexts.put(context.getContextId(), context);
            }
        }
        return contexts;
    }

    @Override
    public Map<FpcIdentity, FpcPort> getPorts() {
        if (ports == null) {
            ports = new ConcurrentHashMap<FpcIdentity, FpcPort>();
            for (Ports port : (model.getPorts() == null) ? Collections.<Ports>emptyList() : model.getPorts()) {
                ports.put(port.getPortId(), port);
            }
        }
        return ports;
    }

    @Override
    public Map<FpcIdentity, Object> getUnifiedHashmap() {
        return maincache;
    }

    @Override
    public List<Ports> getPayloadPorts() {
        return model.getPorts();
    }

    @Override
    public List<Contexts> getPayloadContexts() {
        return model.getContexts();
    }
}
