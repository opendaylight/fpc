/*
 * Copyright © 2016 - 2017 Copyright (c) Sprint, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.fpc.utils.zeromq;

import java.util.concurrent.CountDownLatch;

import org.zeromq.ZContext;

/**
 * ZMQ Server Socket base.
 */
abstract public class ZMQServerSocket extends ZMQBaseSocket {

    /**
     * Server Socket Constructor.
     * 
     * @param context - ZConext
     * @param address - ZMQ Address
     * @param socketType - ZMQ Socket Type
     * @param startSignal - threadpool start signal
     */
    public ZMQServerSocket(ZContext context, String address, int socketType,  CountDownLatch startSignal) {
        super(context,address,socketType,startSignal);
    }

    @Override
    public void open() {
        socket = context.createSocket(socketType); // Should be ZMQ.REP
        socket.setLinger(0);
        socket.setReceiveTimeOut(1000);
        socket.bind(address);
    }

    @Override
    public void close() {
        socket = null;
    }
}
