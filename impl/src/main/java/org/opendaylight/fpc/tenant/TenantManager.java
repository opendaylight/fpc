/*
 * Copyright © 2016 Copyright (c) Sprint, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.fpc.tenant;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.fpc.activation.ActivationManager;
import org.opendaylight.fpc.activation.ActivatorFactory;
import org.opendaylight.fpc.activation.cache.StorageCache;
import org.opendaylight.fpc.assignment.AssignmentManager;
import org.opendaylight.fpc.dpn.DpnHolder;
import org.opendaylight.fpc.impl.FpcProvider;
import org.opendaylight.fpc.utils.ErrorLog;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.ClientIdentifier;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.Tenants;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.tenants.Tenant;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.tenants.TenantBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.tenants.TenantKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.tenants.tenant.FpcMobilityBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.tenants.tenant.FpcPolicyBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.tenants.tenant.FpcTopology;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.tenants.tenant.FpcTopologyBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.tenants.tenant.fpc.mobility.Ports;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.tenants.tenant.fpc.mobility.PortsBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.tenants.tenant.fpc.mobility.PortsKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.tenants.tenant.fpc.topology.Dpns;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcbase.rev160803.FpcDpnControlProtocol;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcbase.rev160803.FpcDpnId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcbase.rev160803.FpcIdentity;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcbase.rev160803.FpcPortId;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;

/**
 * A Generic Object for Tenant specific information.
 */
