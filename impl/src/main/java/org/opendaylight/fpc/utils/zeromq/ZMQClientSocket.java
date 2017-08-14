/*
 * Copyright © 2016 - 2017 Copyright (c) Sprint, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.fpc.utils.zeromq;

import java.nio.ByteBuffer;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;

import org.opendaylight.fpc.utils.ErrorLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeromq.ZContext;

/**
 * ZMQ Client Socket.
 */
public class ZMQClientSocket extends ZMQBaseSocket {
    private static final Logger LOG = LoggerFactory.getLogger(ZMQClientSocket.class);
    private BlockingQueue<ByteBuffer> blockingQueue;

    /**
     * Client Constructor.
     *
     * @param context - ZConext
     * @param address - ZMQ Address
     * @param socketType - ZMQ Socket Type
     * @param startSignal - threadpool start signal
     * @param blockingQueue - A byte buffer BlockingQueue assigned to the Client
     */
    public ZMQClientSocket(ZContext context, String address, int socketType,
            CountDownLatch startSignal, BlockingQueue<ByteBuffer> blockingQueue) {
        super(context,address,socketType, startSignal);
        this.blockingQueue = blockingQueue;
    }

    /**
     * Retrieves the Worker's Queue.
     * @return A BlockingQueue that accepts a byte buffer
     */
    public BlockingQueue<ByteBuffer> getBlockingQueue() {
        return blockingQueue;
    }

    @Override
    public void open() {
        socket = context.createSocket(socketType);
        socket.connect(address);
    }

    @Override
	public void close() {
    	this.run = false;
    	socket.disconnect(address);
    	socket.close();
    	context.destroySocket(socket);
		super.close();
	}

	@Override
    public void run() {
        this.run = true;
        LOG.info("ZMQClientSocket RUN started - awaiting start signal");
        try {
            startSignal.await();
            LOG.info("ZMQClientSocket start signal received");
            while(run) {
                ByteBuffer bb = blockingQueue.take();
                socket.send(bb.array());
            }
        } catch (InterruptedException e) {
        	ErrorLog.logError(e.getStackTrace());
        }
    }
}
