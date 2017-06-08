/*
 * Copyright © 2016 - 2017 Copyright (c) Sprint, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.fpc.notification;

import java.math.BigInteger;
import java.util.AbstractMap;
import java.util.Collection;

import org.opendaylight.controller.md.sal.binding.api.NotificationPublishService;
import org.opendaylight.fpc.impl.FpcServiceImpl;
import org.opendaylight.fpc.utils.ErrorLog;
import org.opendaylight.fpc.utils.FpcCodecUtils;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.ClientIdentifier;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.ConfigResultNotification;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.ConfigResultNotificationBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.NotificationId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.Notify;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.NotifyBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.OpIdentifier;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.OpStatusValue.OpStatus;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.config.result.notification.value.ConfigResultBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.notify.Value;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.notify.value.DownlinkDataNotification;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.notify.value.DpnAvailability;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.notify.value.MonitorNotification;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.result.body.ResultType;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Uri;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.Notification;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Throwables;

/**
 * Primary Notification Interface
 */
public class Notifier {
    private static final Logger LOG = LoggerFactory.getLogger(Notifier.class);
    /**
     * Qname
     */
    public static final QName TOP_ODL_FPC_QNAME =
            QName.create("urn:ietf:params:xml:ns:yang:fpcagent", "2016-08-03","config-result-notification").intern();
    static final YangInstanceIdentifier configResultNotificationYII =
            YangInstanceIdentifier.of(TOP_ODL_FPC_QNAME);
    static final InstanceIdentifier<ConfigResultNotification> configResultNotificationII =
            InstanceIdentifier.create(ConfigResultNotification.class);
    static final FpcCodecUtils fpcCodecUtils;
    static {
        try {
            fpcCodecUtils = FpcCodecUtils.get(ConfigResultNotification.class, configResultNotificationYII);
        } catch (Exception e) {
            LOG.error("Exception occured during FpcCodecUtilsInitialization");
            throw Throwables.propagate(e);
        }
    }

    protected static NotificationPublishService notificationService;

    /**
     * Initialization function.
     * @param notificationSvc - NotificationPublishService
     */
    public static void init(NotificationPublishService notificationSvc) {
        notificationService = notificationSvc;
    }

    /**
     * Issues a Configuration Result over the Northbound over the NotificationPublishService
     * @param clientId - Client Identifier
     * @param opid - Operation Identifier
     * @param status - Operation Status
     * @param rt - Result Type
     * @param issueInternal - indicates if an internal framework notification should also occur
     * @param causeValue - cause value returned from DPN
     */
    static public void issueConfigResult(
            ClientIdentifier clientId,
            OpIdentifier opid,
            OpStatus status,
            ResultType rt,
            boolean issueInternal,
            Short causeValue) {
        Long notificationId = NotificationInfo.next();

        ConfigResultNotification result = new ConfigResultNotificationBuilder()
                .setNotificationId(new NotificationId(notificationId))
                .setTimestamp(BigInteger.valueOf(System.currentTimeMillis()))
                .setValue(new ConfigResultBuilder()
                        .setOpId(opid)
                        .setOpStatus(status)
                        .setResultType(rt)
                        .setCauseValue(Long.valueOf(causeValue))
                        .build())
                .build();
        try {
            Uri uri = FpcServiceImpl.getNotificationUri(clientId);
            if (uri != null) {
                if (uri.getValue().startsWith("http") &&
                    (HTTPClientPool.instance() != null)) {
                    HTTPClientPool.instance().getWorker().getQueue().put(
                        new AbstractMap.SimpleEntry<Uri,Notification>(
                                uri,
                                result));
                }
            }
            if (issueInternal &&
                    (notificationService != null)) {
                notificationService.putNotification(result);
            }
        } catch (InterruptedException e) {
            LOG.warn("Notification Service Interruption occurred while sending a Read Notification");
            ErrorLog.logError(e.getStackTrace());
        }
    }

    /**
     * Issue notification to the specified list of Uris
     * @param value - Notification to issue
     * @param uris - Uris to issue the notification to
     */
    static protected void issueNotification(Value value, Collection<Uri> uris) {
        Long notificationId = NotificationInfo.next();
        Notify notif = new NotifyBuilder()
                .setNotificationId(new NotificationId(notificationId))
                .setTimestamp(BigInteger.valueOf(System.currentTimeMillis()))
                .setValue(value)
                .build();

        for(Uri uri : uris) {
            if (uri.getValue().startsWith("http") &&
                    (HTTPClientPool.instance() != null)) {
                    try {
                        HTTPClientPool.instance().getWorker().getQueue().put(
                            new AbstractMap.SimpleEntry<Uri,Notification>(
                                    uri,
                                    notif));
                    } catch (InterruptedException e) {
                    	ErrorLog.logError(e.getStackTrace());
                    }
                }
        }
    }

    /**
     * Issue a Downlink Data Notification.
     * @param uris - Uris to issue the notification to
     * @param ddn - Downlink Data Notification to be issued
     */
    static public void issueDownlinkDataNotification(Collection<Uri> uris,
            DownlinkDataNotification ddn) {
        issueNotification(ddn, uris);
    }

    /**
     * Issue a DPN Availability Notification
     * @param uris - Uris to issue the notification to
     * @param dpnAvailability - DPN
     */
    static public void issueDpnAvailabilityNotification(Collection<Uri> uris, DpnAvailability dpnAvailability){
    	issueNotification(dpnAvailability, uris);
    }

    /**
     * Issue a blob (unstructured) Notification
     * @param uris - Uris to issue the notification to
     * @param mn - Unstructured Notification to issue
     */
    static public void issueBlobNotification(Collection<Uri> uris,
            MonitorNotification mn) {
        issueNotification(mn, uris);
    }
}
