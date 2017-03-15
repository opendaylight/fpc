/*
 * Copyright © 2016 Copyright (c) Sprint, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.fpc.assignment;

import java.util.ArrayList;
import java.util.List;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.fpc.dpn.DpnAssignmentMgr;
import org.opendaylight.fpc.dpn.DpnHolder;
import org.opendaylight.fpc.tenant.TenantManager;
import org.opendaylight.fpc.utils.Counter;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.instructions.Instructions;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.payload.Contexts;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.payload.ContextsBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.tenants.tenant.fpc.topology.Dpns;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcbase.rev160803.FpcAccessType;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcbase.rev160803.FpcDirection;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcbase.rev160803.FpcDpnId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcbase.rev160803.IetfPmipAccessType;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcbase.rev160803.ThreeGPPAccessType;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcbase.rev160803.fpc.context.DpnsBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcbase.rev160803.fpc.context.profile.MobilityTunnelParametersBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcbase.rev160803.mobility.info.mobprofile.parameters.ThreegppTunnelBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.dmm.fpc.pmip.rev160119.PmipCommandset;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.threegpp.rev160803.ThreegppCommandset;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.threegpp.rev160803.ThreegppInstr;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Assignment Manager handles activities in the FPC Assignment phase.
 *
 * The Assignment Manager MAY update the Cache with individual objects that includes new data / assignment
 * information.  However, it MUST be done quickly as the FPC Client MAY be waiting on the plug-ins response
 * before completing some signaling work.
 *
 * Any work that can be reasonably delayed to another phase MUST be delayed as there is time sensitive
 * work taking place in this portion of the FPC Agent processing.
 */
// TODO - Feature - Update IPv4 Pools to apply across multiple DPNs
public class AssignmentManager {
    private static final Logger LOG = LoggerFactory.getLogger(AssignmentManager.class);
    static final Class<? extends FpcAccessType> DEFAULT_ACCESS_TECHNOLOGY = ThreeGPPAccessType.class;

    TenantManager tenantMgr;
    DpnAssignmentMgr dpnAssignmentMgr;

    /**
     * Constructor
     * @param tenantMgr - Tenant Manager (and information)
     * @param dataBroker - Data Broker
     * @throws Exception thrown if a DpnAssignmentMgr cannot be initialized
     */
    public AssignmentManager(TenantManager tenantMgr,
            DataBroker dataBroker) throws Exception {
        this.tenantMgr = tenantMgr;
        dpnAssignmentMgr = new DpnAssignmentMgr(tenantMgr, dataBroker);
        if (dpnAssignmentMgr == null)
            throw new Exception("AssignmentManager - FATAL ERROR - DpnAssignmentMgr.getInstance() returned null - Unable to assign DPNs");
    }

    /**
     * Adds an IPv4 Pool to the Assignment Manager
     * @param dpnId - DPN
     * @param poolName - Pool Name
     * @param prefix - IPv4 Prefix
     * @throws Exception if DPN does not exist
     */
    public void addPool(FpcDpnId dpnId, String poolName, IpPrefix prefix) throws Exception {
        DpnHolder dpn = tenantMgr.getDpnInfo().get(dpnId.toString());
        if (dpn != null) {
            dpn.ipv4PoolManagers.put(poolName, IPv4RangeManager.createIPv4RangeManager(prefix));
        } else {
            throw new Exception("AssignmentManager.addPool: Dpn -" + dpnId.toString() + " does not exist.");
        }
    }

    /**
     * Removes an IPv4 Pool from a DPN.
     *
     * This does NOT impact existing sessions.
     *
     * @param dpnId - DPN
     * @param poolName - IPv4 pool name
     */
    public void removePool(FpcDpnId dpnId, String poolName) {
        DpnHolder dpn = tenantMgr.getDpnInfo().get(dpnId.toString());
        if (dpn != null) {
            dpn.ipv4PoolManagers.remove(poolName);
        }
    }

