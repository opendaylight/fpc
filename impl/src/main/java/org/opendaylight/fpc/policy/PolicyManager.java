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

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataTreeChangeListener;
import org.opendaylight.controller.md.sal.binding.api.DataTreeIdentifier;
import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.controller.md.sal.binding.api.DataObjectModification.ModificationType;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.fpc.utils.ErrorLog;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.Tenants;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.tenants.Tenant;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.tenants.TenantKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.tenants.tenant.FpcMobility;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.tenants.tenant.FpcPolicy;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.tenants.tenant.fpc.mobility.Ports;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.tenants.tenant.fpc.policy.Actions;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.tenants.tenant.fpc.policy.Descriptors;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.tenants.tenant.fpc.policy.Policies;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.tenants.tenant.fpc.policy.PolicyGroups;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcbase.rev160803.FpcIdentity;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract base class that loads Descriptors, Actions, 
 * and Policies from the data store and sets up change listeners.
 */
public abstract class PolicyManager implements AutoCloseable {
    protected static final Logger LOG = LoggerFactory.getLogger(PolicyManager.class);

    protected FpcIdentity tenantId;
    protected DataBroker db;
    protected ListenerRegistration<DescriptorChangeManager> dataTreeChangeListenerRegistration;
    protected ListenerRegistration<ActionsChangeManager> dataTreeChangeListenerRegistration2;
    protected ListenerRegistration<PolicyChangeManager> dataTreeChangeListenerRegistration3;
    protected ListenerRegistration<PolicyGroupChangeManager> dataTreeChangeListenerRegistration4;
    protected ListenerRegistration<PortChangeManager> dataTreeChangeListenerRegistration5;
    /**
     * Default Constructor.
     */
    protected PolicyManager() {
        db = null;
        dataTreeChangeListenerRegistration = null;
        dataTreeChangeListenerRegistration2 = null;
        dataTreeChangeListenerRegistration3 = null;
        dataTreeChangeListenerRegistration4 = null;
        dataTreeChangeListenerRegistration5 = null;
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
        FpcPolicy policy = tenant.getFpcPolicy();
        if (policy != null){
        	for(Descriptors desc : (policy.getDescriptors() == null) ? Collections.<Descriptors>emptyList() : policy.getDescriptors()) {
        		try {
        			LOG.info("Loading Descriptor {} from Storage  for Tenant {}", desc.getDescriptorId(),tenantId);
        			addDescriptor(desc);
        		} catch (Exception e){
        			LOG.error("PolicyManager- Error during Descriptor load for Tenant {} / Policy {}", desc.getDescriptorId(), tenantId);
        			ErrorLog.logError(e.getStackTrace());
        		}
        	}
        	for(Actions act : (policy.getActions() == null) ? Collections.<Actions>emptyList() : policy.getActions()){
        		try {
        			LOG.info("Loading Action {} from Storage for Tenant {}", act.getActionId(), tenantId);
        			addAction(act);
        		} catch (Exception e){
        			LOG.error("PolicyManager- Error during Action load for Tenant {} / Policy {}", act.getActionId(), tenantId);
        			ErrorLog.logError(e.getStackTrace());
        		}
        	}
        	for(Policies pol : (policy.getPolicies() == null) ? Collections.<Policies>emptyList() : policy.getPolicies()){
        		try{
        			LOG.info("Loading Policy {} from Storage for Tenant {}", pol.getPolicyId(), tenantId);
        			addPolicy(pol);
        		} catch (Exception e){
        			LOG.error("PolicyManager- Error during Policies load for Tenant {} / Policy {}", pol.getPolicyId(), tenantId);
        			ErrorLog.logError(e.getStackTrace());
        		}
        	}
        	for(PolicyGroups polgro : (policy.getPolicyGroups() == null) ? Collections.<PolicyGroups>emptyList() : policy.getPolicyGroups()){
        		try{
        			LOG.info("Loading Policy Group {} from Storage for Tenant {}", polgro.getPolicyGroupId(), tenantId);
        			addPolicyGroups(polgro);
        		} catch (Exception e){
        			LOG.error("PolicyGroupManager- Error during Policies load for Tenant {} / Policy {}", polgro.getPolicyGroupId(), tenantId);
        			ErrorLog.logError(e.getStackTrace());
        		}
        	}
        }
    }

    /**
     * Adds a Descriptor to this manager.
     * @param desc - Descriptor to add
     * @throws Exception - If the Add violates the existing state.
     */
    abstract public void addDescriptor(Descriptors desc) throws Exception;

    /**
     * Removes a Descriptor from this manager.
     * @param desc - Descriptor to remove
     */
    abstract public void removeDescriptor(Descriptors desc);

