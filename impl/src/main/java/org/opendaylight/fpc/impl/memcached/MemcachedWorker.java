/*
 * Copyright © 2016 - 2017 Copyright (c) Sprint, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.fpc.impl.memcached;

import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;

import org.opendaylight.fpc.dpn.DPNStatusIndication;
import org.opendaylight.fpc.impl.FpcServiceImpl;
import org.opendaylight.fpc.monitor.EventMonitorMgr;
import org.opendaylight.fpc.utils.ErrorLog;
import org.opendaylight.fpc.utils.Worker;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.OpHeader.OpType;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.OpInput;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.notify.value.DownlinkDataNotification;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcbase.rev160803.FpcContext;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcbase.rev160803.FpcContextId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcbase.rev160803.FpcDpnId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcbase.rev160803.FpcIdentity;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpPrefix;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.spy.memcached.AddrUtil;
import net.spy.memcached.MemcachedClient;

public class MemcachedWorker implements Worker {
	private static final Logger LOG = LoggerFactory.getLogger(FpcServiceImpl.class);
	private BlockingQueue<Map.Entry<FpcContext,OpType>> blockingQueue;
	private static Map<FpcContextId,Long> createTimestamp = new ConcurrentHashMap<FpcContextId,Long>();
	private boolean run;
	protected final CountDownLatch startSignal;
	protected String memcachedUri;
	protected MemcachedClient  mcc;
	public MemcachedWorker(CountDownLatch startSignal, LinkedBlockingQueue<Map.Entry<FpcContext, OpType>> linkedBlockingQueue, String memcachedUri){
		this.startSignal = startSignal;
		this.blockingQueue = linkedBlockingQueue;
		this.memcachedUri = memcachedUri;
	}

	/**
	  * Returns the blocking queue of the worker
	 * @return Blocking Queue
	 */
	public BlockingQueue<Map.Entry<FpcContext,OpType>> getBlockingQueue() {
       return blockingQueue;
   }

	@Override
	public void run() {
		this.run = true;
		try {
			System.setProperty("net.spy.log.LoggerImpl","java.util.logging.Logger");
			mcc =  new MemcachedClient(AddrUtil.getAddresses(memcachedUri));
            while(run) {
            	Map.Entry<FpcContext,OpType> context = blockingQueue.take();
            	long timestamp = System.currentTimeMillis();
            	FpcContext ctxt = context.getKey();
            		IpPrefix assignedPrefix = (ctxt.getDelegatingIpPrefixes() == null) ? null
        					: (ctxt.getDelegatingIpPrefixes().isEmpty()) ? null : ctxt.getDelegatingIpPrefixes().get(0);

        			String entry = assignedPrefix.getIpv4Prefix().getValue().split("/")[0] +","+
        					String.valueOf(ctxt.getImsi().getValue().longValue()) +","+String.valueOf(ctxt.getImsi().getValue().longValue()) +"," +
        					String.valueOf( (ctxt.getContextId().getInt64() != null) ? ctxt.getContextId().getInt64() :
        						( (ctxt.getContextId().getString() != null) ? ctxt.getContextId().getString() : ctxt.getContextId().getInstanceIdentifier().toString())
        							);
            		if(context.getValue().equals(OpType.Create)){
            			createTimestamp.put(ctxt.getContextId(), timestamp);
            			entry = entry + "," + String.valueOf(timestamp) +",";
            			mcc.set(assignedPrefix.getIpv4Prefix().getValue().split("/")[0], 0, entry);
            		} else if(context.getValue().equals(OpType.Delete)){
            			entry = entry + createTimestamp.get(ctxt.getContextId()) +"," + String.valueOf(timestamp);
            			mcc.replace(assignedPrefix.getIpv4Prefix().getValue().split("/")[0], 0, entry);
            			createTimestamp.remove(ctxt.getContextId());
                    }

            }
        } catch (InterruptedException e) {
        	ErrorLog.logError(e.getStackTrace());
        } catch (Exception e) {
        	ErrorLog.logError(e.getMessage(),e.getStackTrace());
        }
	}

	@Override
	public void close() throws Exception {
		this.run = false;
		mcc.shutdown();
	}

	@Override
	public void stop() {
		// TODO Auto-generated method stub

	}

	@Override
	public void open() {
		// TODO Auto-generated method stub

	}

	@Override
	public boolean isOpen() {
		// TODO Auto-generated method stub
		return false;
	}

}
