/*
 * Copyright © 2016 - 2017 Copyright (c) Sprint, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.fpc.activation.cache;

import java.util.HashMap;
import java.util.Map;

import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcbase.rev160803.FpcContext;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcbase.rev160803.FpcIdentity;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcbase.rev160803.FpcPort;

/**
 * Base Class for all Caches.
 */
class BaseCache implements Cache {
    protected Map<FpcIdentity, FpcContext> contexts;
    protected Map<FpcIdentity, FpcPort> ports;
    protected Map<FpcIdentity, Object> maincache;

    /**
     * Default Constructor.
     */
    protected BaseCache() {
        contexts = new HashMap<FpcIdentity, FpcContext>();
        ports = new HashMap<FpcIdentity, FpcPort>();
    }

    /**
     * Generates the unified (Identity to Object) hashmap used for {@link #genUnifiedHashmap() genUnifiedHashmap()}.
     */
    public void genUnifiedHashmap() {
        maincache.putAll(ports);
        maincache.putAll(contexts);
    }

    @Override
    public Map<FpcIdentity, Object> getUnifiedHashmap() {
        return maincache;
    }

    /**
     * Adds a port to the cache
     * @param port - Port to be added
     */
    public void addPort(FpcPort port) {
        ports.put(port.getPortId(), port);
    }

    /**
     * Adds a context to the cache
     * @param context - Context to be added
     */
    public void addContext(FpcContext context) {
        contexts.put(context.getContextId(), context);
    }

    @Override
    public Map<FpcIdentity, FpcPort> getPorts() {
        return ports;
    }

    @Override
    public FpcContext getContext(FpcIdentity key) {
        return contexts.get(key);
    }

    @Override
    public FpcPort getPort(FpcIdentity key) {
        return ports.get(key);
    }

    @Override
    public Map<FpcIdentity, FpcContext> getContexts() {
        return contexts;
    }
}