    /**
     * Registers Change Listener for Descriptors under the Tenant.
     */
 
    public void registerListeners() {
        dataTreeChangeListenerRegistration = this.db
                   .registerDataTreeChangeListener(
                           new DataTreeIdentifier<Descriptors>(LogicalDatastoreType.CONFIGURATION,
                                  InstanceIdentifier.builder(Tenants.class)
                                    .child(Tenant.class, new TenantKey( tenantId ))
                                    .child(FpcPolicy.class)
                                    .child(Descriptors.class).build() ),
                               new DescriptorChangeManager() );
        dataTreeChangeListenerRegistration2 = this.db
        		.registerDataTreeChangeListener(
        				new DataTreeIdentifier<Actions>(LogicalDatastoreType.CONFIGURATION,
                                InstanceIdentifier.builder(Tenants.class)
                                  .child(Tenant.class, new TenantKey( tenantId ))
                                  .child(FpcPolicy.class)
                                  .child(Actions.class).build() ),
                             new ActionsChangeManager() );
        dataTreeChangeListenerRegistration3 = this.db
        		.registerDataTreeChangeListener(
        				new DataTreeIdentifier<Policies>(LogicalDatastoreType.CONFIGURATION,
                                InstanceIdentifier.builder(Tenants.class)
                                  .child(Tenant.class, new TenantKey( tenantId ))
                                  .child(FpcPolicy.class)
                                  .child(Policies.class).build() ),
                             new PolicyChangeManager() );
        dataTreeChangeListenerRegistration4 = this.db
        		.registerDataTreeChangeListener(
        				new DataTreeIdentifier<PolicyGroups>(LogicalDatastoreType.CONFIGURATION,
                                InstanceIdentifier.builder(Tenants.class)
                                  .child(Tenant.class, new TenantKey( tenantId ))
                                  .child(FpcPolicy.class)
                                  .child(PolicyGroups.class).build() ),
                             new PolicyGroupChangeManager() );
        dataTreeChangeListenerRegistration5 = this.db
        		.registerDataTreeChangeListener(
        				new DataTreeIdentifier<Ports>(LogicalDatastoreType.OPERATIONAL,
                                InstanceIdentifier.builder(Tenants.class)
                                  .child(Tenant.class, new TenantKey( tenantId ))
                                  .child(FpcMobility.class)
                                  .child(Ports.class).build() ),
                             new PortChangeManager() );
        LOG.info("DescriptorChangeManager Registered for Tenant {}", tenantId);
        LOG.info("ActionsChangeManager Registered for Tenant {}", tenantId);
        LOG.info("PolicyChangeManager Registered for Tenant {}", tenantId);
        LOG.info("PolicyGroupChangeManager Registered for Tenant {}", tenantId);
        LOG.info("PortChangeManager Registered for Tenant {}", tenantId);
    }

    @Override
    public void close() {
        dataTreeChangeListenerRegistration.close();
        dataTreeChangeListenerRegistration2.close();
        dataTreeChangeListenerRegistration3.close();
        dataTreeChangeListenerRegistration4.close();
        dataTreeChangeListenerRegistration5.close();
    }

    
    /**
     * Private Change Listener class for Descriptors.
     */
    private class DescriptorChangeManager implements DataTreeChangeListener<Descriptors> {
         @Override
         public void onDataTreeChanged(Collection<DataTreeModification<Descriptors>> changes) {
             for (DataTreeModification<Descriptors> descModification : changes) {
                 LOG.info("Descriptor Change has occured for Tenant-Id {} / Descriptor-Id {}",tenantId,
                         descModification.getRootPath().toString());
                 if (descModification.getRootNode().getModificationType() == ModificationType.DELETE) {
                     removeDescriptor(descModification.getRootNode().getDataBefore());
                 } else {
                     try {
                        addDescriptor(descModification.getRootNode().getDataAfter());
                    } catch (Exception e) {
                        ErrorLog.logError("DescriptorChangeManager - Error occured during Descriptor Create/Write - " + e.getLocalizedMessage(), e.getStackTrace());
                    }
                 }

             }
         }
    }
    
    /**
     * Adds an Action to this manager.
     * @param act - Action to add
     * @throws Exception - If the Add violates the existing state.
     */
    abstract public void addAction(Actions act) throws Exception;

    /**
     * Removes an Action from this manager.
     * @param act - Action to remove
     */
    abstract public void removeAction(Actions act);
    
    /**
     * Registers Change Listener for Actions under the Tenant.
     */
    
