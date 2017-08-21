/*
 * Copyright © 2016 - 2017 Copyright (c) Sprint, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.fpc.impl.zeromq;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.concurrent.ThreadLocalRandom;

import org.opendaylight.fpc.activation.impl.dpdkdpn.DpnAPI2;
import org.opendaylight.fpc.activation.impl.dpdkdpn.DpnAPIListener;
import org.opendaylight.fpc.dpn.DPNStatusIndication;
import org.opendaylight.fpc.utils.ErrorLog;
import org.opendaylight.fpc.utils.zeromq.ZMQClientPool;
import org.opendaylight.fpc.utils.zeromq.ZMQClientSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;
import org.zeromq.ZMQ.Socket;

/**
 * ZMQ Northbound Interface Listener - untested.
 */
public class ZMQSBListener implements AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(ZMQSBListener.class);
    private ZMQClientSocket sock;
    private ZMQSBWorker broadcastAllSub;
    private ZMQSBWorker broadcastControllersSub;
    private ZMQSBWorker broadcastDpnsSub;
    private ZMQSBWorker subscriber;
    private final String address;
    private static Short subscriberId;
    private final Short broadcastAllId;
    private final Short broadcastControllersId;
    private final Short broadcastDpnsId;
    private static Long controllerSourceId;
    private Thread broadcastAllWorker;
    private Thread broadcastControllersWorker;
    private Thread generalWorker;
    private Thread broadcastTopicWorker;
    private final String nodeId;
    private final String networkId;
    private boolean conflictingTopic;

    private static int MIN_TOPIC_VAL = 4;
    private static int MAX_TOPIC_VAL = 255;

    private static byte ASSIGN_ID = 0b0000_1010;
    private static byte ASSIGN_CONFLICT = 0b0000_1011;
    private static byte HELLO_REPLY = 0b0000_1101;

    private static byte CONTROLLER_STATUS_INDICATION = 0b0000_1110;
    private static byte HELLO = 0b0000_0001;
    private static byte GOODBYE = 0b0000_0010;
    protected ZMQSBMessageWorker zmqSBMessageWorker;
    /**
     * Constructor.
     * @param address - Address
     * @param broadcastAllId - ZMQ Topic Id to broadcast to all subscribers
     * @param broadcastControllersId - ZMQ Topic Id to broadcast to all controllers
     * @param broadcastDpnsId - ZMQ Topic Id to broadcast to all DPNs
     * @param nodeId - Controller's node Id
     * @param networkId - Controller's network Id
     */
    public ZMQSBListener(String address, String broadcastAllId, String broadcastControllersId, String broadcastDpnsId, String nodeId, String networkId) {
    	sock = ZMQClientPool.getInstance().getWorker();
        this.address = address;
        this.broadcastAllId = Short.parseShort(broadcastAllId);
        this.broadcastControllersId = Short.parseShort(broadcastControllersId);
        this.broadcastDpnsId = Short.parseShort(broadcastDpnsId);
        subscriberId = null;
        this.broadcastAllWorker = null;
        this.broadcastControllersWorker = null;
        this.generalWorker = null;
        this.broadcastTopicWorker = null;
        this.conflictingTopic = false;
        this.controllerSourceId = (long) ThreadLocalRandom.current().nextInt(0,65535);
        this.nodeId = nodeId;
        this.networkId = networkId;
        zmqSBMessageWorker = ZMQSBMessagePool.getInstance().getWorker();
    }

    /**
     * Gets the ZMQ Topic to which the controller subscribes to
     * @return - ZMQ Topic
     */
    public static Short getControllerTopic(){
    	return subscriberId;
    }

    /**
     * Gets the Source Id of the controller
     * @return - ZMQ Topic
     */
    public static Long getControllerSourceId(){
    	return controllerSourceId;
    }

    @Override
    public void close() throws Exception {
        stop();
    }

    /**
     * Stops the Listener.
     */
    public void stop() {
    	sendGoodbyeToDpns();
    	if(sock != null){
    		sock.close();
    	}
        if (subscriber != null) {
            subscriber.stop();
        }
        if (broadcastControllersSub != null) {
        	broadcastControllersSub.stop();
        }
        if (broadcastDpnsSub != null) {
        	broadcastDpnsSub.stop();
        }
        if (broadcastAllSub != null) {
        	broadcastAllSub.stop();
        }
    }

    /**
     * Opens the Listener resources.
     */
    public void open() {
        try {
        	broadcastAllSub = new ZMQSBWorker(address, broadcastAllId);
            broadcastAllWorker = new Thread(broadcastAllSub);
            broadcastAllWorker.start();
            broadcastControllersSub = new ZMQSBWorker(address, broadcastControllersId);
            broadcastControllersWorker = new Thread(broadcastControllersSub);
            broadcastControllersWorker.start();
            //broadcastDpnsSub = new ZMQSBWorker(address, broadcastDpnsId);
            //broadcastDpnsWorker = new Thread(broadcastDpnsSub);
            //broadcastDpnsWorker.start();
            subscriberId = (short) ThreadLocalRandom.current().nextInt(MIN_TOPIC_VAL,MAX_TOPIC_VAL+1);
            broadcastTopicWorker = new Thread(new BroadcastTopic(subscriberId));
            broadcastTopicWorker.start();
        } catch (Exception e) {
            ErrorLog.logError(e.getStackTrace());
        }
    }

    /**
     * Interrupts the BroadcastTopicworker if there is an Assign topic Conflict
     * @param conflict - Flag to indicate conflict
     * @param subId - Topic Id that caused the conflict
     */
    protected void BroadcastAllSubIdCallBack(boolean conflict, Short subId){
    	if(conflict && subscriberId.equals(subId)){
    		this.conflictingTopic = true;
    		this.broadcastTopicWorker.interrupt();
    	}
    }

    /**
     * Broadcasts the HELLO message to all the DPNs
     */
    public void sendHelloToDpns(){
    	ByteBuffer bb = ByteBuffer.allocate(10+nodeId.length()+networkId.length());
        bb.put(DpnAPI2.toUint8(broadcastDpnsId))
            .put(CONTROLLER_STATUS_INDICATION)
            .put(DpnAPI2.toUint8(subscriberId))
            .put(HELLO)
            .put(DpnAPI2.toUint32(controllerSourceId))
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

    /**
     * Broadcasts the GOODBYE message to all the DPNs
     */
    public void sendGoodbyeToDpns(){
    	ByteBuffer bb = ByteBuffer.allocate(10+nodeId.length()+networkId.length());
        bb.put(DpnAPI2.toUint8(broadcastDpnsId))
            .put(CONTROLLER_STATUS_INDICATION)
            .put(DpnAPI2.toUint8(subscriberId))
            .put(GOODBYE)
            .put(DpnAPI2.toUint32(controllerSourceId))
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

    /**
     * Broadcasts an Assign Conflict message
     * @param contents - byte array received over the southbound.
     */
    protected void SendAssignConflictMessage(byte[] contents){
    	byte topic = contents[2];
    	short nodeIdLen = contents[7];
        short networkIdLen = contents[8+nodeIdLen];
    	String node_id = new String(Arrays.copyOfRange(contents, 8, 8+nodeIdLen));
    	String network_id = new String(Arrays.copyOfRange(contents, 9+nodeIdLen, 9+nodeIdLen+networkIdLen));

    	if(DpnAPI2.toUint8(subscriberId) == topic || nodeId.equals(node_id) || networkId.equals(network_id)){
    		ByteBuffer bb = ByteBuffer.allocate(9+nodeId.length()+networkId.length());
	        bb.put(DpnAPI2.toUint8(broadcastAllId))
	            .put(ASSIGN_CONFLICT)
	            .put(topic)
	            .put(DpnAPI2.toUint32(controllerSourceId))
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

    /**
     * Class to broadcast a topic for the controller
     */
    protected class BroadcastTopic implements Runnable {
    	private Short topic;

    	/**
    	 * Constructor
    	 * @param topic - Topic to broadcast
    	 */
    	public BroadcastTopic(Short topic){
    		this.topic = topic;
    	}

    	/**
    	 * Broadcasts the topic
    	 * @param topic - Topic to broadcast
    	 */
    	private void broadcastTopic(Short topic){
    		ByteBuffer bb = ByteBuffer.allocate(9+nodeId.length()+networkId.length());
	        bb.put(DpnAPI2.toUint8(broadcastAllId))
	            .put(ASSIGN_ID)
	            .put(DpnAPI2.toUint8(this.topic))
	            .put(DpnAPI2.toUint32(controllerSourceId))
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
		@Override
		public void run() {
	        try {
	            this.broadcastTopic(this.topic);
	            LOG.info("Thread sleeping: "+Thread.currentThread().getName());
	            Thread.sleep(10000);
	        } catch (InterruptedException e) {
	        	if(conflictingTopic){
	        		conflictingTopic = false;
	        		this.topic = (short) ThreadLocalRandom.current().nextInt(MIN_TOPIC_VAL,MAX_TOPIC_VAL+1);
	        		subscriberId = this.topic;
	        		this.run();
	        		return;
	        	}
	        	else{
	        		ErrorLog.logError(e.getStackTrace());
	        	}
	        }
	        subscriberId = this.topic;
	        LOG.info("Topic Id: "+subscriberId.toString());
	        subscriber = new ZMQSBWorker(address, subscriberId);
	        generalWorker = new Thread(subscriber);
	        generalWorker.start();
	        try {
				Thread.sleep(2000);
			} catch (InterruptedException e) {
        		ErrorLog.logError(e.getStackTrace());
			}
	        sendHelloToDpns();
		}


    }

	/**
     * Worker Class.
     */
    protected class ZMQSBWorker implements Runnable {
        private final ZContext ctx;
        private final String address;
        private final Short subscriberId;
        private final DpnAPIListener dpnApi;
        private boolean run;

        /**
         * Constructor.
         * @param address - Address
         * @param subscriberId - ZMQ Subscription Id
         */
        public ZMQSBWorker(String address,
                Short subscriberId) {
            ctx = new ZContext();
            run = true;
            this.address = address;
            this.subscriberId = subscriberId;
            dpnApi = new DpnAPIListener();
        }

        @Override
        public void run() {
            Socket subscriber = ctx.createSocket(ZMQ.SUB);
            subscriber.connect(address);
            subscriber.subscribe(new byte[] {DpnAPI2.toUint8(subscriberId)});
            while ((!Thread.currentThread ().isInterrupted ()) &&
                    run) {
                byte[] contents = subscriber.recv();
                byte topic = contents[0];
                byte messageType = contents[1];
                switch(topic){
                	case 1:
                			if(messageType==ASSIGN_CONFLICT && dpnApi.toInt(contents, 3) != controllerSourceId){
                				BroadcastAllSubIdCallBack(true,(short) contents[2]);
                			}
                			else if(messageType==ASSIGN_ID && dpnApi.toInt(contents, 3) != controllerSourceId){
                				SendAssignConflictMessage(contents);
                			}
                			break;
                	default:
							try {
								ZMQSBMessagePool.getInstance().getWorker().getBlockingQueue().put(contents);
							} catch (InterruptedException e) {
								e.printStackTrace();
							}
                }
            }
            subscriber.disconnect(address);
            subscriber.close();
            ctx.destroySocket(subscriber);
            try {
                Thread.currentThread().join();
            } catch (InterruptedException e) {
            	ErrorLog.logError(e.getStackTrace());
            }
        }

        /**
         * Stops the Worker
         */
        public void stop() {
            run = false;
        }
    }
}
