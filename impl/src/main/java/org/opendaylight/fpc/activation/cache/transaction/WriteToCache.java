/*
 * Copyright © 2016 - 2017 Copyright (c) Sprint, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.fpc.activation.cache.transaction;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.opendaylight.fpc.impl.zeromq.ZMQSBListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WriteToCache implements Runnable {
	private static final Logger LOG = LoggerFactory.getLogger(ZMQSBListener.class);
	public static BlockingQueue<Transaction> blockingQueue;

	public static void addToQueue(Transaction t){
		blockingQueue.add(t);
	}

	public WriteToCache(){
		blockingQueue = new LinkedBlockingQueue<Transaction>();
	}

	@Override
	public void run() {
		try {
			LOG.info("Starting Write To Cache Thread");
			Transaction t = blockingQueue.take();
			t.writeToCache();
			t.completeAndClose(System.currentTimeMillis());
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}


	}



}
