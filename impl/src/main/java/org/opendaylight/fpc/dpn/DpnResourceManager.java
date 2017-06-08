/*
 * Copyright © 2016 - 2017 Copyright (c) Sprint, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.fpc.dpn;

import java.util.Collection;
import java.util.Collections;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataObjectModification.ModificationType;
import org.opendaylight.controller.md.sal.binding.api.DataTreeChangeListener;
import org.opendaylight.controller.md.sal.binding.api.DataTreeIdentifier;
import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.fpc.utils.ErrorLog;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.Tenants;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.tenants.Tenant;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.tenants.TenantKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.tenants.tenant.FpcTopology;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.tenants.tenant.fpc.topology.Dpns;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcbase.rev160803.FpcIdentity;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract base class that loads DPNs from the data store and sets up change listeners.
 */
abstract public class DpnResourceManager implements AutoCloseable {
    protected static final Logger LOG = LoggerFactory.getLogger(DpnResourceManager.class);

    protected FpcIdentity tenantId;
    protected DataBroker db;
    protected ListenerRegistration<DpnChangeManager> dataTreeChangeListenerRegistration;

    /**
     * Default Constructor.
     */
    protected DpnResourceManager() {
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

        // Load Stored DPNs from the Tenant
        FpcTopology topo = tenant.getFpcTopology();
        if (topo != null) {
             for (Dpns dpns : (topo.getDpns() == null) ? Collections.<Dpns>emptyList() : topo.getDpns()) {
                try {
                    LOG.info("Loading DPN {} from Storage for Tenant {}", tenantId, dpns.getDpnId());
                    addDpn(dpns);
                } catch (Exception e) {
                    LOG.error("DpnResourceManager - Error during DPN load for Tenant {} / DPN {} ", tenantId,
                            dpns.getDpnId());
                    ErrorLog.logError(e.getStackTrace());
                }
            }
        }
    }

    /**
     * Adds a DPN to this manager.
     * @param dpn - DPN to add
     * @throws Exception - If the Add violates the existing state.
     */
    abstract public void addDpn(Dpns dpn) throws Exception;

    /**
     * Removes a DPN from this manager.
     * @param dpn - DPN to remove
     */
    abstract public void removeDpn(Dpns dpn);

    /**
     * Registers Change Listener for Dpns under the Tenant.
     */
    public void registerListeners() {
        dataTreeChangeListenerRegistration = this.db
                   .registerDataTreeChangeListener(
                           new DataTreeIdentifier<Dpns>(LogicalDatastoreType.CONFIGURATION,
                                  InstanceIdentifier.builder(Tenants.class)
                                    .child(Tenant.class, new TenantKey( tenantId ))
                                    .child(FpcTopology.class)
                                    .child(Dpns.class).build() ),
                               new DpnChangeManager() );
        LOG.info("DpnChangeManager Registered for Tenant {}", tenantId);
    }

    @Override
    public void close() {
        dataTreeChangeListenerRegistration.close();
    }

    /**
     * Private Change Listener class for Dpns.
     */
    private class DpnChangeManager implements DataTreeChangeListener<Dpns> {
         @Override
         public void onDataTreeChanged(Collection<DataTreeModification<Dpns>> changes) {
             for (DataTreeModification<Dpns> dpnModification : changes) {
                 LOG.info("Dpn Change has occured for Tenant-Id {} / DPN-Id {}",tenantId,
                         dpnModification.getRootPath().toString());
                 if (dpnModification.getRootNode().getModificationType() == ModificationType.DELETE) {
                     removeDpn(dpnModification.getRootNode().getDataBefore());
                 } else {
                     try {
                        addDpn(dpnModification.getRootNode().getDataAfter());
                    } catch (Exception e) {
                        ErrorLog.logError("DpnChangeManager - Error occured during DPN Create/Write - " + e.getLocalizedMessage(), e.getStackTrace());
                    }
                 }

             }
         }
    }
}
