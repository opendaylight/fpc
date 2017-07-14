/*
 * Copyright © 2016 Copyright (c) Sprint, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.fpc.impl;

import java.util.Iterator;
import java.util.Map.Entry;

import org.opendaylight.fpc.activation.cache.transaction.Transaction;
import org.opendaylight.fpc.dpn.DpnHolder;
import org.opendaylight.fpc.tenant.TenantManager;
import org.opendaylight.fpc.utils.ErrorLog;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.DpnOperation;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.OpInput;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.payload.Contexts;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.tenants.tenant.fpc.topology.Dpns;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SessionThread implements Runnable {
	private static final Logger LOG = LoggerFactory.getLogger(FpcagentDispatcher.class);
	private Dpns dpn, vdpn;
	private DpnOperation op;
    private static final String targetPrefix = "/ietf-dmm-fpcagent:tenants/tenant/default/fpc-mobility/contexts/";
	private Boolean deleteFlag;
	public SessionThread(Dpns vdpn, Dpns dpn, DpnOperation op, Boolean deleteFlag){
		this.vdpn = vdpn;
		this.dpn = dpn;
		this.op = op;
		this.deleteFlag = deleteFlag;
	}
	
	@Override
	public void run() {
		LOG.info("in thread!");
		DpnHolder dpnInfo;
		Contexts context;
		Iterator<Entry<Contexts, Transaction>> it = TenantManager.vdpnContextsMap.get(vdpn.getDpnId()).entrySet().iterator();
		while(it.hasNext()){
			Entry<Contexts, Transaction> cMap = it.next();
			Transaction tx = cMap.getValue();
			OpInput input = tx.getOpInput();
    		context = cMap.getKey();
			dpnInfo = tx.getTenantContext().getDpnInfo().get(dpn.getDpnId().toString());
			
			if (dpnInfo.activator != null) {
                try {
                	if(op == DpnOperation.Add){
                		LOG.info("thread : I am copying sessions");
                		dpnInfo.activator.activate(input.getOpType(), (context.getInstructions() != null) ?
                                context.getInstructions() : input.getInstructions(), context, tx.getReadCache());
                	}else if(op == DpnOperation.Remove){
                		LOG.info("thread : I am deleting sessions");
                		dpnInfo.activator.delete(input.getInstructions(), null, context);
                		if(deleteFlag){
                			tx.getTenantContext().getSc().remove(targetPrefix+context.getContextId().getString());
                			TenantManager.vdpnContextsMap.get(vdpn.getDpnId()).remove(context);
                			it = TenantManager.vdpnContextsMap.get(vdpn.getDpnId()).entrySet().iterator();
                		}
                	}
                } catch (Exception e) {
                    ErrorLog.logError(e.getStackTrace());
                }
            } else {
                LOG.info("No activator found for DPN" + dpn.getDpnId().toString());
            }
		}
		//for(Entry<Contexts, Transaction> cMap : TenantManager.vdpnContextsMap.get(vdpn.getDpnId()).entrySet()){
			
		//}
		LOG.info("ContextsMap in SessionThread: "+TenantManager.vdpnContextsMap);
		
	}
	
}
