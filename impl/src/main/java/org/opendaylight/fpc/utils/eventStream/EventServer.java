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
import java.util.ArrayList;
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
 * A Http Servlet to server response streams
 */
public class EventServer extends HttpServlet {
	private static final Logger LOG = LoggerFactory.getLogger(EventServer.class);
	private static ArrayList<String> clientUriList = new ArrayList<String>();

	/**
	 * Method for stream initialization
	 * @param clientUri - Client Uri
	 * @param request - The servlet request object
	 * @param response - The servlet Response Object
	 */
	private void init(String clientUri,HttpServletRequest request, HttpServletResponse response){
		LOG.info("Response Stream Inititated");
		try{
			HttpSession session = request.getSession();
			session.setMaxInactiveInterval(60*60);
			EventClient client = new EventClient();
			client.connectToClient(clientUri);
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
			Map<String,AsyncContext> responseStreams = (ConcurrentHashMap<String,AsyncContext>) servletContext.getAttribute("responseStreams");
			responseStreams.put(clientUri,asyncContext);
		} catch (Exception e){
			ErrorLog.logError(e.getMessage(),e.getStackTrace());
		}
	}

	protected void doPost( HttpServletRequest request, HttpServletResponse response){
		try{
			String clientUri = null;
			StringBuffer jsonStringBuilder = new StringBuffer();
			String line = null;
			try {
				BufferedReader br = request.getReader();
				while ((line = br.readLine()) != null)
					jsonStringBuilder.append(line);
			} catch (Exception e) { e.printStackTrace(); }

			try {
				if(jsonStringBuilder.length()>0){
					JSONObject jsonObj =  new JSONObject(jsonStringBuilder.toString());
					clientUri = jsonObj.getString("client-uri");
					if(!clientUriList.contains(clientUri)){
						init(clientUri, request, response);
					}
					jsonStringBuilder.setLength(0);
				}
			} catch (JSONException e) {
				ErrorLog.logError(e.getLocalizedMessage(),e.getStackTrace());
			}
		} catch (Exception e) {
			ErrorLog.logError(e.getLocalizedMessage(),e.getStackTrace());
		}

	}
}
