/*
 * Copyright © 2016 - 2017 Copyright (c) Sprint, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.fpc.dpn;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataObjectModification.ModificationType;
import org.opendaylight.controller.md.sal.binding.api.DataTreeChangeListener;
import org.opendaylight.controller.md.sal.binding.api.DataTreeIdentifier;
import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.fpc.activation.impl.dpdkdpn.DpnAPIListener;
import org.opendaylight.fpc.impl.FpcProvider;
import org.opendaylight.fpc.tenant.TenantManager;
import org.opendaylight.fpc.utils.ErrorLog;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.Tenants;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.fpc.dpn.group.DpnGroupPeers;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.tenants.Tenant;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.tenants.TenantKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.tenants.tenant.FpcTopology;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.tenants.tenant.fpc.topology.DpnGroups;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.tenants.tenant.fpc.topology.Dpns;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcbase.rev160803.FpcAccessType;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcbase.rev160803.FpcDpnGroupId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcbase.rev160803.FpcDpnId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcbase.rev160803.FpcForwaridingplaneRole;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcbase.rev160803.FpcIdentity;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Maps Remote IP addresses and Technology Types to DPN Groups
 */
public class DpnAssignmentMgr extends DpnResourceManager implements AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(DpnAssignmentMgr.class);

    private static final Map<String, String> globalDpnIdMap = new HashMap<String, String>();

    /**
     * Retrieves Tenant Identity for a given DPN identity.
     * @param identity - DPN Identity
     * @return Tenant Identity for the given DPN identity, if present, null otherwise.
     */
    public static final String getTenant(FpcDpnId identity) {
        return globalDpnIdMap.get(identity.toString());
    }

    private ListenerRegistration<DpnGroupChangeManager> groupsDataTreeChangeListenerRegistration;
    private Map<PeerIpAcessRoleKey, Set<String>> accessAndRoleToDpnGroups;
    private Map<String, Set<String>> dpnGroupsToDpns;
    private Map<PeerIpAcessRoleKey, Set<String>> remoteIpAddressToDpnGroups;
    private final TenantManager tenantMgr;

    /**
     * Tracks Peer Ip Access roles.
     */
    @SuppressWarnings("unused")
    public class PeerIpAcessRoleKey {
        private Class<? extends FpcAccessType> accessType;
        private Class<? extends FpcForwaridingplaneRole> accessRole;
        private String peerIpAddress;
        /**
         * Constructor
         * @param accessType - Access Type
         * @param accessRole - Access Role
         * @param peerIpAddress - Peer IP Address
         */
        public PeerIpAcessRoleKey(Class<? extends FpcAccessType> accessType,
                Class<? extends FpcForwaridingplaneRole> accessRole,
                String peerIpAddress) {
            this.accessType = accessType;
            this.accessRole = accessRole;
            this.peerIpAddress = peerIpAddress;
        }
    }

    /**
     * Primary Constructor.
     * @param tenantMgr - Tenant Manager
     * @param db - Data Broker
     */
    public DpnAssignmentMgr(TenantManager tenantMgr, DataBroker db) {
        super();
        this.tenantMgr = tenantMgr;
        init(db, tenantMgr);
    }

    /**
     * Initialization function.
     * @param db - Data Broker
     * @param tenantMgr - Tenant Manager the Assignment Manager is bound to
     */
    public void init(DataBroker db,
                    TenantManager tenantMgr) {
        accessAndRoleToDpnGroups = new HashMap<PeerIpAcessRoleKey, Set<String>>();
        dpnGroupsToDpns = new HashMap<String, Set<String>>();
        remoteIpAddressToDpnGroups = new HashMap<PeerIpAcessRoleKey, Set<String>>();
        super.init(db, tenantMgr.getTenant());

        // Load Stored DPNs
        if (tenantMgr.getTenant().getFpcTopology() != null) {
            for (DpnGroups group : (tenantMgr.getTenant().getFpcTopology().getDpnGroups() == null) ?
                    Collections.<DpnGroups>emptyList() : tenantMgr.getTenant().getFpcTopology().getDpnGroups()) {
                try {
                    loadDpnGroup(group);
                } catch (Exception e) {
                    LOG.error("DpnAssignmentMgr - Error during DPN load for " + group.getDpnGroupId());
                    ErrorLog.logError(e.getStackTrace());
                }
            }
        }
    }

    @Override
    public void registerListeners() {
        super.registerListeners();
        String defaultTenant = FpcProvider.getInstance().getConfig().getDefaultTenantId();
        FpcIdentity defaultIdentity = (defaultTenant == null) ?  new FpcIdentity(0L) :  new FpcIdentity(defaultTenant);

        groupsDataTreeChangeListenerRegistration = this.db
                   .registerDataTreeChangeListener(
                           new DataTreeIdentifier<DpnGroups>(LogicalDatastoreType.CONFIGURATION,
                                   InstanceIdentifier.builder(Tenants.class)
                                     .child(Tenant.class, new TenantKey( defaultIdentity ))
                                     .child(FpcTopology.class)
                                     .child(DpnGroups.class).build() ),
                               new DpnGroupChangeManager() );
        LOG.info("DpnManager Registered");
    }

    /**
     * Loads a Dpn Group into this manager.
     * @param group - DpnGroups to be added
     */
    private void loadDpnGroup(DpnGroups group) {
        PeerIpAcessRoleKey byAccessRole = new PeerIpAcessRoleKey(group.getAccessType(),
                group.getDataPlaneRole(),
                "all");
        Set<String> groupsByAccessAndRole = accessAndRoleToDpnGroups.get(byAccessRole);
        if (groupsByAccessAndRole == null) {
            groupsByAccessAndRole = new HashSet<String>();
            accessAndRoleToDpnGroups.put(byAccessRole, groupsByAccessAndRole);
        }
        groupsByAccessAndRole.add(group.getDpnGroupId().toString());

        Set<String> groupsToDpns = dpnGroupsToDpns.get(group.getDpnGroupId().toString());
        if (groupsToDpns == null) {
            groupsToDpns = new HashSet<String>();
            dpnGroupsToDpns.put(group.getDpnGroupId().toString(), groupsToDpns);
        }

        for (DpnGroupPeers peerGroup : (group.getDpnGroupPeers() != null) ? group.getDpnGroupPeers() :
            Collections.<DpnGroupPeers>emptyList()) {
            if (peerGroup.getRemoteEndpointAddress() != null) {
                PeerIpAcessRoleKey byIpAccessRole = new PeerIpAcessRoleKey(group.getAccessType(),
                        group.getDataPlaneRole(),
                        peerGroup.getRemoteEndpointAddress().toString());
                Set<String> dpnGroups = remoteIpAddressToDpnGroups.get(byIpAccessRole);
                if (dpnGroups == null) {
                    groupsByAccessAndRole = new HashSet<String>();
                    remoteIpAddressToDpnGroups.put(byIpAccessRole, dpnGroups);
                }
                dpnGroups.add(group.getDpnGroupId().toString());
            }
        }
    }

    /**
     * Removes a Dpn Group from this manager.
     * @param group - DpnGroups to be removed
     */
    private void removeDpnGroup(DpnGroups group) {
        accessAndRoleToDpnGroups.remove(new PeerIpAcessRoleKey(group.getAccessType(),
                group.getDataPlaneRole(),
                "all"));
        dpnGroupsToDpns.remove(group.getDpnGroupId().toString());

        for (DpnGroupPeers peerGroup : (group.getDpnGroupPeers() != null) ? group.getDpnGroupPeers() :
            Collections.<DpnGroupPeers>emptyList()) {
            if (peerGroup.getRemoteEndpointAddress() != null) {
                remoteIpAddressToDpnGroups.remove(new PeerIpAcessRoleKey(group.getAccessType(),
                        group.getDataPlaneRole(),
                        peerGroup.getRemoteEndpointAddress().toString()));
            }
        }
    }

    /**
     * Change Listener for DPNGroups.
     */
    private class DpnGroupChangeManager implements DataTreeChangeListener<DpnGroups> {
         @Override
         public void onDataTreeChanged(Collection<DataTreeModification<DpnGroups>> changes) {
             for (DataTreeModification<DpnGroups> dpnGroupModification : changes) {
                 LOG.info("Dpn Groups Change has occured for " + dpnGroupModification.getRootPath().toString());
                 if (dpnGroupModification.getRootNode().getModificationType() == ModificationType.DELETE) {
                     removeDpnGroup(dpnGroupModification.getRootNode().getDataBefore());
                 } else {
                     try {
                         loadDpnGroup(dpnGroupModification.getRootNode().getDataAfter());
                    } catch (Exception e) {
                        ErrorLog.logError("DpnChangeManager - Error occured during DPN Create/Write - " + e.getLocalizedMessage(), e.getStackTrace());
                    }
                 }

             }
         }
    }

    @Override
    public void addDpn(Dpns dpn) throws Exception {
    	DpnAPIListener.setUlDpnMapping(dpn.getNodeId()+"/"+dpn.getNetworkId(), dpn.getDpnId());
        LOG.info("DpnAssignmentMgr - Adding DPN from Groups " + dpn.getDpnName() );
        for (FpcDpnGroupId groupId : (dpn.getDpnGroups() == null) ? Collections.<FpcDpnGroupId>emptyList() :
                dpn.getDpnGroups()) {
            Set<String> dpnList = dpnGroupsToDpns.get(groupId.toString());
            if (dpnList == null) {
                dpnList = new HashSet<String>();
                dpnGroupsToDpns.put(groupId.toString(), dpnList);
            }
            dpnList.add(dpn.getDpnId().toString());
        }
        if (globalDpnIdMap.get(dpn.getDpnId().toString()) != null) {
            if (globalDpnIdMap.get(dpn.getDpnId().toString()).compareTo(tenantId.toString()) != 0) {
                // TODO - Determine if this becomes a real problem
                LOG.warn("A DPN ID Conflict has been detected for DPN ID = {} with old Tenant={} and new Tenant={}\n",
                        dpn.getDpnId().toString(), globalDpnIdMap.get(dpn.getDpnId().toString()), tenantId.toString());
            }
        }
        globalDpnIdMap.put(dpn.getDpnId().toString(), tenantId.toString());
    }

    @Override
    public void removeDpn(Dpns dpn) {
        LOG.info("DpnAssignmentMgr - Removing DPN " + dpn.getDpnName() );
        for (FpcDpnGroupId groupId : (dpn.getDpnGroups() == null) ? Collections.<FpcDpnGroupId>emptyList() :
            dpn.getDpnGroups()) {
            Set<String> dpnList = dpnGroupsToDpns.get(groupId.toString());
            if (dpnList != null) {
                dpnList.remove(dpn.getDpnId().toString());
            }
        }
        globalDpnIdMap.remove(dpn.getDpnId().toString());
    }

    @Override
    public void close() {
        super.close();
        groupsDataTreeChangeListenerRegistration.close();
    }

    /**
     * Retrieves a DPN.
     * @param remoteAddress - User Plane Remote Address
     * @param accessType - Technology Access Type
     * @param assignAnyway - Notes that if no viable DPN is found a default one should be assigned.
     * @return a List of Dpns that meets the provided criteria or null otherwise.
     */
    public List<Dpns> getDpn(IpAddress remoteAddress,
            Class<? extends FpcAccessType> accessType,
            boolean assignAnyway) {
        // TODO - If you are going to innovate here is the location to do it.  Add an iterator, balancer, etc.
        // to select the group in a more dynamic / interesting fashion.

        // We assume anchor as the default Role of 'null'
        PeerIpAcessRoleKey byIpAccessRole = new PeerIpAcessRoleKey(accessType,
                null,
                remoteAddress.toString());
        if (remoteIpAddressToDpnGroups.get(byIpAccessRole) != null) {
            String dpnGroup = remoteIpAddressToDpnGroups.get(byIpAccessRole).iterator().next();
            Set<String> dpns = dpnGroupsToDpns.get(dpnGroup);
            if (dpns.isEmpty()) {
                return null;
            } else {
                List<Dpns> retVal = new ArrayList<Dpns>();
                for (String dpnId : dpns) {
                    DpnHolder info = this.tenantMgr.getDpnInfo().get(dpnId);
                    if (info != null) {
                        retVal.add(info.dpn);
                    }
                }
                return retVal;
            }
        }
        PeerIpAcessRoleKey byAccessRole = new PeerIpAcessRoleKey(accessType,
                null,
                null);
        if (remoteIpAddressToDpnGroups.get(byAccessRole) != null) {
            String dpnGroup = remoteIpAddressToDpnGroups.get(byAccessRole).iterator().next();
            Set<String> dpns = dpnGroupsToDpns.get(dpnGroup);
            if (dpns.isEmpty()) {
                return null;
            } else {
                List<Dpns> retVal = new ArrayList<Dpns>();
                for (String dpnId : dpns) {
                    DpnHolder info = this.tenantMgr.getDpnInfo().get(dpnId);
                    if (info != null) {
                        retVal.add(info.dpn);
                    }
                }
                return retVal;
            }
        }
        return null;
    }
}
