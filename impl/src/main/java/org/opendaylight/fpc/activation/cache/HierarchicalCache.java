/*
 * Copyright © 2016 Copyright (c) Sprint, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.fpc.activation.cache;

import java.util.List;
import java.util.Map;

import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.Payload;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.RefScope;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.payload.Contexts;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.payload.Ports;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcbase.rev160803.FpcContext;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcbase.rev160803.FpcIdentity;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcbase.rev160803.FpcPort;

/*
 * The main generic Hierarchical Cache.  Its houses the cache(s) used during RPC processing.
 */
public class HierarchicalCache implements PayloadCache {
    private PayloadCache opCache;
    private BundleCache bundleCache;
    private StorageCache storageCache;
    private boolean useLazyOpCache;
    private boolean useGlobal;

    /**
     * Cache Constructor configured based upon input parameters.
     *
      * @param scope - Cache scope (RefScope)
     * @param sc - Storage Cache associated with the Tenant impacted by the operation.
     * @param useLazyOpCache - indicates if a {@link org.opendaylight.fpc.activation.cache.LazyOpCache LazyOpCache} strategy should be used.
     */
    public HierarchicalCache(RefScope scope, StorageCache sc, boolean useLazyOpCache) {
        opCache = null;
        bundleCache = null;
        storageCache = null;
        this.useLazyOpCache = useLazyOpCache;
        useGlobal = false;

        switch (scope) {
            case None:
            case Op :
                break;
            case Bundle :
                bundleCache = new BundleCache();
                break;
            default:
                storageCache = sc; // TODO - Add isBundle param to constructor to ensure caches are built correctly
                useGlobal = true;
        }
    }

    /**
     * Returns the associated OpCache
     * @return - Associated OpCache or null (if it was an internal operation).
     */
    public PayloadCache getOpCache() {
        return opCache;
    }

    /**
     * Returns the associated BundleCache
     * @return - Associated BundleCache or null if RefScope is not 'bundle'
     */
    public BundleCache getBundleCache() {
        return bundleCache;
    }

    /**
     * Determines if global cache use is permitted.
     * @return boolean indicating global cache use
     */
    public boolean getGlobalUse() {
        return this.useGlobal;
    }

    /**
     * Sets the global use parameter.
     * @param useGlobal - boolean indicating whether global use is permitted
     */
    public void setGlobalUse(boolean useGlobal) {
        this.useGlobal = useGlobal;
    }

    /**
     * Generates a new OpCache
     * @param model - Payload model to use for the OpCache
     * @return - new constructed OpCache
     */
    public PayloadCache newOpCache(Payload model) {
        mergeOpCache();
        this.opCache = (useLazyOpCache) ? new LazyOpCache(model) : new OpCache(model);
        return this.opCache;
    }

    /**
     * Merges the current OpCache with the bundle cache (if present).
     */
    public void mergeOpCache() {
        if ((opCache != null) && (bundleCache != null)) {
            this.bundleCache.addToCache(this.opCache);
        }
    }

    @Override
    public Map<FpcIdentity, FpcContext> getContexts() {
        return (bundleCache != null) ? bundleCache.getContexts() :
            ((opCache != null) ? opCache.getContexts() : null);
    }

    @Override
    public Map<FpcIdentity, FpcPort> getPorts() {
        return (bundleCache != null) ? bundleCache.getPorts() :
            ((opCache != null) ? opCache.getPorts() : null);
    }

    @Override
    public Map<FpcIdentity, Object> getUnifiedHashmap() {
        return (bundleCache != null) ? bundleCache.getUnifiedHashmap() :
            ((opCache != null) ? opCache.getUnifiedHashmap() : null);
    }

    @Override
    public FpcContext getContext(FpcIdentity key) {
        if (opCache != null) {
            if (opCache.getContext(key) != null) {
                return opCache.getContext(key);
            }
        }
        if (bundleCache != null) {
            if (bundleCache.getContext(key) != null) {
                return bundleCache.getContext(key);
            }
        }
        if (useGlobal) {
            return storageCache.getContext(key);
        }
        return null;
    }

    @Override
    public FpcPort getPort(FpcIdentity key) {
        if (opCache != null) {
            if (opCache.getPort(key) != null) {
                return opCache.getPort(key);
            }
        }
        if (bundleCache != null) {
            if (bundleCache.getPort(key) != null) {
                return bundleCache.getPort(key);
            }
        }
        if (useGlobal) {
            return storageCache.getPort(key);
        }
        return null;
    }

    @Override
    public List<Ports> getPayloadPorts() {
        return (opCache != null) ?
                opCache.getPayloadPorts() : null;
    }

    @Override
    public List<Contexts> getPayloadContexts() {
        return (opCache != null) ?
                opCache.getPayloadContexts() : null;
    }

    /**
     * Small private class used as the Bundle Cache.
     */
    private class BundleCache extends BaseCache {
        /**
         * Default Constructor.
         */
        public BundleCache() {
            super();
        }

        /**
         * Merges a Payload Cache to the Bundle.
         * @param cache - The PayloadCache to be merged.
         */
        public void addToCache(PayloadCache cache) {
            contexts.putAll(cache.getContexts());
            maincache.putAll(cache.getContexts());
            ports.putAll(cache.getPorts());
            maincache.putAll(cache.getPorts());
        }
    }

}
