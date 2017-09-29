/*
 * Copyright © 2016 - 2017 Copyright (c) Sprint, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.fpc.utils.eventStream;

import java.io.IOException;
import java.nio.CharBuffer;
import java.util.AbstractMap;
import java.util.ArrayList;


import org.apache.http.HttpResponse;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClients;
import org.apache.http.nio.IOControl;
import org.apache.http.nio.client.methods.AsyncCharConsumer;
import org.apache.http.nio.client.methods.HttpAsyncMethods;
import org.apache.http.nio.protocol.HttpAsyncRequestProducer;
import org.apache.http.protocol.HttpContext;
import org.opendaylight.fpc.utils.ErrorLog;

/**
 * A HTTP client that sends a request to a FPC Client to initiate the request stream.
 */
public class EventClient {
	private static final CloseableHttpAsyncClient client = HttpAsyncClients.createDefault();
	private static ArrayList<String> clientUriList = new ArrayList<String>();
	protected String clientUri;
	/**
	 * Send HttpRequest to Client
	 * @param uri - FPC Client Uri
	 */
	public void connectToClient(String uri){
		this.clientUri = uri;
        try{
            client.start();
            HttpAsyncRequestProducer get = HttpAsyncMethods.createGet(this.clientUri);
            client.execute(get, new MyResponseConsumer(this.clientUri), null);
        } catch (Exception e) {
        	ErrorLog.logError(e.getStackTrace());
        }
	}

     /**
     * A character consumer to read incoming characters on the request stream
     */
    static class MyResponseConsumer extends AsyncCharConsumer<Boolean> {
    	   private String clientUri;
            /**
             * Constructor
             * @param clientUri - URI of the FPC Client
             */
            public MyResponseConsumer(String clientUri) {
				this.clientUri = clientUri;
            }

			@Override
            protected void onResponseReceived(final HttpResponse response) {
            }

            @Override
            protected void onCharReceived(final CharBuffer buf, final IOControl ioctrl) throws IOException {
            	try {
            		char[] charArray = new char[buf.remaining()];
            		System.arraycopy(buf.array(), 0, charArray, 0, buf.remaining());
            		CharBuffer charBuffer =  CharBuffer.wrap(charArray);
					ParseStream.blockingQueue.put(new AbstractMap.SimpleEntry<String, CharBuffer>(clientUri, charBuffer));
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
            }

            @Override
            protected void releaseResources() {
            }

            @Override
            protected Boolean buildResult(final HttpContext context) {
                return Boolean.TRUE;
            }

        }
	}


