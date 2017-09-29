/*
 * Copyright © 2016 - 2017 Copyright (c) Sprint, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.fpc.utils.eventStream;

import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

import javax.servlet.AsyncContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.ServletOutputStream;

import org.opendaylight.fpc.activation.cache.transaction.Transaction;
import org.opendaylight.fpc.utils.ErrorLog;


/**
 * A servlet context listener that sends response (ConfigureOutput) events to clients.
 */
public class ConfigureService implements ServletContextListener {
	//key = client-id, value = configure-output
   public static BlockingQueue<Map.Entry<String,Map.Entry<Transaction,String>>> blockingQueue = new LinkedBlockingQueue<Map.Entry<String,Map.Entry<Transaction,String>>>();
   public static BlockingQueue<Map.Entry<String,String>> registerClientQueue = new LinkedBlockingQueue<Map.Entry<String,String>>();

   public void contextInitialized(ServletContextEvent sce) {
	   Map<String, AsyncContext> responseStreams = new ConcurrentHashMap<String, AsyncContext>();
	   sce.getServletContext().setAttribute("responseStreams", responseStreams);
	   Thread thread = new Thread(new Runnable(){

		@Override
		public void run() {
			String clientUri = null;
			AsyncContext asyncContext = null;
			while(true)
			   {
				   try {
					   Map.Entry<String,Map.Entry<Transaction,String>> entry = blockingQueue.take();
					   clientUri = entry.getKey();
					   asyncContext = responseStreams.get(clientUri);
					   if(responseStreams.get(entry.getKey()) != null){
						   ServletOutputStream out = asyncContext.getResponse().getOutputStream();
						   out.write(entry.getValue().getValue().getBytes());
						   out.flush();
						   asyncContext.getResponse().flushBuffer();
						   entry.getValue().getKey().setResponseSent();
					   }
				   } catch (Exception e){
					   ErrorLog.logError("Exception - Cannot write to client",	e.getStackTrace());
					   asyncContext.complete();
					   responseStreams.remove(clientUri);
					   break;
				   }
			   }
		}

	   });
	   thread.start();

	   Thread thread1 = new Thread(new Runnable(){

			@Override
			public void run() {
				String clientUri = null;
				AsyncContext asyncContext = null;
				while(true)
				   {
					   try {
						   Map.Entry<String,String> entry = registerClientQueue.take();
						   clientUri = entry.getKey();
						   asyncContext = responseStreams.get(clientUri);
						   if(responseStreams.get(entry.getKey()) != null){
							   ServletOutputStream out = asyncContext.getResponse().getOutputStream();
							   out.write(entry.getValue().getBytes());
							   out.flush();
							   asyncContext.getResponse().flushBuffer();
						   }
					   } catch (Exception e){
						   ErrorLog.logError("Exception - Cannot write to client",	e.getStackTrace());
						   asyncContext.complete();
						   responseStreams.remove(clientUri);
						   break;
					   }
				   }
			}

		   });
		   thread1.start();

   }

   public void contextDestroyed(ServletContextEvent sce) {
   }
}
