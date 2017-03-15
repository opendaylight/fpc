/*
 * Copyright © 2016 Copyright (c) Sprint, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.fpc.monitor;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.opendaylight.fpc.activation.cache.StorageCache;
import org.opendaylight.fpc.activation.cache.StorageCacheUtils;
import org.opendaylight.fpc.impl.FpcServiceImpl;
import org.opendaylight.fpc.notification.Notifier;
import org.opendaylight.fpc.tenant.TenantManager;
import org.opendaylight.fpc.utils.FpcCodecUtils;
import org.opendaylight.fpc.utils.NameResolver;
import org.opendaylight.mdsal.dom.api.DOMDataTreeChangeListener;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.ClientIdentifier;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.notify.value.MonitorNotificationBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.notify.value.monitor.notification.monitor.notification.value.SimpleMonitorBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcbase.rev160803.FpcIdentity;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcbase.rev160803.report.ReportValue;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcbase.rev160803.report.config.event.config.value.ThresholdConfig;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcbase.rev160803.report.report.value.AnyDataBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Uri;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeCandidate;
import org.opendaylight.yangtools.yang.data.api.schema.tree.ModificationType;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;

/**
 * Threshold Monitor Manager.
 */
public class ChangeMonitor implements AutoCloseable {
    static private NameResolver resolver = NameResolver.get();
    static private FpcCodecUtils codecs = FpcCodecUtils.get();
    static private Map<String, ThreshListener> thresholds = new HashMap<String, ThreshListener>();
    static private ChangeMonitor cm = new ChangeMonitor();

    /**
     * Threshold Monitor (ChangeListner)
     */
    protected class ThreshListener implements DOMDataTreeChangeListener, AutoCloseable {
        final ListenerRegistration<DOMDataTreeChangeListener> registration;
        final Uri uri;
        final YangInstanceIdentifier yiid;
        final StorageCache sc;
        final SimpleMonitorBuilder smb;

        /**
         * Constructor.
         *
         * @param clientId - Client Idnetifier
         * @param smb - SimpleMonitorBuilder prepared to generate the Notification.
         */
        public ThreshListener(ClientIdentifier clientId, SimpleMonitorBuilder smb) {
            this.yiid = resolver.toInstanceIdentifier(resolver.extractString(smb.getTarget()));
            // TODO - A Schema check to verify the monitored node is a numeric type.
            Preconditions.checkNotNull(yiid);
            this.smb = smb;
            this.uri = FpcServiceImpl.getNotificationUri(clientId);
            this.sc = TenantManager.getTenantManager(clientId).getSc();
            this.registration = sc.watch(yiid, this);
            thresholds.put(this.smb.getMonitorId().toString(), this);
            StorageCacheUtils.writeMonitor(this.sc, smb.getMonitorId(), smb.getTarget(), smb.getEventConfigValue());
        }

        /**
         * Issues a Notification.
         */
        public void issueNotification() {
            // TODO - Change the Report from AnyData to SimpleReport Type
            Map.Entry<InstanceIdentifier<?>, DataObject> val = sc.readAsPair(yiid);
            if (val != null) {
                String retVal = codecs.jsonStringFromDataObject(val.getKey(), val.getValue());
                smb.setReportValue((ReportValue) new AnyDataBuilder().setData(retVal));
                Notifier.issueBlobNotification(Arrays.asList(uri), new MonitorNotificationBuilder()
                        .setMonitorNotificationValue(smb.build()).build());
            }
        }

        @Override
        public void close() {
            registration.close();
            thresholds.remove(this.smb.getMonitorId().toString());
            StorageCacheUtils.removeMonitor(this.sc, this.smb.getMonitorId());
        }

        @Override
        public void onDataTreeChanged(Collection<DataTreeCandidate> arg0) {
            ModificationType mt = arg0.iterator().next().getRootNode().getModificationType();

            if (mt == ModificationType.DELETE) {
                close();
            } else {
                Optional<NormalizedNode<?,?>> node = arg0.iterator().next().getRootNode().getDataAfter();
                if (node.isPresent()) {
                    Object val = node.get().getValue();
                    try {
                        long l = Long.parseLong(val.toString());
                        Long lo = ((ThresholdConfig)this.smb.getEventConfigValue()).getLoThresh();
                        Long hi = ((ThresholdConfig)this.smb.getEventConfigValue()).getHiThresh();
                        if (((lo != null) && (l < lo)) || ((hi != null) && (l > hi)))    {
                            issueNotification();
                        }
                    } catch (NumberFormatException  e) {
                        // Do Nothing.
                    }
                }
            }
        }
    }

    /**
     * Indicates if this Manager has the specified Monitor.
     * @param monitorId - Monitor Identity
     * @return true if this Manager has the specified Monitor; false otherwise
     */
    static public boolean hasMonitorId(FpcIdentity monitorId) {
        return thresholds.containsKey(monitorId.toString());
    }

    /**
     * Registers a new Threshold Configuration Monitor.
     * @param monitorId - Monitor Identity
     * @param clientId - Client Identity
     * @param target - Monitoring Target
     * @param config - Monitor Configuration
     */
    static public void register(FpcIdentity monitorId, ClientIdentifier clientId, FpcIdentity target,
            ThresholdConfig config) {
        cm.new ThreshListener(clientId, new SimpleMonitorBuilder()
                .setMonitorId(monitorId)
                .setTarget(target)
                .setEventConfigValue(config));
    }

    /**
     * Generates a Notification if the Monitor is present in this Manager.
     * @param monitorId - Monitor Identity
     */
    public static void probe(FpcIdentity monitorId) {
        ThreshListener entry = thresholds.get(monitorId.toString());
        if  (entry != null) {
            entry.issueNotification();
        }
    }

    /**
     * Deregisters a Monitor if the Monitor is present in this Manager.
     * @param monitorId - Monitor Identity
     */
    public static void deregister(FpcIdentity monitorId) {
        ThreshListener entry = thresholds.get(monitorId.toString());
        if  (entry != null) {
            entry.close();
        }
    }

    @Override
    public void close() throws Exception {
        // Intentionally does nothing.
    }
}
