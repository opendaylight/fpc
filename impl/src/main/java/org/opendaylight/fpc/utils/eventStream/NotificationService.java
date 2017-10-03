/*
 * Copyright © 2016 - 2017 Copyright (c) Sprint, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.fpc.utils.eventStream;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

import javax.servlet.AsyncContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.ServletOutputStream;

import org.eclipse.jetty.io.EofException;
import org.opendaylight.fpc.utils.ErrorLog;


/**
 * A servlet context listener that sends event-data pairs for config result notifications
 */
public class NotificationService implements ServletContextListener {
	//key = client id, value = config-result-notification
   public static BlockingQueue<Map.Entry<String,String>> blockingQueue = new LinkedBlockingQueue<Map.Entry<String,String>>();
   public void contextInitialized(ServletContextEvent sce) {
	   Map<String, AsyncContext> notificationStreams = new ConcurrentHashMap<String, AsyncContext>();
	   sce.getServletContext().setAttribute("notificationStreams", notificationStreams);
	   Thread thread = new Thread(new Runnable(){

		@Override
		public void run() {
			String clientId = null;
			AsyncContext asyncContext = null;
			while(true)
			   {
				   try {
					   Map.Entry<String, String> entry = blockingQueue.take();
					   clientId = entry.getKey();
					   asyncContext = notificationStreams.get(clientId);
					   if(notificationStreams.get(entry.getKey()) != null){
						   ServletOutputStream out = asyncContext.getResponse().getOutputStream();
						   out.write(entry.getValue().getBytes());
						   out.flush();
						   asyncContext.getResponse().flushBuffer();
					   }
				   } catch (Exception e){
					   ErrorLog.logError("Cannot write to client",	e.getStackTrace());
					   asyncContext.complete();
					   notificationStreams.remove(clientId);
					   break;
				   }
			   }
		}

	   });
	   thread.start();

   }

   public void contextDestroyed(ServletContextEvent sce) {
   }
}