public class TenantManager implements AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(TenantManager.class);
    private static final Map<String, TenantManager> tenants = new HashMap<String, TenantManager>();
    private static final Map<String, TenantManager> clientIdToTenants = new HashMap<String, TenantManager>();
    private static final List<ActivatorFactory> defaultActivatorFactories = new ArrayList<ActivatorFactory>();
    private static DataBroker dataBroker;
    //public static Map<FpcIdentity, ArrayList<FpcDpnId>> absDpnMap =
    		//new HashMap<FpcIdentity, ArrayList<FpcIdentity>>(); *previously*
    public static Map<FpcDpnId, List<FpcDpnId>> absDpnMap = 
    		new HashMap<FpcDpnId, List<FpcDpnId>>();

    protected StorageCache sc;
    protected final Tenant tenant;
    protected final Map<String, DpnHolder> dpnInfo;
    protected AssignmentManager assignmentManager;
    protected final ActivationManager activationManager;

    /**
     * Creates a Tenant Manager.
     * @param tenant - Tenant Identity
     * @return Tenant Manager for the specified tenant identity.
     */
    static public TenantManager populateTenant(FpcIdentity tenant) {
        return populateTenant(tenant, null);
    }

    /**
     * Creates a Tenant Manager.
     * @param tenant - Tenant Identity
     * @param cpFactories - Control Protocols for the Tenant
     * @return Tenant Manager for the specified tenant identity.
     */
    static public TenantManager populateTenant(FpcIdentity tenant,
            Map<Class<? extends FpcDpnControlProtocol>,ActivatorFactory> cpFactories) {
        if (dataBroker == null) {
            dataBroker = FpcProvider.getInstance().getDataBroker();
        }
        TenantManager retVal = new TenantManager(tenant, cpFactories);
        tenants.put(tenant.toString(), retVal);
        return retVal;
    }

    /**
     * Provides the Tenant Managers.
     * @return A Map of Tenant Identifiers to their Tenant Managers.
     */
    public static Map<String, TenantManager> getTenantsState() {
        return tenants;
    }

    /**
     * Provides specified Tenant Manager.
     * @param id - Tenant Identity
     * @return Tenant Manager for provided id; null otherwise.
     */
    public static TenantManager getTenantManager(FpcIdentity id) {
        return tenants.get(id.toString());
    }

    /**
     * Provides specified Tenant Manager.
     * @param id - Tenant Identity
     * @return Tenant Manager for provided id; null otherwise.
     */
    public static TenantManager getTenantManager(String id) {
        return tenants.get(id);
    }

    /**
     * Registers a Client Identity with a Tenant Identity.
     * @param clientId - Client Identity
     * @param tenantId - Tenant Identity
     * @return Tenant Manager for provided id; null otherwise.
     */
    public static TenantManager registerClient(ClientIdentifier clientId, FpcIdentity tenantId) {
        if (tenants.get(tenantId.toString()) != null) {
            clientIdToTenants.put(clientId.toString(), tenants.get(tenantId.toString()));
        }
        return tenants.get(tenantId.toString());
    }

    /**
     * De-registers a Client Identity from its Tenant.
     * @param clientId - Client Identity
     * @return Tenant Manager the Client Id was registered to; null otherwise.
     */
    public static TenantManager deregisterClient(ClientIdentifier clientId) {
        return clientIdToTenants.remove(clientId.toString());
    }

    /**
     * Retrieves a Tenant Manager.
     * @param clientId - Client Identifier
     * @return Tenant Manager the Client is associated to, null otherwise.
     */
    public static TenantManager getTenantManagerForClient(ClientIdentifier clientId) {
        return clientIdToTenants.get(clientId.toString());
    }

    /**
     * Adds a Default Activator Factory.  These factories are assigned to Tenant Managers
     * during construction if none are provided in constructor related calls.
     *
     * @param factory - Activator Factory
     */
    public static void addDefaultActivatorFactory(ActivatorFactory factory) {
        defaultActivatorFactories.add(factory);
        for (TenantManager tenantContext : tenants.values()) {
            factory.register(tenantContext.activationManager);
        }
    }

    /**
     * Removes a Default Activator Factory.
     * @param factory - Activator Factory
     */
    public static void removeDefaultActivatorFactory(ActivatorFactory factory) {
        defaultActivatorFactories.remove(factory);
        for (TenantManager tenantContext : tenants.values()) {
            factory.deRegister(tenantContext.activationManager);
        }
    }

    /**
     * Adds an Activator Factory to a Tenant.
     * @param factory - Activator Factory
     * @param tenant - Tenant Identity
     */
    public static void addActivatorFactory(ActivatorFactory factory, FpcIdentity tenant) {
        if (tenants.get(tenant.toString()) != null) {
            factory.register(tenants.get(tenant.toString()).activationManager);
        }
    }

    /**
     * Removes an Activator Factory from a Tenant.
     * @param factory - Activator Factory
     * @param tenant - Tenant Identity
     */
    public static void removeActivatorFactory(ActivatorFactory factory, FpcIdentity tenant) {
        if (tenants.get(tenant.toString()) != null) {
            factory.deRegister(tenants.get(tenant.toString()).activationManager);
        }
    }

    /**
     * Primary Constructor.
     * @param tenantId - Tenant Identity
     * @param cpFactories - Activator Factories
     */
    protected TenantManager(FpcIdentity tenantId,
            Map<Class<? extends FpcDpnControlProtocol>,ActivatorFactory> cpFactories) {
        Tenant t = getTenant(tenantId);
        this.assignmentManager = null;
        List<Ports> ara = new ArrayList<Ports>();
        FpcPortId ident = new FpcPortId("pre-prov");
        ara.add(new PortsBuilder()
                .setPortId(ident)
                .setKey(new PortsKey(ident))
                .build());
        this.dpnInfo = new HashMap<String, DpnHolder>();
        this.tenant = (t == null) ? new TenantBuilder()
                .setTenantId(tenantId)
                .setKey(new TenantKey(tenantId))
                .setFpcMobility(new FpcMobilityBuilder()
                //        .setPorts(ara)
                        .build())
                .setFpcPolicy(new FpcPolicyBuilder().build())
                .setFpcTopology(new FpcTopologyBuilder().build())
                .build() : t;
        this.activationManager = new ActivationManager(this, dataBroker);
        if (cpFactories != null) {
            if (!cpFactories.isEmpty()) {
                for (Map.Entry<Class<? extends FpcDpnControlProtocol>,ActivatorFactory> entry : cpFactories.entrySet()) {
                    activationManager.addActivatorFactory(entry.getKey(), entry.getValue());
                }
            }
        }
        initActivationManager();
        this.sc = new StorageCache(dataBroker, this);
    }

    /**
     * Generates an Assignment Manager for the Tenant.
     */
    public void genAssignmentManager() {
        try {
            this.assignmentManager = new AssignmentManager(this,dataBroker);
        } catch (Exception e) {
            LOG.error("Error in attempt by Tenant Manager to create an AssignmentManager");
            ErrorLog.logError(e.getStackTrace());
        }
    }

	/**
	 * Retrieves the Storage Cache.
	 * @return StorageCache for the Tenant
	 */
    public StorageCache getSc() {
        return sc;
    }

    /**
     * Retrieves the Tenant.
     * @return Tenant this instance manages
     */
    public Tenant getTenant() {
        return tenant;
    }

    /**
     * Retrieves the Assignment Manager
     * @return AssignmentManager for the Tenant
     */
    public AssignmentManager getAssignmentManager() {
        return assignmentManager;
    }

    /**
     * Retrieves the Activation Manager.
     * @return Activation Manager for the Tenant
     */
    public ActivationManager getActivationManager() {
        return activationManager;
    }

    /**
     * Retrieves DPN Information
     * @return A Map of DPN Identities to DpnHolder (DPN information).
     */
    public Map<String, DpnHolder> getDpnInfo() {
        return dpnInfo;
    }

    /**
     * Reads the Tenant from Storage
     * @param tenantId - Tenant Identity
     * @return Tenant or null if not present in storage
     */
    private Tenant getTenant(FpcIdentity tenantId) {
        ReadOnlyTransaction rtrans = dataBroker.newReadOnlyTransaction();
        Optional<Tenant> dTenant;
        try {
            dTenant = rtrans.read(LogicalDatastoreType.CONFIGURATION,
                        InstanceIdentifier.create(Tenants.class)
                            .child(Tenant.class, new TenantKey( tenantId ))).get();
            return (dTenant.isPresent()) ? dTenant.get() : null;
        } catch (InterruptedException e) {
            LOG.warn("TenantManager may not be initialized - Error occurred during Configuration Store read for DPN Toplogy");
            ErrorLog.logError(e.getStackTrace());
        } catch (ExecutionException e) {
            LOG.warn("TenantManager may not be initialized - Error occurred during Configuration Store read for DPN Toplogy");
            ErrorLog.logError(e.getStackTrace());
        }
        return null;
    }

    /**
     * Initializes the Activation Manager.
     */
    protected void initActivationManager() {
        for (ActivatorFactory factory : defaultActivatorFactories) {
            factory.register(activationManager);
        }
        if (tenant != null) {
            FpcTopology topo = tenant.getFpcTopology();
            if (topo != null) {
                for (Dpns dpns : (topo.getDpns() == null) ? Collections.<Dpns>emptyList() : topo.getDpns()) {
                    try {
                        LOG.info("Loading DPN " + dpns.getDpnId() + " from Storage");
                        activationManager.addDpn(dpns);
                    } catch (Exception e) {
                        LOG.error("TenantManager - Error during DPN load for " + dpns.getDpnId());
                        ErrorLog.logError(e.getStackTrace());
                    }
                }
            }
        }
    }

    @Override
    public void close() throws Exception {
        if (activationManager != null) {
            activationManager.close();
        }
    }
}
