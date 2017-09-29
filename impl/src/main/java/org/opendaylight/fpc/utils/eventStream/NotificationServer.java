/*
 * Copyright © 2016 - 2017 Copyright (c) Sprint, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.fpc.utils.eventStream;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.AsyncContext;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.json.JSONException;
import org.json.JSONObject;
import org.opendaylight.fpc.utils.ErrorLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A HTTP servlet that handles requests for notification streams
 */
public class NotificationServer extends HttpServlet {
	private static final Logger LOG = LoggerFactory.getLogger(EventServer.class);
	protected void doPost( HttpServletRequest request, HttpServletResponse response){
		LOG.info("Notification stream Inititated");
		String clientId = null;
		StringBuffer jsonStringBuilder = new StringBuffer();
		String line = null;
		try {
			BufferedReader br = request.getReader();
			while ((line = br.readLine()) != null)
				jsonStringBuilder.append(line);
		} catch (Exception e) { e.printStackTrace(); }

		try {
			if(jsonStringBuilder.length() > 0){
				JSONObject jsonObj =  new JSONObject(jsonStringBuilder.toString());
				clientId = jsonObj.getString("client-id");
				jsonStringBuilder.setLength(0);
				HttpSession session = request.getSession();
				session.setMaxInactiveInterval(60*60);
				response.setHeader("Content-Type", "text/event-stream");
				response.setHeader("Cache-Control", "no-cache, no-store");
				response.setHeader("Connection", "keep-alive");
				AsyncContext asyncContext = request.startAsync(request,response);
				asyncContext.setTimeout(60*60*1000);
				asyncContext.getResponse().setBufferSize(1200);
				try {
					asyncContext.getResponse().flushBuffer();
				} catch (IOException e1) {
					ErrorLog.logError(e1.getMessage(),e1.getStackTrace());
				}
				ServletContext servletContext = request.getServletContext();
				Map<String,AsyncContext> notificationStreams = (ConcurrentHashMap<String,AsyncContext>) servletContext.getAttribute("notificationStreams");
				notificationStreams.put(clientId,asyncContext);
				LOG.info("Client Id received in the notification stream request: "+clientId);
			}
		} catch (JSONException e) {
			ErrorLog.logError(e.getLocalizedMessage(),e.getStackTrace());
		}
	}
}
