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
import org.opendaylight.fpc.utils.ErrorLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class that writes a transaction to a cache
 */
public class WriteToCache implements Runnable {
	private static final Logger LOG = LoggerFactory.getLogger(ZMQSBListener.class);
	public static BlockingQueue<Transaction> blockingQueue;
	private boolean run;
	/**
	 * Adds a transaction to the Blocking queue
	 * @param t - Transaction to be added
	 */
	public static void addToQueue(Transaction t){
		blockingQueue.add(t);
	}

	/**
	 * Constructor
	 */
	public WriteToCache(){
		blockingQueue = new LinkedBlockingQueue<Transaction>();
	}

	/**
	 * Sets the run flag to true
	 */
	public void start(){
		this.run = true;
	}

	/**
	 * Sets the run flag to false
	 */
	public void stop(){
		this.run = false;
	}

	@Override
	public void run() {
		while(run){
			try {
				Transaction t = blockingQueue.take();
				t.writeToCache();
				t.completeAndClose(System.currentTimeMillis());
			} catch (InterruptedException e) {
				ErrorLog.logError(e.getMessage(),e.getStackTrace());
			} catch (Exception e) {
				ErrorLog.logError(e.getMessage(),e.getStackTrace());
			}
		}
	}



}
