/*
 * Copyright © 2016 - 2017 Copyright (c) Sprint, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.fpc.activation.cache;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.fpc.impl.FpcagentServiceBase;
import org.opendaylight.fpc.tenant.TenantManager;
import org.opendaylight.fpc.utils.ErrorLog;
import org.opendaylight.fpc.utils.FpcCodecUtils;
import org.opendaylight.fpc.utils.NameResolver;
import org.opendaylight.fpc.utils.NameResolver.FixedType;
import org.opendaylight.mdsal.common.api.ReadFailedException;
import org.opendaylight.mdsal.dom.api.DOMDataTreeChangeListener;
import org.opendaylight.mdsal.dom.api.DOMSchemaService;
import org.opendaylight.mdsal.dom.broker.osgi.OsgiBundleScanningSchemaService;
import org.opendaylight.mdsal.dom.spi.store.DOMStoreReadTransaction;
import org.opendaylight.mdsal.dom.spi.store.DOMStoreReadWriteTransaction;
import org.opendaylight.mdsal.dom.spi.store.DOMStoreThreePhaseCommitCohort;
import org.opendaylight.mdsal.dom.spi.store.DOMStoreWriteTransaction;
import org.opendaylight.mdsal.dom.store.inmemory.InMemoryDOMDataStore;
import org.opendaylight.mdsal.dom.store.inmemory.InMemoryDOMDataStoreFactory;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.Tenants;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.TenantsBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.tenants.Tenant;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.tenants.TenantBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.tenants.tenant.FpcMobility;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.tenants.tenant.FpcMobilityBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.tenants.tenant.fpc.mobility.Contexts;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.tenants.tenant.fpc.mobility.ContextsBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.tenants.tenant.fpc.mobility.ContextsKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.tenants.tenant.fpc.mobility.Ports;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.tenants.tenant.fpc.mobility.PortsBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.tenants.tenant.fpc.mobility.PortsKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.tenants.tenant.fpc.policy.Actions;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.tenants.tenant.fpc.policy.ActionsBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.tenants.tenant.fpc.policy.ActionsKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.tenants.tenant.fpc.policy.Descriptors;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.tenants.tenant.fpc.policy.DescriptorsBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.tenants.tenant.fpc.policy.DescriptorsKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.tenants.tenant.fpc.policy.Policies;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.tenants.tenant.fpc.policy.PoliciesBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.tenants.tenant.fpc.policy.PoliciesKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.tenants.tenant.fpc.policy.PolicyGroups;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.tenants.tenant.fpc.policy.PolicyGroupsBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.tenants.tenant.fpc.policy.PolicyGroupsKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcbase.rev160803.FpcAction;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcbase.rev160803.FpcActionIdType;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcbase.rev160803.FpcContext;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcbase.rev160803.FpcContextId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcbase.rev160803.FpcDescriptor;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcbase.rev160803.FpcIdentity;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcbase.rev160803.FpcPolicy;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcbase.rev160803.FpcPolicyGroup;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcbase.rev160803.FpcPolicyGroupId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcbase.rev160803.FpcPolicyId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcbase.rev160803.FpcPort;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcbase.rev160803.FpcPortId;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;

import javassist.bytecode.Descriptor.Iterator;

/**
 * In-memory Storage Cache.
 */
