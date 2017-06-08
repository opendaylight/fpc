/*
 * Copyright © 2016 - 2017 Copyright (c) Sprint, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.fpc.activation.impl.dpdkdpn;


import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import org.opendaylight.fpc.utils.ErrorLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Deletes a context scheduler
 */
public final class DeleteContextScheduler {

	private static volatile DeleteContextScheduler instance;
	private final ScheduledExecutorService scheduledExecutorService;
	private final int POOL_SIZE = 5;
	private static final Logger LOG = LoggerFactory.getLogger(DeleteContextScheduler.class);

	class DeleteBearerCall implements Callable<DeleteBearerCall>{
	    private final Short dpnTopic;
	    private DpnAPI2 api;
	    private Long s1u_sgw_gtpu_teid;
	    ScheduledFuture<DeleteBearerCall> schedFuture = null;

	    public DeleteBearerCall(DpnAPI2 api, Short dpnTopic2, Long s1u_sgw_gtpu_teid) {
	        this.dpnTopic = dpnTopic2;
	        this.s1u_sgw_gtpu_teid = s1u_sgw_gtpu_teid;
	        this.api = api;
	    }

	    public DeleteBearerCall call() {

	    	api.delete_bearer(dpnTopic, s1u_sgw_gtpu_teid);
	    	DeleteContextScheduler.logNotifification(schedFuture);
			return this;
	    }

	    protected void setFuture(ScheduledFuture<DeleteBearerCall> future) {
            this.schedFuture = (ScheduledFuture<DeleteBearerCall>) future;
        }

	    public void close() {
            schedFuture.cancel(false);
        }
	}

	private DeleteContextScheduler()
	{
		scheduledExecutorService = Executors.newScheduledThreadPool(POOL_SIZE);

	}

	private static void logNotifification(ScheduledFuture<DeleteBearerCall> schedFuture)
	{
		DeleteBearerCall bearerInstance = null;
		try {
			bearerInstance = schedFuture.get();
			LOG.info("Context delete success for  DPNTopic: {}, teid: {}", bearerInstance.dpnTopic, bearerInstance.s1u_sgw_gtpu_teid);
		} catch (InterruptedException | ExecutionException e) {
			ErrorLog.logError(e.getStackTrace());
        }

		/*** North bound cache notification code here**/

	}

	/**
	 * Get an instance of DeleteContextScheduler
	 * @return - instance of DeleteContextScheduler
	 */
	public static DeleteContextScheduler getInstance()
	{
		if (instance == null) {
            synchronized (DeleteContextScheduler.class) {
                if (instance == null) {
                    instance = new DeleteContextScheduler();
                }
            }
        }
        return instance;
	}

	/**
	 * Delete a bearer after specified time
	 * @param api - DPN API object
	 * @param dpnTopic - ZMQ Topic of the DPN
	 * @param s1u_sgw_gtpu_teid - GTPU TEID of the bearer
	 * @param time - Time after which the delete should occur
	 */
	public void delete(DpnAPI2 api, Short dpnTopic, Long s1u_sgw_gtpu_teid, Long time) {

		DeleteBearerCall deleteBearerInstance = new DeleteBearerCall(api, dpnTopic, s1u_sgw_gtpu_teid);
		ScheduledFuture<DeleteBearerCall> futureValue = (ScheduledFuture<DeleteBearerCall>) scheduledExecutorService.schedule(deleteBearerInstance, time, TimeUnit.SECONDS);
		deleteBearerInstance.setFuture(futureValue);

	}


}