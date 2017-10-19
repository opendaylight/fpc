/*
 * Copyright © 2016 Copyright (c) Sprint, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.fpc.impl;

import java.util.Collections;
import java.util.Iterator;
import java.util.Map.Entry;

import org.opendaylight.fpc.activation.cache.transaction.ContextInfoHolder;
import org.opendaylight.fpc.activation.cache.transaction.Transaction;
import org.opendaylight.fpc.activation.cache.transaction.Transaction.OperationStatus;
import org.opendaylight.fpc.activation.impl.dpdkdpn.DpnAPI2;
import org.opendaylight.fpc.dpn.DpnHolder;
import org.opendaylight.fpc.tenant.TenantManager;
import org.opendaylight.fpc.utils.ErrorLog;
import org.opendaylight.fpc.utils.zeromq.ZMQClientPool;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.DpnOperation;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.OpInput;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.op.input.op_body.DeleteOrQuery;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.payload.Contexts;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.tenants.tenant.fpc.topology.Dpns;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcbase.rev160803.targets.value.Targets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
/**
 * When adding a DPN to a VDPN, copy sessions to new DPN
 * When removing a DPN from a VDPN, delete sessions from that DPN (When removing the last DPN in a VDPN, delete everything)
 */
public class SessionThread implements Runnable {
	private static final Logger LOG = LoggerFactory.getLogger(FpcagentDispatcher.class);
	private Dpns dpn, vdpn;
	private DpnOperation op;
    private static final String targetPrefix = "/ietf-dmm-fpcagent:tenants/tenant/default/fpc-mobility/contexts/";
	private Boolean deleteFlag;
	private DpnAPI2 api;
	/**
	 * Constructor
	 * @param vdpn - Virtual DPN
	 * @param dpn - Actual DPN
	 * @param op - Operation Add / Remove
	 * @param deleteFlag - Set to True if context has to be deleted from in-memory cache
	 */
	public SessionThread(Dpns vdpn, Dpns dpn, DpnOperation op, Boolean deleteFlag){
		this.vdpn = vdpn;
		this.dpn = dpn;
		this.op = op;
		this.deleteFlag = deleteFlag;
		api = new DpnAPI2(ZMQClientPool.getInstance().getWorker());
	}

	@Override
	public void run() {
		DpnHolder dpnInfo;
		Contexts context;
		Iterator<Entry<Contexts, ContextInfoHolder>> it = TenantManager.vdpnContextsMap.get(vdpn.getDpnId()).entrySet().iterator();
		while(it.hasNext()){
			Entry<Contexts, ContextInfoHolder> cMap = it.next();
			ContextInfoHolder contextInfoHolder = cMap.getValue();
			OpInput input = contextInfoHolder.opInput;
    		context = cMap.getKey();
			dpnInfo = contextInfoHolder.tenantManager.getDpnInfo().get(dpn.getDpnId().toString());

			if (dpnInfo != null && dpnInfo.activator != null) {
                try {
                	if(op == DpnOperation.Add){
                		LOG.info("Copying existing sessions to newly added DPN");
                		dpnInfo.activator.activate(api, input.getClientId(), input.getOpId(), input.getOpType(), (context.getInstructions() != null) ?
                                context.getInstructions() : input.getInstructions(), context, contextInfoHolder.payloadCache);
                	}else if(op == DpnOperation.Remove){
                		LOG.info("Deleting existing sessions from removed DPN");
	                	if(deleteFlag){
	                		dpnInfo.activator.delete(api, input.getClientId(), input.getOpId(), input.getInstructions(), null, context);
	                		contextInfoHolder.tenantManager.getSc().remove(targetPrefix+context.getContextId().getString());
	                		TenantManager.vdpnContextsMap.get(vdpn.getDpnId()).remove(context);
	               			it = TenantManager.vdpnContextsMap.get(vdpn.getDpnId()).entrySet().iterator();
	               		}else{
	                		dpnInfo.activator.delete(api, input.getClientId(), input.getOpId(), input.getInstructions(), null, context);
	                	}
                	}
                } catch (Exception e) {
                    ErrorLog.logError(e.getStackTrace());
                }
            } else {
                LOG.info("No activator found for DPN" + dpn.getDpnId().toString());
            }
		}
	}

}
