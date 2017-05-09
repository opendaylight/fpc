/*
 * Copyright © 2016 Copyright (c) Sprint, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.fpc.notification;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.util.AbstractMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;

import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.concurrent.FutureCallback;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClients;
import org.apache.http.nio.client.methods.HttpAsyncMethods;
import org.opendaylight.fpc.utils.ErrorLog;
import org.opendaylight.fpc.utils.FpcCodecUtils;
import org.opendaylight.fpc.utils.Worker;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.ConfigResultNotification;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Uri;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.Notification;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Throwables;

/**
 * HTTP Notification Worker.
 */
class HTTPNotifier implements Worker {
    private static final Logger LOG = LoggerFactory.getLogger(HTTPNotifier.class);
    public static final QName TOP_ODL_FPC_QNAME =
            QName.create("urn:ietf:params:xml:ns:yang:fpcagent", "2016-08-03","config-result-notification").intern();
    private static final YangInstanceIdentifier configResultNotificationYII =
            YangInstanceIdentifier.of(TOP_ODL_FPC_QNAME);
    private static final InstanceIdentifier<ConfigResultNotification> configResultNotificationII =
            InstanceIdentifier.create(ConfigResultNotification.class);
    private static final CloseableHttpAsyncClient client = HttpAsyncClients.createDefault();

    // TODO - This is prob a bad idea... FIX ME!
    private static final Map<String, HttpURLConnection> connections = new ConcurrentHashMap<String, HttpURLConnection>();
    private static final FpcCodecUtils fpcCodecUtils;
    static {
        try {
            fpcCodecUtils = FpcCodecUtils.get(ConfigResultNotification.class, configResultNotificationYII);
        } catch (Exception e) {
            LOG.error("Exception occured during FpcCodecUtilsInitialization");
            throw Throwables.propagate(e);
        }
    }

    protected final CountDownLatch startSignal;
    protected boolean run;
    private final BlockingQueue<AbstractMap.SimpleEntry<Uri,Notification>> blockingNotificationQueue;

    /**
     * Primary Constructor.
     *
     * @param startSignal - Latch start signal
     * @param blockingNotificationQueue - Blocking Queue assigned to the worker
     */
    public HTTPNotifier(CountDownLatch startSignal,
            BlockingQueue<AbstractMap.SimpleEntry<Uri,Notification>> blockingNotificationQueue) {
        this.run = false;
        this.startSignal = startSignal;
        this.blockingNotificationQueue = blockingNotificationQueue;
    }


    @Override
    public boolean isOpen() {
        return true;
    }

    @Override
    public void stop() {
        this.run = false;
    }

    @Override
    public void open() {
    }

    @Override
    public void close() {
    }

    /**
     * Retrieves the Worker's Queue.
     * @return A BlockingQueue that accepts a pair (Map.Entry) of Uri and Notification to be issued
     */
    public BlockingQueue<AbstractMap.SimpleEntry<Uri,Notification>> getQueue() {
        return blockingNotificationQueue;
    }

    @Override
    public void run() {
        this.run = true;
        LOG.info("HTTPNotifier RUN started");
        try {
            while(run) {
                AbstractMap.SimpleEntry<Uri,Notification> obj =
                        (AbstractMap.SimpleEntry<Uri,Notification>) blockingNotificationQueue.take();
                if (obj.getValue() instanceof ConfigResultNotification) {
                    if(obj.getKey() != null){
                        String url = obj.getKey().getValue();
                        try{
                            client.start();
                            String postBody = fpcCodecUtils.notificationToJsonString(ConfigResultNotification.class,
                                    (DataObject)obj.getValue(),
                                    true);
                            HttpRequest post = HttpAsyncMethods.createPost(url, postBody, ContentType.APPLICATION_JSON).generateRequest();
                            post.setHeader("User-Agent", "ODL Notification Agent");
                            post.setHeader("charset", "utf-8");
                            client.execute((HttpUriRequest) post, new FutureCallback<HttpResponse>() {
                                @Override
                                public void cancelled() {
                                    LOG.error(post.getRequestLine() + "-> Cancelled");
                                }

                                @Override
                                public void completed(HttpResponse resp) {
                                    LOG.debug(post.getRequestLine() + "->" + resp.getStatusLine());
                                }

                                @Override
                                public void failed(Exception e) {
                                    ErrorLog.logError(post.getRequestLine() + "->" + e.getMessage(), e.getStackTrace());
                                }
                            });
                        } catch (UnsupportedEncodingException e) {
                        	ErrorLog.logError(e.getStackTrace());
                        } catch (IOException e) {
                        	ErrorLog.logError(e.getStackTrace());
                        } catch (HttpException e) {
                        	ErrorLog.logError(e.getStackTrace());
                        } catch (Exception e){
                        	ErrorLog.logError(e.getStackTrace());
                        }
                    }
                }

            }
        } catch (InterruptedException e) {
        	ErrorLog.logError(e.getStackTrace());
        }
    }
}
