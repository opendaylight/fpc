/*
 * Copyright © 2016 - 2017 Copyright (c) Sprint, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.fpc.utils.eventStream;

import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.nio.SelectChannelConnector;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * SSE Server implementation
 */
public class JettyServer {
	private static final Logger LOG = LoggerFactory.getLogger(JettyServer.class);

	/**
	 * Method used to initialize and start the SSE server
	 */
	public static void init()
	{
		Server server = new Server();
		SelectChannelConnector connector = new SelectChannelConnector();
		connector.setPort(8070);
		connector.setAcceptors(3);
		connector.setThreadPool(new QueuedThreadPool(50));
		connector.setMaxIdleTime(72*60*60*1000);
		connector.setAcceptQueueSize(50000);
		connector.setRequestBufferSize(50000);
		connector.setResponseBufferSize(50000);
		server.setConnectors(new Connector[] { connector });
		ServletContextHandler context = new ServletContextHandler(
				ServletContextHandler.SESSIONS);
		context.setContextPath("/");
		//context.setResourceBase(System.getProperty("java.io.tmpdir"));

		ServletHolder requestServletHolder = new ServletHolder(EventServer.class);
		context.addServlet(requestServletHolder, "/response");

		ServletHolder notificationServletHolder = new ServletHolder(NotificationServer.class);
		context.addServlet(notificationServletHolder, "/notification");


		server.setHandler(context);
		context.addEventListener(new ConfigureService());
		context.addEventListener(new NotificationService());

		try {
			server.start();
			LOG.info("Server Started");
			server.join();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}


	}

}
