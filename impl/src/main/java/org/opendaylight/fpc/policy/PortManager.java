/*
 * Copyright © 2016 Copyright (c) Sprint, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.fpc.policy;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataTreeChangeListener;
import org.opendaylight.controller.md.sal.binding.api.DataTreeIdentifier;
import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.controller.md.sal.binding.api.DataObjectModification.ModificationType;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.fpc.activation.impl.dpdkdpn.DpdkImpl;
/*
import org.opendaylight.fpc.policy.PolicyManager.ActionsChangeManager;
import org.opendaylight.fpc.policy.PolicyManager.DescriptorChangeManager;
import org.opendaylight.fpc.policy.PolicyManager.PolicyChangeManager;
*/
import org.opendaylight.fpc.utils.ErrorLog;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.Tenants;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.payload.Ports;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.tenants.Tenant;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.tenants.TenantKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.tenants.tenant.FpcMobility;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.tenants.tenant.FpcPolicy;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.tenants.tenant.fpc.mobility.Contexts;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.tenants.tenant.fpc.mobility.PortsBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.tenants.tenant.fpc.mobility.PortsKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.tenants.tenant.fpc.policy.Actions;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.tenants.tenant.fpc.policy.Descriptors;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.tenants.tenant.fpc.policy.Policies;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcbase.rev160803.FpcIdentity;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcbase.rev160803.FpcPolicyGroupId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcbase.rev160803.fpc.context.Dpns;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcbase.rev160803.fpc.context.profile.Nexthop;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base class that loads DPNs from the data store and sets up change listeners.
 * ??
 */
public abstract class PortManager implements AutoCloseable {
    protected static final Logger LOG = LoggerFactory.getLogger(PortManager.class);

    protected FpcIdentity tenantId;
    protected DataBroker db;
    protected ListenerRegistration<ContextChangeManager> dataTreeChangeListenerRegistration;
    
    /**
     * Default Constructor.
     */
    protected PortManager() {
        db = null;
        dataTreeChangeListenerRegistration = null;
        tenantId = null;
    }

    /**
     * Initialization function.
     * @param db - Data Broker
     * @param tenant - Tenant
     */
    protected void init(DataBroker db, Tenant tenant) {
        this.db = db;
        this.tenantId = tenant.getTenantId();
        FpcMobility mobility = tenant.getFpcMobility();
        if (mobility != null){
        	for(Contexts cntx : (mobility.getContexts() == null) ? Collections.<Contexts>emptyList() : mobility.getContexts()){
        		try{
        			LOG.info("Loading Context {} from Storage for Tenant {}", mobility.getContexts(), tenantId);
        			addContext(cntx);
        		} catch (Exception e){
        			LOG.error("PortManager - Error during Context load for Tenant {} / Mobility {}", mobility.getContexts(), tenantId);
        			ErrorLog.logError(e.getStackTrace());
        		}
        		/*for(Descriptors desc : (policy.getDescriptors() == null) ? Collections.<Descriptors>emptyList() : policy.getDescriptors()) {
        		try {
        			LOG.info("Loading Descriptor {} from Storage  for Tenant {}", desc.getDescriptorId(),tenantId);
        			addDescriptor(desc);
        		} catch (Exception e){
        			LOG.error("PolicyManager- Error during Descriptor load for Tenant {} / Policy {}", desc.getDescriptorId(), tenantId);
        			ErrorLog.logError(e.getStackTrace());
        		}
        	}*/
        	}
        }
    }

    /**
     * Adds a Context to this manager.
     * @param cntx - Context to add
     * @throws Exception - If the Add violates the existing state.
     */
    abstract public void addContext(Contexts cntx) throws Exception;

    /**
     * Removes a Context from this manager.
     * @param cntx - Context to remove
     */
    abstract public void removeContext(Contexts cntx);

    /**
     * Registers Change Listener for Descriptors under the Tenant.
     */
 
    public void registerListeners() {
        dataTreeChangeListenerRegistration = this.db
                   .registerDataTreeChangeListener(
                           new DataTreeIdentifier<Contexts>(LogicalDatastoreType.OPERATIONAL,
                                  InstanceIdentifier.builder(Tenants.class)
                                    .child(Tenant.class, new TenantKey( tenantId ))
                                    .child(FpcMobility.class)
                                    .child(Contexts.class).build() ),
                               new ContextChangeManager() );
        LOG.info("ContextChangeManager Registered for Tenant {}", tenantId);
    }

    @Override
    public void close() {
        dataTreeChangeListenerRegistration.close();
    }
 
    /**
     * Private Change Listener class for Descriptors.
     */
    private class ContextChangeManager implements DataTreeChangeListener<Contexts> {
         @Override
         public void onDataTreeChanged(Collection<DataTreeModification<Contexts>> changes) {
             for (DataTreeModification<Contexts> cntxModification : changes) {
                 LOG.info("Descriptor Change has occured for Tenant-Id {} / Descriptor-Id {}",tenantId,
                         cntxModification.getRootPath().toString());
                 if (cntxModification.getRootNode().getModificationType() == ModificationType.DELETE) {
                     removeContext(cntxModification.getRootNode().getDataBefore());
                 } else {
                     try {
                        addContext(cntxModification.getRootNode().getDataAfter());
                    } catch (Exception e) {
                        ErrorLog.logError("DescriptorChangeManager - Error occured during Descriptor Create/Write - " + e.getLocalizedMessage(), e.getStackTrace());
                    }
                 }

             }
         }
    }

}
	
	
