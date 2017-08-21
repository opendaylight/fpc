/*
 * Copyright © 2016 - 2017 Copyright (c) Sprint, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.fpc.impl.zeromq;

import java.nio.ByteBuffer;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import org.opendaylight.fpc.activation.impl.dpdkdpn.DpnAPI2;
import org.opendaylight.fpc.activation.impl.dpdkdpn.DpnAPIListener;
import org.opendaylight.fpc.dpn.DPNStatusIndication;
import org.opendaylight.fpc.monitor.EventMonitorMgr;
import org.opendaylight.fpc.utils.ErrorLog;
import org.opendaylight.fpc.utils.Worker;
import org.opendaylight.fpc.utils.zeromq.ZMQClientPool;
import org.opendaylight.fpc.utils.zeromq.ZMQClientSocket;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.notify.value.DownlinkDataNotification;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcbase.rev160803.FpcDpnId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implements a Worker to process replies from DPN
 */
public class ZMQSBMessageWorker implements Worker {
	protected static final Logger LOG = LoggerFactory.getLogger(ZMQSBMessageWorker.class);
	private BlockingQueue<byte[]> blockingQueue;
	private boolean run;
	protected final CountDownLatch startSignal;
	private DpnAPIListener dpnApi;
	private final String nodeId;
    private final String networkId;
    private ZMQClientSocket sock;
    private static byte HELLO_REPLY = 0b0000_1101;
	/**
	 * ZMQSBMessageWorker Constructor
	 * @param startSignal - Start Signal
	 * @param blockingQueue - Blocking Queue
	 * @param nodeId - Controller Node Id
	 * @param networkId - Controller Network Id
	 */
	public ZMQSBMessageWorker(CountDownLatch startSignal, BlockingQueue<byte[]> blockingQueue, String nodeId, String networkId){
		this.sock = ZMQClientPool.getInstance().getWorker();
		this.blockingQueue = blockingQueue;
		this.startSignal = startSignal;
		this.dpnApi = new DpnAPIListener();
		this.nodeId = nodeId;
	    this.networkId = networkId;
	}

	 /**
	  * Returns the blocking queue of the worker
	 * @return Blocking Queue
	 */
	public BlockingQueue<byte[]> getBlockingQueue() {
        return blockingQueue;
    }

	 /**
	  * Sends a reply to a DPN Hello
	 * @param dpnStatus - DPN Status Indication message received from the DPN
	 */
	protected void sendHelloReply(DPNStatusIndication dpnStatus){
	    	if(DpnAPIListener.getTopicFromNode(dpnStatus.getKey()) != null){
		    	ByteBuffer bb = ByteBuffer.allocate(9+nodeId.length()+networkId.length());
		        bb.put(DpnAPI2.toUint8(DpnAPIListener.getTopicFromNode(dpnStatus.getKey())))
		            .put(HELLO_REPLY)
		            .put(DpnAPI2.toUint8(ZMQSBListener.getControllerTopic()))
		            .put(DpnAPI2.toUint32(ZMQSBListener.getControllerSourceId()))
		            .put(DpnAPI2.toUint8((short)nodeId.length()))
		            .put(nodeId.getBytes())
		            .put(DpnAPI2.toUint8((short)networkId.length()))
		            .put(networkId.getBytes());

		        try {
					sock.getBlockingQueue().put(bb);
				} catch (InterruptedException e) {
					ErrorLog.logError(e.getStackTrace());
				}
	    	}
	    }



	@Override
	public void run() {
		this.run = true;
		try {
            while(run) {
                byte[] contents = blockingQueue.take();
				Map.Entry<FpcDpnId, Object> entry = dpnApi.decode(contents);
    		 	if(entry!=null){
    		 		if (entry.getValue() instanceof DownlinkDataNotification) {
    		 			EventMonitorMgr.processEvent(entry.getKey(),(DownlinkDataNotification)entry.getValue());
    		 		} else if (entry.getValue()  instanceof DPNStatusIndication) {
    		 			DPNStatusIndication dpnStatus = (DPNStatusIndication)entry.getValue();
    		 			EventMonitorMgr.processEvent(entry.getKey(),dpnStatus);
    		 			if(dpnStatus.getStatus() == DPNStatusIndication.Status.HELLO){
    		 	    		sendHelloReply(dpnStatus);
    		 	    	}
    		 		}
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
	}

	@Override
	public void stop() {
	}

	@Override
	public void open() {
	}

	@Override
	public boolean isOpen() {
		return false;
	}

}
