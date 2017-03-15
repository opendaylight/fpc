/*
 * Copyright © 2016 Copyright (c) Sprint, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.fpc.utils.zeromq;

import java.util.concurrent.CountDownLatch;

import org.opendaylight.fpc.utils.Worker;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;

/**
 * ZMQ Socket base.
 */
abstract public class ZMQBaseSocket implements Worker {
    protected final ZContext context;
    protected final String address;
    protected final int socketType;
    protected final CountDownLatch startSignal;
    protected boolean run;

    protected ZMQ.Socket socket;

    /**
     * Worker Constructor.
     * 
     * @param context - ZConext
     * @param address - ZMQ Address
     * @param socketType - ZMQ Socket Type
     * @param startSignal - threadpool start signal
     */
    public ZMQBaseSocket(ZContext context, String address, int socketType, CountDownLatch startSignal) {
        this.context = context;
        this.address = address;
        this.socketType = socketType;
        this.run = false;
        this.startSignal = startSignal;
    }

    /**
     * Retrieves the ZMQ Socket associated with the Worker.
     * @return ZMQ.Socket or null otherwise
     */
    public ZMQ.Socket getSocket() {
        return socket;
    }

    @Override
    public boolean isOpen() {
        return socket != null;
    }

    @Override
    public void stop() {
        this.run = false;
    }

    @Override
    public void close() {
        if (socket != null) {
            socket.setLinger(0);
            socket.close();
        }
        socket = null;
    }
}