    /**
     * Creates a TEID Range Manager for the specified DPN and IP Address
     * @param dpnId - DPN Identity
     * @param address - IP Address
     * @throws Exception - if the DPN does not exist in the Assignment Manager.
     */
    public void createTeidRange(FpcDpnId dpnId, IpAddress address) throws Exception {
        DpnHolder dpn = tenantMgr.getDpnInfo().get(dpnId.toString());
        if (dpn != null) {
            dpn.teidManagers.put(address.toString(), new Counter());
        } else {
            throw new Exception("AssignmentManager.addPool: Dpn -" + dpnId.toString() + " does not exist.");
        }
    }

    /**
     * Removes a TEID range Manager for an IP Address.
     *
     * @param dpnId - DPN Identity
     * @param address - IP Address corresponding to the TEID range Managers
     */
    public void removeTeidRange(FpcDpnId dpnId, IpAddress address) {
        DpnHolder dpn = tenantMgr.getDpnInfo().get(dpnId.toString());
        if (dpn != null) {
            dpn.teidManagers.remove(address.toString());
        }
    }

    /**
     * Makes all assignments for a Context.
     * @param instr - assignment instructions
     * @param context - Context that requiring assignments
     * @return - A new Context based upon the original and new assignments.  If no assignments were made the
     * original value is returned.
     * @throws Exception - If a specific assignment was requested but insufficient data exists to make the assignment work.
     */
    public Contexts assign(Instructions instr, Contexts context) throws Exception {
        boolean assignmentOccured = false;

        // TODO - support for IetfPmipAccessType then optimize
        if (instr != null) {
            if (instr.getInstrType() != null) {
                if (instr.getInstrType() instanceof ThreegppCommandset) {
                    ThreegppInstr threegInstr =
                            ((ThreegppCommandset) instr.getInstrType()).getInstr3gppMob();
                    ArrayList<org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcbase.rev160803.fpc.context.Dpns>
                    newDpns;
                    ContextsBuilder cb = new ContextsBuilder(context);
                    if (threegInstr.isAssignDpn()) {
                        if (context.getDl() == null) {
                            throw new Exception("AssignmentManager - ERROR - No downlink provided - cannot assign DPN");
                        }

                        // TODO - move to a Group Assignment *then* a DPN list...
                        assignmentOccured = true;
                        List<Dpns> values = dpnAssignmentMgr.getDpn(
                                context.getDl().getTunnelRemoteAddress(),
                                ThreeGPPAccessType.class,
                                true);

                        if (values.size() == 0)
                            return context;

                        newDpns = new ArrayList<org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcbase.rev160803.fpc.context.Dpns>();
                        for (Dpns dpn : values) {
                            DpnsBuilder db = new DpnsBuilder();
                            db.fieldsFrom(dpn);
                            db.setDirection(FpcDirection.Uplink);
                            newDpns.add( db.build() );
                        }
                        cb.setDpns(newDpns);
                    }

                    org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcbase.rev160803.fpc.context.Dpns
                        assignedDpn = null;

                    if (threegInstr.isAssignFteidTeid()) {
                        assignmentOccured = true;
                        boolean foundUlDpn = false;
                        newDpns = new ArrayList<org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcbase.rev160803.fpc.context.Dpns>();
                        for (org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcbase.rev160803.fpc.context.Dpns dpn :
                            cb.getDpns()) {
                            if (dpn.getDirection() == FpcDirection.Uplink) {
                                foundUlDpn = true;
                                DpnHolder ulDpn = tenantMgr.getDpnInfo().get(dpn.getDpnId().toString());
                                if (ulDpn != null) {
                                    Counter teidRange = ulDpn.teidManagers.get(dpn.getTunnelLocalAddress().toString());
                                    if (teidRange != null) {
                                        // TODO - SPEED this up later
                                        MobilityTunnelParametersBuilder mtpb = (dpn.getMobilityTunnelParameters() == null) ?
                                                new MobilityTunnelParametersBuilder() :
                                                new MobilityTunnelParametersBuilder(dpn.getMobilityTunnelParameters());

                                        mtpb.setMobprofileParameters(new ThreegppTunnelBuilder()
                                                .setTunnelIdentifier(new Long(teidRange.nextValue())).build());

                                        DpnsBuilder dpnBldr = new DpnsBuilder(dpn)
                                                .setMobilityTunnelParameters(mtpb.build());
                                        newDpns.add(dpnBldr.build());
                                    } else {
                                        throw new Exception("INTERNAL SYSTEM ERROR - AssignmentManager - TEID Assignment - DPN " +
                                                dpn.getDpnId().toString() + " has no TEID management for local address - " +
                                                dpn.getTunnelLocalAddress().toString());
                                    }
                                } else {
                                    throw new Exception("INTERNAL SYSTEM ERROR - AssignmentManager - TEID Assignment - DPN " +
                                            dpn.getDpnId().toString() + " has been assigned to Context - " +
                                            context.getContextId());
                                }
                            } else {
                                newDpns.add(dpn);
                            }
                        }
                        if (!foundUlDpn) {
                            throw new Exception("INTERNAL SYSTEM ERROR - AssignmentManager - TEID Assignment - No uplink DPN " +
                                    " has been assigned to Context - " +
                                    context.getContextId());
                        }
                        cb.setDpns(newDpns);
                    }
                    if (threegInstr.isAssignIp()) {
                        assignmentOccured = true;
                        boolean foundUlDpn = false;
                        List<IpPrefix> prefixes = new ArrayList<IpPrefix>();
                        newDpns = new ArrayList<org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcbase.rev160803.fpc.context.Dpns>();
                        for (org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcbase.rev160803.fpc.context.Dpns dpn :
                            cb.getDpns()) {
                            // TODO - Support more advanced pool management.
                            if (dpn.getDirection() == FpcDirection.Uplink) {
                                foundUlDpn = true;
                                DpnHolder ulDpn = tenantMgr.getDpnInfo().get(dpn.getDpnId().toString());
                                if (ulDpn != null) {
                                    IPv4RangeManager ipV4RangeManager = ulDpn.ipv4PoolManagers.get(dpn.getTunnelLocalAddress().toString());
                                    if (ipV4RangeManager != null) {
                                        prefixes.add(ipV4RangeManager.getNextAddrAsPrefix());
                                    }
                                } else {
                                    LOG.info("INTERNAL SYSTEM ERROR - AssignmentManager - TEID Assignment - DPN " +
                                            dpn.getDpnId().toString() + " has been assigned to Context - " +
                                            context.getContextId());
                                }
                            }
                        }
                        if (!foundUlDpn) {
                            throw new Exception("INTERNAL SYSTEM ERROR - AssignmentManager - IP Assignment - No uplink DPN " +
                                    " has been assigned to Context - " +
                                    context.getContextId());
                        }
                        if (prefixes.size() == 0) {
                            throw new Exception("INTERNAL SYSTEM ERROR - AssignmentManager - IP Assignment - " +
                                    "Although Uplink DPNs were found, none had pools to assign from");
                        }
                        if (cb.getDelegatingIpPrefixes() != null) {
                            prefixes.addAll(cb.getDelegatingIpPrefixes());
                        }
                        cb.setDelegatingIpPrefixes(prefixes);
                    }
                    if (assignmentOccured) {
                        return cb.build();
                    }
                }
            }
        }

        return context;
    }

    /**
     * Returns the Access (Technology) Type for the provided instructions.
     * @param instr - Instructions
     * @return Access Type or the Default Access Technology Type of the Agent.
     */
    public Class<? extends FpcAccessType> getAccessTechnology(Instructions instr) {
        // Infer by Command Set
        if (instr != null) {
            if (instr.getInstrType() != null) {
                if (instr.getInstrType() instanceof ThreegppCommandset) {
                    return ThreeGPPAccessType.class;
                } else if (instr.getInstrType() instanceof PmipCommandset) {
                    return IetfPmipAccessType.class;
                }
            }
        }

        // Default Assumption
        return DEFAULT_ACCESS_TECHNOLOGY;
    }
}