public class StorageCache implements AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(StorageCache.class);

    private final NameResolver resolver;
    private DataBroker db;
    private final Collection<String> identities;
    private final InMemoryDOMDataStore memoryCache;
    protected final FpcCodecUtils codecs;
    protected final InstanceIdentifier<FpcMobility> mobilityIid;
    private final InstanceIdentifier<
        org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.tenants.tenant.FpcPolicy> policyIid;
    private final ContextsBuilder cb;
    private final PortsBuilder pb;
    private final PolicyGroupsBuilder pgb;
    private final PoliciesBuilder polb;
    private final ActionsBuilder ab;
    private final DescriptorsBuilder descb;
    private final TenantManager mgr;
    protected boolean isDirty;

    /**
     * Storage Cache Constructor
     * @param db - DataBroker
     * @param mgr - TenantManager represented by this Cache.
     */
    public StorageCache(DataBroker db,
            TenantManager mgr) {
        this.mgr = mgr;
        this.db = db;
        this.resolver = NameResolver.get();

        identities = new HashSet<String>();
        cb = new ContextsBuilder();
        pb = new PortsBuilder();
        pgb = new PolicyGroupsBuilder();
        polb = new PoliciesBuilder();
        ab = new ActionsBuilder();
        descb = new DescriptorsBuilder();

        LOG.info("Storage Cache was initialized");

        // Inmemory Cache instantiation
        codecs = FpcCodecUtils.get();
        OsgiBundleScanningSchemaService schemaService = OsgiBundleScanningSchemaService.getInstance();
        if (schemaService == null) {
            BundleContext ctxt = FrameworkUtil.getBundle(StorageCache.class).getBundleContext();
            schemaService = OsgiBundleScanningSchemaService.createInstance(ctxt);
        }
        memoryCache = InMemoryDOMDataStoreFactory.create("fpccache", (DOMSchemaService)schemaService);

        mobilityIid = InstanceIdentifier.builder(Tenants.class)
                .child(Tenant.class, mgr.getTenant().getKey() ).child(FpcMobility.class).build();

        policyIid = InstanceIdentifier.builder(Tenants.class)
          .child(Tenant.class, mgr.getTenant().getKey() ).child(
            org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.tenants.tenant.FpcPolicy.class).build();

        //Mobility Cache Preparation
        TenantBuilder tb = new TenantBuilder(mgr.getTenant());

        //Prep Mobility Structure
        FpcMobilityBuilder mobBldr = (tb.getFpcMobility() == null) ? new FpcMobilityBuilder()
                : new FpcMobilityBuilder(tb.getFpcMobility());
        boolean build = (mobBldr.getContexts() == null) ? true : (mobBldr.getContexts().size() == 0);
        FpcContextId ctxtId = new FpcContextId("234dummy432");
        if (build) {
            List<Contexts> ctxts = new ArrayList<Contexts>();
            ctxts.add(new ContextsBuilder()
                    .setContextId(ctxtId)
                    .setKey(new ContextsKey(ctxtId)).build());
            mobBldr.setContexts(ctxts);
        }
        build = (mobBldr.getPorts() == null) ? true : (mobBldr.getPorts().size() == 0);
        FpcPortId portId = new FpcPortId("234dummy432555");
        if (build) {
            List<Ports> ports = new ArrayList<Ports>();
            ports.add(new PortsBuilder()
                    .setPortId(portId)
                    .setKey(new PortsKey(portId)).build());
            mobBldr.setPorts(ports);
        }
        tb.setFpcMobility(mobBldr.build());

        List<Tenant> tenants = new ArrayList<Tenant>();
        tenants.add(tb.build());
        Tenants tenantsTop = new TenantsBuilder()
                .setTenant(tenants)
                .build();

        Map.Entry<YangInstanceIdentifier,NormalizedNode<?,?>> node =
            codecs.getCodecRegistry().toNormalizedNode(InstanceIdentifier.builder(Tenants.class).build(),tenantsTop);
        DOMStoreWriteTransaction wtrans = memoryCache.newWriteOnlyTransaction();
        wtrans.write(node.getKey(), node.getValue());
        commitTrans(wtrans);

        WriteTransaction wtrans0 = db.newWriteOnlyTransaction();
        wtrans0.put(LogicalDatastoreType.OPERATIONAL, InstanceIdentifier.builder(Tenants.class)
                .child(Tenant.class, mgr.getTenant().getKey() ).build(), tb.build());

        try {
            wtrans0.submit().get();
        } catch (InterruptedException | ExecutionException e) {
            ErrorLog.logError(e.getStackTrace());
        }

        //This is done for an obscure bug we have in the system where it errors out on the first
        //Context write
        DOMStoreWriteTransaction wtrans1 = memoryCache.newWriteOnlyTransaction();
        Map.Entry<YangInstanceIdentifier,NormalizedNode<?,?>> nnode = codecs.getCodecRegistry().toNormalizedNode(
                mobilityIid
                .child(Contexts.class, new ContextsKey(ctxtId)), new ContextsBuilder()
                .setContextId(ctxtId)
                .setKey(new ContextsKey(ctxtId)).build());
        wtrans1.write(nnode.getKey(), nnode.getValue());
        this.commitTrans(wtrans1);

        isDirty = false;
        StorageWriter writer = StorageWriter.getInstance();
        if (writer != null) {
            writer.addCache(this);
        }
    }

    /**
     * Indicates if the cache is dirty (has not been synced with Storage).
     * @return boolean indicating if the cache is dirty.
     */
    public boolean isDirty() {
        return isDirty;
    }

    /**
     * Sets the dirty state to false.
     */
    protected void snapshotComplete() {
        isDirty = false;
    }

    // Action
    /**
     * Adds an Action to the Cache.
     * @param arg - Action to be added.
     */
    public void addAction(FpcAction arg) {
        Actions a = (arg instanceof Actions) ? (Actions) arg : null;
        if (a == null) {
            ab.fieldsFrom(arg);
            a = ab.build();
        }
        write(a);
    }

    /**
     * Writes an Action to the in-memory Cache.
     * @param act - Action to be added.
     */
    protected void write(Actions act) {
        Map.Entry<YangInstanceIdentifier,NormalizedNode<?,?>> node = codecs.getCodecRegistry().toNormalizedNode(
                policyIid.child(Actions.class, act.getKey()), act);
        write(NameResolver.extractString(act.getActionId()), node.getKey(), node.getValue());
    }

    /**
     * Retrieves an Action
     * @param key - Action Identity
     * @return An Action for the corresponding Identity or null if not present.
     */
    public Actions getAction(FpcIdentity key) {
        DataObject dObj = read( codecs.getCodecRegistry().toYangInstanceIdentifier( policyIid
                .child(Actions.class, new ActionsKey(new FpcActionIdType(key)))) );
        return (dObj != null) ? (Actions) dObj : null;
    }

    /**
     * Removes an Action
     * @param act - Action to be removed.
     */
    public void remove(Actions act) {
        remove(policyIid.child(Actions.class, act.getKey()));
        identities.remove(NameResolver.extractString(act.getActionId()));
    }

    // Descriptor
    /**
     * Adds a Descriptor to the Cache.
     * @param arg - Descriptor to be added.
     */
    public void addDescrptor(FpcDescriptor arg) {
        Descriptors d = (arg instanceof Descriptors) ? (Descriptors) arg : null;
        if (d == null) {
            descb.fieldsFrom(arg);
            d = descb.build();
        }
        write(d);
    }

    /**
     * Writes a Descriptor to the in-memory Cache.
     * @param desc - Descriptor to be added.
     */
    protected void write(Descriptors desc) {
        Map.Entry<YangInstanceIdentifier,NormalizedNode<?,?>> node = codecs.getCodecRegistry().toNormalizedNode(
                policyIid.child(Descriptors.class, desc.getKey()), desc);
        write(NameResolver.extractString(desc.getDescriptorId()), node.getKey(), node.getValue());
    }

    /**
     * Retrieves a Descriptor
     * @param key - Descriptor Identity
     * @return A Descriptor for the corresponding Identity or null if not present.
     */
    public Descriptors getDescriptor(FpcIdentity key) {
        DataObject dObj = read( codecs.getCodecRegistry().toYangInstanceIdentifier( policyIid
                .child(Descriptors.class, new DescriptorsKey(new FpcPolicyId(key)))) );
        return (dObj != null) ? (Descriptors) dObj : null;
    }

    /**
     * Removes a Descriptor
     * @param desc - Descriptor to be removed.
     */
    public void remove(Descriptors desc) {
        remove(policyIid.child(Descriptors.class, desc.getKey()));
        identities.remove(NameResolver.extractString(desc.getDescriptorId()));
    }

    // Policy
    /**
     * Adds a Policy to the Cache.
     * @param arg - Policy to be added.
     */
    public void addPolicy(FpcPolicy arg) {
        Policies p = (arg instanceof Policies) ? (Policies) arg : null;
        if (p == null) {
            polb.fieldsFrom(arg);
            p = polb.build();
        }
        write(p);
    }
    /**
     * Writes a Policy to the in-memory Cache.
     * @param pol - Policy to be added.
     */
    protected void write(Policies pol) {
        Map.Entry<YangInstanceIdentifier,NormalizedNode<?,?>> node = codecs.getCodecRegistry().toNormalizedNode(
                policyIid.child(Policies.class, pol.getKey()), pol);
        write(NameResolver.extractString(pol.getPolicyId()), node.getKey(), node.getValue());
    }

    /**
     * Retrieves a Policy
     * @param key - Policy Identity
     * @return A Policy for the corresponding Identity or null if not present.
     */
    public Policies getPolicy(FpcIdentity key) {
        DataObject dObj = read( codecs.getCodecRegistry().toYangInstanceIdentifier( policyIid
                .child(Policies.class, new PoliciesKey(new FpcPolicyId(key)))) );
        return (dObj != null) ? (Policies) dObj : null;
    }

    /**
     * Removes a Policy
     * @param pol - Policy to be removed.
     */
    public void remove(Policies pol) {
        remove(policyIid.child(Policies.class, pol.getKey()));
        identities.remove(NameResolver.extractString(pol.getPolicyId()));
    }

    // PolicyGroup
    /**
     * Adds a PolicyGroup to the Cache.
     * @param arg - PolicyGroup to be added.
     */
    public void addPolicyGroup(FpcPolicyGroup arg) {
        PolicyGroups p = (arg instanceof PolicyGroups) ? (PolicyGroups) arg : null;
        if (p == null) {
            pgb.fieldsFrom(arg);
            p = pgb.build();
        }
        write(p);
    }

    /**
     * Writes a PolicyGroup to the in-memory Cache.
     * @param polGroup - PolicyGroup to be added.
     */
    protected void write(PolicyGroups polGroup) {
        Map.Entry<YangInstanceIdentifier,NormalizedNode<?,?>> node = codecs.getCodecRegistry().toNormalizedNode(
                policyIid.child(PolicyGroups.class, polGroup.getKey()), polGroup);
        write(NameResolver.extractString(polGroup.getPolicyGroupId()), node.getKey(), node.getValue());
    }

    /**
     * Retrieves a PolicyGroup
     * @param key - PolicyGroup Identity
     * @return A PolicyGroup for the corresponding Identity or null if not present.
     */
    public FpcPolicyGroup getPolicyGroup(FpcIdentity key) {
        DataObject dObj = read( codecs.getCodecRegistry().toYangInstanceIdentifier( policyIid
                .child(PolicyGroups.class, new PolicyGroupsKey(new FpcPolicyGroupId(key)))) );
        return (dObj != null) ? (FpcPolicyGroup) dObj : null;
    }

    /**
     * Removes a PolicyGroup
     * @param polGroup - PolicyGroup to be removed.
     */
    public void remove(PolicyGroups polGroup) {
        remove(policyIid.child(PolicyGroups.class, polGroup.getKey()));
        identities.remove(NameResolver.extractString(polGroup.getPolicyGroupId()));
    }

    // Port methods
    /**
     * Adds a Port to the Cache.
     * @param arg - Port to be added.
     */
    public void addPort(FpcPort arg) {
        Ports p = (arg instanceof Ports) ? (Ports) arg : null;
        if (p == null) {
            pb.fieldsFrom(arg);
            p = pb.build();
        }
        write(p);
    }

    /**
     * Writes a Port to the in-memory Cache.
     * @param port - Port to be added.
     */
    protected void write(Ports port) {
        Map.Entry<YangInstanceIdentifier,NormalizedNode<?,?>> node = codecs.getCodecRegistry().toNormalizedNode(
                mobilityIid.child(Ports.class, port.getKey()), port);
        write(NameResolver.extractString(port.getPortId()), node.getKey(), node.getValue());
    }

    /**
     * Retrieves a Port
     * @param key - Port Identity
     * @return A Port for the corresponding Identity or null if not present.
     */
    public FpcPort getPort(FpcIdentity key) {
        DataObject dObj = read( codecs.getCodecRegistry().toYangInstanceIdentifier( mobilityIid
                .child(Ports.class, new PortsKey(new FpcPortId(key)))) );
        return (dObj != null) ? (FpcPort) dObj : null;
    }

    /**
     * Removes a Port
     * @param port - Port to be removed.
     */
    public void remove(Ports port) {
        remove(mobilityIid.child(Ports.class, port.getKey()));
        identities.remove(NameResolver.extractString(port.getPortId()));
    }

    // Context Methods
    /**
     * Adds a Context to the Cache.
     * @param arg - Context to be added.
     */
    public void addContext(FpcContext arg) {
        Contexts c = (arg instanceof Contexts) ? (Contexts) arg : null;
        if (c == null) {
            cb.fieldsFrom(arg);
            c = cb.build();
        }
        write(c);
    }

    /**
     * Writes a Context to the in-memory Cache.
     * @param ctxt - Context to be added.
     */
    protected void write(Contexts ctxt) {
        Map.Entry<YangInstanceIdentifier,NormalizedNode<?,?>> node = codecs.getCodecRegistry().toNormalizedNode(
                mobilityIid.child(Contexts.class, ctxt.getKey()), ctxt);
        write(NameResolver.extractString(ctxt.getContextId()), node.getKey(), node.getValue());
    }

    /**
     * Retrieves a Context
     * @param key - Context Identity
     * @return A Context for the corresponding Identity or null if not present.
     */
    public FpcContext getContext(FpcIdentity key) {
        DataObject dObj = read( codecs.getCodecRegistry().toYangInstanceIdentifier( mobilityIid
                .child(Contexts.class, new ContextsKey(new FpcContextId(key)))) );
        return (dObj != null) ? (FpcContext) dObj : null;
    }

    /**
     * Removes a Context
     * @param ctxt - Context to be removed.
     */
    public void remove(Contexts ctxt) {
        remove(mobilityIid.child(Contexts.class, ctxt.getKey()));
        identities.remove(NameResolver.extractString(ctxt.getContextId()));
    }

    // Common Methods
    /**
     * Reads Cache and returns the read object as InstanceIdentifier/DataObject pair.
     * @param iid - Yang Instance ID
     * @return A Map Entry (pair) that holds the Instance Identifier / DataObject pair.
     */
    public Map.Entry<InstanceIdentifier<?>, DataObject> readAsPair(YangInstanceIdentifier iid) {
        DOMStoreReadTransaction rTrans = memoryCache.newReadOnlyTransaction();
        try {
            Optional<NormalizedNode<?,?>> val =
                rTrans.read(iid).get();
            if (val.isPresent()) {
                return codecs.getCodecRegistry().fromNormalizedNode(iid, val.get());
            }
        } catch (InterruptedException | ExecutionException e) {
        	ErrorLog.logError(e.getStackTrace());
        }
        return null;
    }

    /**
     * Reads Cache.
     * @param iid - Yang Instance ID
     * @return DataObject
     */
    public DataObject read(YangInstanceIdentifier iid) {
        DOMStoreReadTransaction rTrans = memoryCache.newReadOnlyTransaction();
        try {
            Optional<NormalizedNode<?,?>> val =
                rTrans.read(iid).get();
            if (val.isPresent()) {
                return codecs.dataObjectFromNormalizedNode(iid, val.get());
            }
        } catch (InterruptedException | ExecutionException e) {
        	ErrorLog.logError(e.getStackTrace());
        }
        return null;
    }

    /**
     * Reads Cache.
     * @param instanceId - String (Yang Instance ID)
     * @return DataObject
     */
    public DataObject read(String instanceId) {
        YangInstanceIdentifier iiCtxt = resolver.toInstanceIdentifier(instanceId);
        return (iiCtxt!= null) ? read(iiCtxt) : null;
    }

    /**
     * Reads Cache.
     * @param iid - Instance Identifier
     * @return DataObject
     */
    public DataObject read(InstanceIdentifier<?> iid) {
        YangInstanceIdentifier iiCtxt = codecs.getCodecRegistry().toYangInstanceIdentifier(iid);
        return (iiCtxt!= null) ? read(iiCtxt) : null;
    }

    /**
     * Adds a Cache to the Storage Cache.
     * @param cache - Cache to be added
     */
    public void addToCache(Cache cache) {
    	java.util.Iterator<FpcPort> i = cache.getPorts().values().iterator();
    	while(i.hasNext()){
    		addPort(i.next());
    	}
//        for (FpcPort port : cache.getPorts().values()) {
//            addPort(port);
//        }
    	java.util.Iterator<FpcContext> c = cache.getContexts().values().iterator();
    	while(c.hasNext()){
    		addContext(c.next());
    	}
//        for (FpcContext context : cache.getContexts().values()) {
//            addContext(context);
//        }
    }

    /**
     * Writes a Node to the Cache.
     * @param identKey - Identity
     * @param key - Yang Instance Identifier key for the Object
     * @param value - NormalizedNode to write to the Cache.
     */
    protected void write(String identKey, YangInstanceIdentifier key, NormalizedNode<?,?> value) {
        boolean isCreate = identities.contains(identKey);
        DOMStoreReadWriteTransaction wtrans = memoryCache.newReadWriteTransaction();//.newWriteOnlyTransaction();
        if (!isCreate) {
            wtrans.write(key, value);
        } else {
            wtrans.merge(key, value);
            /*CheckedFuture<Optional<NormalizedNode<?, ?>>, ReadFailedException> future = wtrans.read(key);
            Futures.addCallback(future, new FutureCallback<Optional<NormalizedNode<?, ?>>>() {

				@Override
				public void onSuccess(Optional<NormalizedNode<?, ?>> result) {
					if(result.isPresent()) {
						DataObject dObj = codecs.dataObjectFromNormalizedNode(key,result.get());
						if(dObj instanceof org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.payload.Contexts){
							org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.payload.Contexts context = (org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.payload.Contexts) dObj;
							FpcagentServiceBase.sessionMap.get(context.getContextId()).getValue().add(context);
						}
					}

				}

				@Override
				public void onFailure(Throwable t) {
					// TODO Auto-generated method stub

				}

            });*/
        }
        if (commitTrans(wtrans)) {
            if (!isCreate) {
                identities.add(identKey);
            }
        }
    }

    /**
     * Removes an Object form the Cache.
     * @param instanceId - Instance Identifier
     */
    public void remove(String instanceId) {
        Map.Entry<FixedType, String> entityInfo = this.resolver.extractTypeAndKey(instanceId);
        remove(resolver.toInstanceIdentifier(instanceId));
        if ((!identities.remove(instanceId)) && (entityInfo != null)) {
        	identities.remove(entityInfo.getValue());
        }
    }

    /**
     * Removes an Object form the Cache.
     * @param iid - Instance Identifier
     */
    protected void remove(InstanceIdentifier<?> iid) {
        remove(codecs.getCodecRegistry().toYangInstanceIdentifier(iid));
    }

    /**
     * Removes an Object form the Cache.
     * @param iiCtxt - Yang Instance Identifier
     */
    protected void remove(YangInstanceIdentifier iiCtxt) {
        if (iiCtxt != null) {
            DOMStoreWriteTransaction wtrans = memoryCache.newWriteOnlyTransaction();
            wtrans.delete(iiCtxt);
            commitTrans(wtrans);
        }
    }

    /**
     * Commits the Transaction
     * @param wtrans - write transaction
     * @return boolean indicating if the transaction was successful
     */
    protected boolean commitTrans(DOMStoreWriteTransaction wtrans) {
        DOMStoreThreePhaseCommitCohort trans = wtrans.ready();
        try {
            assert(trans.canCommit().get().booleanValue());
            trans.preCommit();
            trans.commit();
            isDirty = true;
            return true;
        } catch (InterruptedException e) {
        	ErrorLog.logError(e.getStackTrace());
        } catch (ExecutionException e) {
        	ErrorLog.logError(e.getStackTrace());
        }
        return false;
    }

    @Override
    public void close() {
        memoryCache.close();
        StorageWriter writer = StorageWriter.getInstance();
        if (writer != null) {
            writer.removeCache(this);
        }
    }

    /**
     * Returns the Mobility Tree within the cache.
     * @return A pair (Map Entry) with the FPC Mobility Instance Identifier and value.
     */
    public Map.Entry<InstanceIdentifier<FpcMobility>, FpcMobility> getMobilityTree() {
        return new AbstractMap.SimpleEntry<InstanceIdentifier<FpcMobility>, FpcMobility>(mobilityIid,
                (FpcMobility)read(mobilityIid));
    }

    /**
     * Registers a Change Listener with the Cache.
     * @param iid - Instance Identifier to Monitor
     * @param listener - Listener to be registered
     * @return Listener Registration
     */
    public ListenerRegistration<DOMDataTreeChangeListener> watch(YangInstanceIdentifier iid,
            DOMDataTreeChangeListener listener) {
        return memoryCache.registerTreeChangeListener(iid, listener);
    }

	/**
	 * Checks if the Identifiers map contains an identity
	 * @param identifier - identifier to check
	 * @return - true if exists, false otherwise
	 */
	public boolean hasIdentity(String identifier) {
		return identities.contains(identifier);
	}
}
