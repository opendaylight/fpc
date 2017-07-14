/*
 * Copyright © 2016 Copyright (c) Sprint, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.fpc.activation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.fpc.activation.cache.transaction.Transaction;
import org.opendaylight.fpc.dpn.DpnHolder;
import org.opendaylight.fpc.dpn.DpnResourceManager;
import org.opendaylight.fpc.tenant.TenantManager;
import org.opendaylight.fpc.utils.ErrorLog;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.payload.Contexts;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.tenants.Tenant;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.tenants.tenant.fpc.topology.Dpns;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcbase.rev160803.FpcContextId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcbase.rev160803.FpcDpnControlProtocol;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcbase.rev160803.FpcDpnId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.fpc.rev150105.ZmqDpnControlProtocol;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Activation Managers map DPNs to Activator Classes.  Activator Factories are mapped by the
 * FpcDpnControlProtocol protocol supported.  When a DPN is added to the tenant (this is set
 * up by the parent {@link org.opendaylight.fpc.dpn.DpnResourceManager DpnResourceManager} , the class attempt to locate
 * the most appropriate ActivatorFactory and assign it to the DPN's id.
 */
public class ActivationManager extends DpnResourceManager implements AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(ActivationManager.class);

    private final Map<Class<? extends FpcDpnControlProtocol>,ActivatorFactory> factories;
    private final TenantManager tenantMgr;
    private boolean initialized;

    /**
     * Primary Constructor.
     * @param tenantMgr - Tenant Manager Assigned to the Activation Manager.
     * @param dataBroker - Data Broker
     */
    public ActivationManager(TenantManager tenantMgr,
            DataBroker dataBroker) {
        super();
        initialized = false;
        this.tenantMgr = tenantMgr;
        factories = new java.util.HashMap<Class<? extends FpcDpnControlProtocol>,ActivatorFactory>();
        init(dataBroker, tenantMgr.getTenant());
    }

    /**
     * Retrieves the mappings of FpcDpnControlProtocol to {@link org.opendaylight.fpc.activation.ActivatorFactory ActivatorFactory}.
     *
     * @return A Map of FpcDpnControlProtocol instances mapped to Activator Factories.
     */
    public Map<Class<? extends FpcDpnControlProtocol>,ActivatorFactory> getActivatorFactoriess() {
        return factories;
    }

    /**
     * Assigns an {@link org.opendaylight.fpc.activation.ActivatorFactory ActivatorFactory} to a specific Control Protocol (FpcDpnControlProtocol).
     *
     * @param protocol - DPN Control Protocol
     * @param activator - Activator Factory assigned to the protocol
     */
    public void addActivatorFactory(Class<? extends FpcDpnControlProtocol> protocol, ActivatorFactory activator) {
        LOG.info("Activation Manager - Adding Activator " + protocol + " with factory: " + activator);
        factories.put(protocol, activator);
    }

    /**
     * Removes any ActivatorFactory assigned to the DPN Control Protocol.
     * @param protocol - DPN Control Protocol
     */
    public void removeActivatorFactory(Class<? extends FpcDpnControlProtocol> protocol) {
        LOG.info("Activation Manager - Removing Activator " + protocol);
        factories.remove(protocol);
    }
    
    @Override
    public void updateDpn(Dpns dpnBefore, Dpns dpnAfter) throws Exception {
    	if(dpnBefore.isAbstract().compareTo(dpnAfter.isAbstract()) != 0){
    		ErrorLog.logError("Cannot change abstract value of a dpn", null);
    		throw new Exception();
    	}
    }

    @Override
    public void addDpn(Dpns dpn) throws Exception {
    	LOG.info("addDpn called");
        ActivatorFactory factory = null;
        if (dpn != null) {
        	if(dpn.isAbstract()){
        		if(TenantManager.vdpnDpnsMap.get(dpn.getDpnId()) == null)
        			TenantManager.vdpnDpnsMap.put(dpn.getDpnId(), new ArrayList<FpcDpnId>());
        		if(TenantManager.vdpnContextsMap.get(dpn.getDpnId()) == null)
        			TenantManager.vdpnContextsMap.put(dpn.getDpnId(), new HashMap<Contexts, Transaction>());
        	}
            DpnHolder dpnHolder = tenantMgr.getDpnInfo().get(dpn.getDpnId().toString());
            if (dpnHolder == null) {
                dpnHolder = new DpnHolder(dpn);
                tenantMgr.getDpnInfo().put(dpn.getDpnId().toString(), dpnHolder);
            }
            if (dpnHolder.activator != null) {
                LOG.info("Activation Manager - Applying Configuration for Dpn-ID: " +  dpn.getDpnId().getString());
                dpnHolder.activator.applyConfiguration(dpnHolder);
            } else {
                // TODO - Determine if we need a special 'default control protocol or throw an error
                factory = factories.get(ZmqDpnControlProtocol.class);
                if (factory != null) {
                    LOG.info("Activation Manager - Attempting Activator creation of type " + factory.getClass().toString()
                                + " for Dpn-ID: " +  dpn.getDpnId().getString());
                    dpnHolder.activator = factory.newInstance(dpnHolder);
                } else {
                    LOG.info("Activation Manager - No activator found for Dpn-ID: " +  dpn.getDpnId().getString() + " and type " +
                            ZmqDpnControlProtocol.class);
                }
            }
            if (dpnHolder.activator != null) {
            	
            	//TODO addDpn Logic
            	
            }
            
        } else {
            LOG.info("Activation Manager - DPN from parent call was null");
        }
    }

    @Override
    public void removeDpn(Dpns dpn) {
        if (dpn != null) {
            LOG.info("Activation Manager - Removing Dpn " + dpn.getDpnId());
            
            //TODO removeDpn Logic
            
            tenantMgr.getDpnInfo().remove(dpn.getDpnId().toString());
            if(dpn.isAbstract()){
            	TenantManager.vdpnDpnsMap.remove(dpn.getDpnId());
            	TenantManager.vdpnContextsMap.remove(dpn.getDpnId());
            }
        }
    }

    @Override
    protected void init(DataBroker db, Tenant tenant) {
        if (!initialized) {
            super.init(db, tenant);
            this.db = db;
            this.initialized = true;
            registerListeners();
            LOG.info("Factory initialization completed");
        }
    }    
}