    private class ActionsChangeManager implements DataTreeChangeListener<Actions> {
        @Override
        public void onDataTreeChanged(Collection<DataTreeModification<Actions>> changes) {
            for (DataTreeModification<Actions> actModification : changes) {
                LOG.info("Action Change has occured for Tenant-Id {} / Action-Id {}",tenantId,
                        actModification.getRootPath().toString());
                if (actModification.getRootNode().getModificationType() == ModificationType.DELETE) {
                    removeAction(actModification.getRootNode().getDataBefore());
                } else {
                    try {
                       addAction(actModification.getRootNode().getDataAfter());
                   } catch (Exception e) {
                       ErrorLog.logError("ActionsChangeManager - Error occured during Action Create/Write - " + e.getLocalizedMessage(), e.getStackTrace());
                   }
                }

            }
        }
   }
    
    /**
     * Adds a Policy to this manager.
     * @param policy - Policy to add
     * @throws Exception - If the Add violates the existing state.
     */
    abstract public void addPolicy(Policies policy) throws Exception;
    
    /**
     * Removes a Policy from this manager.
     * @param policy - Policy to remove
     */
    abstract public void removePolicy(Policies policy);
    
    /**
     * Registers Change Listener for Policies under the Tenant.
     */
    
    private class PolicyChangeManager implements DataTreeChangeListener<Policies> {
        @Override
        public void onDataTreeChanged(Collection<DataTreeModification<Policies>> changes) {
            for (DataTreeModification<Policies> policyModification : changes) {
                LOG.info("Policy Change has occured for Tenant-Id {} / Policy-Id {}",tenantId,
                        policyModification.getRootPath().toString());
                if (policyModification.getRootNode().getModificationType() == ModificationType.DELETE) {
                    removePolicy(policyModification.getRootNode().getDataBefore());
                } else {
                    try {
                       addPolicy(policyModification.getRootNode().getDataAfter());
                   } catch (Exception e) {
                       ErrorLog.logError("PolicyChangeManager - Error occured during Policy Create/Write - " + e.getLocalizedMessage(), e.getStackTrace());
                   }
                }

            }
        }
   }
    /**
     * Adds a Policy Group to this manager.
     * @param polgro - PolicyGroup to add
     * @throws Exception - If the Add violates the existing state.
     */
    abstract public void addPolicyGroups(PolicyGroups polgro) throws Exception;
    
    /**
     * Removes a Policy Group from this manager.
     * @param polgro - Policy Group to remove
     */
    abstract public void removePolicyGroups(PolicyGroups polgro);
    
    /**
     * Registers Change Listener for Policies under the Tenant.
     */
    
    private class PolicyGroupChangeManager implements DataTreeChangeListener<PolicyGroups> {
        @Override
        public void onDataTreeChanged(Collection<DataTreeModification<PolicyGroups>> changes) {
            for (DataTreeModification<PolicyGroups> policygroupModification : changes) {
                LOG.info("Policy Group Change has occured for Tenant-Id {} / Policy-Group-Id {}",tenantId,
                        policygroupModification.getRootPath().toString());
                if (policygroupModification.getRootNode().getModificationType() == ModificationType.DELETE) {
                    removePolicyGroups(policygroupModification.getRootNode().getDataBefore());
                } else {
                    try {
                       addPolicyGroups(policygroupModification.getRootNode().getDataAfter());
                   } catch (Exception e) {
                       ErrorLog.logError("PolicyGroupChangeManager - Error occured during PolicyGroup Create/Write - " + e.getLocalizedMessage(), e.getStackTrace());
                   }
                }

            }
        }
   }
    /**
     * Adds a Policy Group to this manager.
     * @param polgro - PolicyGroup to add
     * @throws Exception - If the Add violates the existing state.
     */
    abstract public void addPorts(Ports port) throws Exception;
    
    /**
     * Removes a Policy Group from this manager.
     * @param polgro - Policy Group to remove
     */
    abstract public void removePorts(Ports port);
    
    private class PortChangeManager implements DataTreeChangeListener<Ports> {
        @Override
        public void onDataTreeChanged(Collection<DataTreeModification<Ports>> changes) {
            for (DataTreeModification<Ports> descModification : changes) {
                LOG.info("Descriptor Change has occured for Tenant-Id {} / Descriptor-Id {}",tenantId,
                        descModification.getRootPath().toString());
                if (descModification.getRootNode().getModificationType() == ModificationType.DELETE) {
                    removePorts(descModification.getRootNode().getDataBefore());
                } else {
                    try {
                       addPorts(descModification.getRootNode().getDataAfter());
                   } catch (Exception e) {
                       ErrorLog.logError("DescriptorChangeManager - Error occured during Descriptor Create/Write - " + e.getLocalizedMessage(), e.getStackTrace());
                   }
                }

            }
        }
   }
}

