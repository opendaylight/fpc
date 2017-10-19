/*
 * Copyright © 2016 Copyright (c) Sprint, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.fpc.policy;

import java.util.Map;
import java.util.Set;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.fpc.activation.impl.dpdkdpn.DpnAPIListener;
import org.opendaylight.fpc.dpn.DpnHolder;
import org.opendaylight.fpc.tenant.TenantManager;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.tenants.tenant.fpc.mobility.Contexts;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.tenants.tenant.fpc.mobility.PortsBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.tenants.tenant.fpc.mobility.PortsKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcbase.rev160803.FpcDpnId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcbase.rev160803.FpcIdentity;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcbase.rev160803.FpcPolicyGroupId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcbase.rev160803.FpcPortId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcbase.rev160803.fpc.context.Dpns;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manager for changes to virtual ports
 */
public class BasePortManager extends PortManager implements AutoCloseable{
	private static final Logger LOG = LoggerFactory.getLogger(BasePolicyManager.class);

	private final TenantManager tenantMgr;
	private final DataBroker dataBroker;
    private boolean initialized;
	 /**
     * Primary Constructor.
     * @param tenantMgr - Tenant Manager Assigned to the Activation Manager.
     * @param dataBroker - Data Broker
     */
    public BasePortManager(TenantManager tenantMgr,
            DataBroker dataBroker) {
        super();
        initialized = false;
        this.tenantMgr = tenantMgr;
        this.dataBroker = dataBroker;
        init(dataBroker, tenantMgr.getTenant());
        registerListeners();
    }
    /**
     * Invoked during a context change
     * Links DPN with Port to prep for ADC rule application
     * @param cntx - Context where change occurs
     */
    @Override
    public void addContext(Contexts cntx) throws Exception {
    	LOG.info("Connecting Port and DPN...");
    	if(cntx.getPorts()!=null && cntx.getDpns()!=null){
    		LOG.info("Contextid: "+cntx.getContextId());
    		LOG.info("cntx.getPorts(): "+cntx.getPorts());
    		FpcPortId portId = cntx.getPorts().get(0);
    		List<FpcPolicyGroupId> polgrpIdList = new ArrayList<FpcPolicyGroupId>();
    		Map<FpcIdentity, Set<FpcIdentity>> revptr = BasePolicyManager.getRevPointer();
    		for(Iterator<FpcIdentity> it = revptr.keySet().iterator(); it.hasNext();){
    			FpcIdentity ident = it.next();
    			if(ident instanceof FpcPolicyGroupId && revptr.get(ident).contains(portId)){
    				polgrpIdList.add((FpcPolicyGroupId) ident);
    			}
    		}
    		org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.tenants.tenant.fpc.mobility.Ports port = new PortsBuilder().setPortId(portId)
    				.setKey(new PortsKey(portId))
    				.setPolicyGroups(polgrpIdList).build();
    		Dpns dpn = cntx.getDpns().get(0);
    		Boolean isVdpn = false;
    		for(FpcDpnId vdpnId : TenantManager.vdpnDpnsMap.keySet()){
    			if(dpn.getDpnId().getString().equals(vdpnId.getString())){
    				isVdpn = true;
    				for(FpcDpnId vdpnDpnId: TenantManager.vdpnDpnsMap.get(dpn.getDpnId())){
    					//if need whole DPN
//    					tenantMgr.getDpnInfo().get(vdpnDpnId.toString()).activator.send_ADC_rules(FpcagentDispatcher.getDpnById(vdpnDpnId), port);
    					tenantMgr.getDpnInfo().get(vdpnDpnId.toString()).activator.send_ADC_rules(DpnAPIListener.getTopicFromDpnId(vdpnDpnId), port);
    				}
    			}
    		}
    		if(!isVdpn){
    			DpnHolder dpnHolder = tenantMgr.getDpnInfo().get(dpn.getDpnId().toString());
    			//if need whole DPN
//    			org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.tenants.tenant.fpc.topology.Dpns topologyDpn =
//    					new DpnsBuilder()
//    					.setDpnId(new FpcDpnId(dpn.getDpnId()))
//    					.setAbstract(false)
//    					.build();
//    			dpnHolder.activator.send_ADC_rules(topologyDpn, port);
    			dpnHolder.activator.send_ADC_rules(DpnAPIListener.getTopicFromDpnId(dpn.getDpnId()), port);
    		}
    	}else{
    		LOG.warn("Port/DPN mapping couldn't be established, make sure context has both a port and a DPN");
    	}
	}

	@Override
	public void removeContext(Contexts cntx) {
		// TODO Auto-generated method stub

	}

}
