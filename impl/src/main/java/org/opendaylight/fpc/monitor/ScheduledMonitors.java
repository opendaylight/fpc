/*
 * Copyright © 2016 Copyright (c) Sprint, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.fpc.monitor;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.opendaylight.fpc.activation.cache.StorageCacheUtils;
import org.opendaylight.fpc.activation.cache.StorageCache;
import org.opendaylight.fpc.impl.FpcServiceImpl;
import org.opendaylight.fpc.notification.Notifier;
import org.opendaylight.fpc.tenant.TenantManager;
import org.opendaylight.fpc.utils.FpcCodecUtils;
import org.opendaylight.fpc.utils.NameResolver;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.ClientIdentifier;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.notify.value.MonitorNotificationBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.notify.value.monitor.notification.monitor.notification.value.SimpleMonitorBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcbase.rev160803.FpcIdentity;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcbase.rev160803.report.ReportValue;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcbase.rev160803.report.config.event.config.value.PeriodicConfig;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcbase.rev160803.report.config.event.config.value.ScheduledConfig;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcbase.rev160803.report.report.value.AnyDataBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Uri;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;

import com.google.common.base.Preconditions;

/**
 * Scheduled Monitors Manager.
 */
public class ScheduledMonitors {
    static public Long ScheduleHorizon = 600000L; // 60 Seconds
    static private ScheduledMonitors em = new ScheduledMonitors();
    static private NameResolver resolver = NameResolver.get();
    static private FpcCodecUtils codecs = FpcCodecUtils.get();
    static public Map<String, ScheduledEvent> registrations = new HashMap<String, ScheduledEvent>();
    static private ScheduledThreadPoolExecutor threadPool;

    /**
     * Initializer
     * @param threadPoolSize - Size of the schedule thread pool
     */
    static public void init(int threadPoolSize) {
        Preconditions.checkArgument((threadPoolSize != 0), "The ScheduledMonitors threadpool size CANNOT be zero");
        threadPool = new ScheduledThreadPoolExecutor(threadPoolSize);
    }

    /**
     * Scheduled Event Task.
     */
    public class ScheduledEvent implements Runnable, AutoCloseable {
        final Uri uri;
        final YangInstanceIdentifier yiid;
        final StorageCache sc;
        final SimpleMonitorBuilder smb;
        ScheduledFuture<ScheduledEvent> schedFuture = null;

        /**
         * Constructor.
         *
         * @param clientId - Client Idnetifier
         * @param smb - SimpleMonitorBuilder prepared to generate the Notification.
         */
        public ScheduledEvent(ClientIdentifier clientId, SimpleMonitorBuilder smb) {
            this.yiid = resolver.toInstanceIdentifier(resolver.extractString(smb.getTarget()));
            Preconditions.checkNotNull(yiid);
            this.smb = smb;
            this.uri = FpcServiceImpl.getNotificationUri(clientId);
            this.sc = TenantManager.getTenantManager(clientId).getSc();
            registrations.put(smb.getMonitorId().toString(), this);
            StorageCacheUtils.writeMonitor(this.sc, smb.getMonitorId(), smb.getTarget(), smb.getEventConfigValue());
        }

        /**
         * Issues a Notification.
         */
        public void issueNotification() {
            Map.Entry<InstanceIdentifier<?>, DataObject> val = sc.readAsPair(yiid);
            if (val != null) {
                String retVal = codecs.jsonStringFromDataObject(val.getKey(), val.getValue());
                smb.setReportValue((ReportValue) new AnyDataBuilder().setData(retVal));
                Notifier.issueBlobNotification(Arrays.asList(uri), new MonitorNotificationBuilder()
                        .setMonitorNotificationValue(smb.build()).build());
            }
        }

        /**
         * Sets the future for this event.
         * @param future - ScheduledFuture that will receive the data when the task is exectued.
         */
        protected void setFuture(ScheduledFuture<ScheduledEvent> future) {
            this.schedFuture = future;
        }

        @Override
        public void close() {
            schedFuture.cancel(false);
            StorageCacheUtils.removeMonitor(this.sc, this.smb.getMonitorId());
        }

        @Override
        public void run() {
            issueNotification();
            registrations.remove(smb.getMonitorId().toString());
        }
    }

    /**
     * Indicates if this Manager has the specified Monitor.
     * @param monitorId - Monitor Identity
     * @return true if this Manager has the specified Monitor; false otherwise
     */
    public static boolean hasMonitorId(FpcIdentity monitorId) {
        return registrations.containsKey(monitorId.toString());
    }

    /**
     * Registers a new Threshold Configuration Monitor.
     * @param monitorId - Monitor Identity
     * @param clientId - Client Identity
     * @param target - Monitoring Target
     * @param conf - Monitor Configuration
     */
    static public void register(FpcIdentity monitorId, ClientIdentifier clientId, FpcIdentity target,
            ScheduledConfig conf) {
        ScheduledEvent evt = em.new ScheduledEvent(clientId, new SimpleMonitorBuilder()
                .setMonitorId(monitorId)
                .setTarget(target)
                .setEventConfigValue(conf));
        ScheduledFuture<?> future = threadPool.schedule(evt, conf.getReportTime() - System.currentTimeMillis(),
                TimeUnit.MILLISECONDS);
        evt.setFuture((ScheduledFuture<ScheduledEvent>) future);
    }

    /**
     * Registers a new Threshold Configuration Monitor.
     * @param monitorId - Monitor Identity
     * @param clientId - Client Identity
     * @param target - Monitoring Target
     * @param conf - Monitor Configuration
     */
    static public void register(FpcIdentity monitorId, ClientIdentifier clientId, FpcIdentity target,
            PeriodicConfig conf) {
        ScheduledEvent evt = em.new ScheduledEvent(clientId, new SimpleMonitorBuilder()
                .setMonitorId(monitorId)
                .setTarget(target)
                .setEventConfigValue(conf));
        ScheduledFuture<?> future = threadPool.scheduleAtFixedRate(evt, 0, conf.getPeriod(), TimeUnit.MILLISECONDS);
        evt.setFuture((ScheduledFuture<ScheduledEvent>) future);
    }

    /**
     * Generates a Notification if the Monitor is present in this Manager.
     * @param monitorId - Monitor Identity
     */
    public static void probe(FpcIdentity monitorId) {
        if (registrations.containsKey(monitorId.toString())) {
            registrations.get(monitorId.toString()).issueNotification();
        }
    }

    /**
     * Deregisters a Monitor if the Monitor is present in this Manager.
     * @param monitorId - Monitor Identity
     */
    public static void deregister(FpcIdentity monitorId) {
        if (registrations.containsKey(monitorId.toString())) {
            ScheduledEvent evt = registrations.remove(monitorId.toString());
            if (evt != null) {
                evt.close();
            }
        }
    }
}
