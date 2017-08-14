/*
 * Copyright © 2016 - 2017 Copyright (c) Sprint, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.fpc.monitor;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.opendaylight.fpc.activation.cache.StorageCacheUtils;
import org.opendaylight.fpc.dpn.DPNStatusIndication;
import org.opendaylight.fpc.impl.FpcProvider;
import org.opendaylight.fpc.impl.FpcServiceImpl;
import org.opendaylight.fpc.notification.Notifier;
import org.opendaylight.fpc.tenant.TenantManager;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.ClientIdentifier;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.notify.value.DownlinkDataNotification;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcbase.rev160803.EventType;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcbase.rev160803.FpcDpnId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcbase.rev160803.FpcIdentity;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcbase.rev160803.report.config.EventConfigValue;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcbase.rev160803.report.config.event.config.value.EventsConfig;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcbase.rev160803.report.config.event.config.value.EventsConfigIdent;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Uri;
import com.google.common.base.Preconditions;

/**
 * Event Monitor Manager.
 */
public class EventMonitorMgr {
    /**
     * Map of Client Registrations
     */
    static public Map<String, Map.Entry<ClientIdentifier, Object>> registrations =
            new HashMap<String, Map.Entry<ClientIdentifier, Object>>();
    /**
     * Map of Tenants to Uris
     */
    static public Map<String, Map<Long, Map<Uri, Long>>> tenant2Uris =
            new HashMap<String, Map<Long, Map<Uri, Long>>>();
    /**
     * Map of imsi to Tenants
     */
    static public Map<String, String> imsi2Tenants = new HashMap<String, String>();

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
     * @param clientId - Client Identity
     * @param monitorId - Monitor Identity
     * @param config - Monitor Configuration
     * @param events - Events to be monitored
     */
    private static void register(ClientIdentifier clientId, FpcIdentity monitorId, EventConfigValue config,
            List<Long> events) {
        Uri uri = FpcServiceImpl.getNotificationUri(clientId);
        TenantManager tm = TenantManager.getTenantManagerForClient(clientId);
        FpcIdentity tenantId = tm.getTenant().getTenantId();
        registrations.put(monitorId.toString(), new
                AbstractMap.SimpleEntry<ClientIdentifier, Object>(clientId, config));
        Map<Long, Map<Uri, Long>> tenantRegistrations = tenant2Uris.get(tenantId.toString());
        if (tenantRegistrations == null) {
            tenantRegistrations = new  HashMap<Long, Map<Uri, Long>>();
            tenant2Uris.put(tenantId.toString(), tenantRegistrations);
        }
        for (Long eventId : events) {
            Map<Uri, Long> uriMaps = tenantRegistrations.get(eventId);
            if (uriMaps == null) {
                uriMaps = new HashMap<Uri, Long>();
                tenantRegistrations.put(eventId, uriMaps);
            }
            Long refs = uriMaps.get(uri);
            if (refs != null) {
                refs++;
            } else {
                uriMaps.put(uri, 1L);
            }
        }
        StorageCacheUtils.writeMonitor(tm.getSc(), monitorId, null, config);
    }

    /**
     * Registers a new Threshold Configuration Monitor.
     * @param monitorId - Monitor Identity
     * @param clientId - Client Identity
     * @param config - Monitor Configuration
     */
    public static void register(FpcIdentity monitorId, ClientIdentifier clientId, EventsConfig config) {
        Preconditions.checkNotNull(config.getEventIds());
        Preconditions.checkArgument((config.getEventIds().size() > 0));
        for (Long eventId : config.getEventIds()) {
            Preconditions.checkNotNull(Events.hasEventId(eventId));
        }
        register(clientId, monitorId, config, config.getEventIds());
    }

    /**
     * Registers a new Threshold Configuration Monitor.
     * @param monitorId - Monitor Identity
     * @param clientId - Client Identity
     * @param config - Event Configuration List
     */
    public static void register(FpcIdentity monitorId, ClientIdentifier clientId, EventsConfigIdent config) {
        Preconditions.checkNotNull(config.getEventIdentities());
        Preconditions.checkArgument((config.getEventIdentities().size() > 0));
        List<Long> values = new ArrayList<Long>();
        for (Class<? extends EventType> clazz : config.getEventIdentities()) {
            Long val = Events.getIdentity(clazz);
            Preconditions.checkNotNull(val);
            values.add(val);
        }
        register(clientId, monitorId, config, values);
    }

    /**
     * Deregisters Monitors for specified events if they are in in this Manager.
     * @param clientId - Client Identity
     * @param events - List of Events
     */
    private static void deregister(ClientIdentifier clientId, List<Long> events) {
        Uri uri = FpcServiceImpl.getNotificationUri(clientId);
        FpcIdentity tenantId = TenantManager.getTenantManagerForClient(clientId).getTenant().getTenantId();
        Map<Long, Map<Uri, Long>> tenantRegistrations = tenant2Uris.get(tenantId.toString());
        if (tenantRegistrations != null) {
            for (Long eventId : events) {
                Map<Uri, Long> uriMaps = tenantRegistrations.get(eventId);
                if (uriMaps != null) {
                    Long refs = uriMaps.get(uri);
                    if (refs == 1L) {
                        uriMaps.remove(uri);
                    } else {
                        uriMaps.put(uri, refs-1);
                    }
                }
            }
        }
    }

    /**
     * Deregisters a Monitor if the Monitor is present in this Manager.
     * @param monitorId - Monitor Identity
     */
    public static void deregister(FpcIdentity monitorId) {
        if (registrations.containsKey(monitorId.toString())) {
            Map.Entry<ClientIdentifier, Object> objPair = registrations.get(monitorId.toString());
            if (objPair.getValue() instanceof EventsConfigIdent) {
                EventsConfigIdent identConfig = (EventsConfigIdent) objPair.getValue();
                List<Long> values = new ArrayList<Long>();
                for (Class<? extends EventType> clazz : identConfig.getEventIdentities()) {
                    Long val = Events.getIdentity(clazz);
                    Preconditions.checkNotNull(val);
                    values.add(val);
                }
                deregister(objPair.getKey(), values);
            } else if (objPair.getValue() instanceof EventsConfig) {
                deregister(objPair.getKey(), ((EventsConfig)objPair.getValue()).getEventIds());
            }
            registrations.remove(monitorId).toString();
            TenantManager tm = TenantManager.getTenantManagerForClient((ClientIdentifier)objPair.getValue());
            StorageCacheUtils.removeMonitor(tm.getSc(), monitorId);
        }
    }

    /**
     * Returns the Notificaiton Uris for given tenant and event type.
     * @param tenantId - Tenant Identity
     * @param eventType - Event Type
     * @return A collect of Uris associated with the input, null otherwise.
     */
    private static Collection<Uri> getUris(String tenantId, Long eventType) {
        if (tenant2Uris.get(tenantId) != null) {
            if (tenant2Uris.get(tenantId).get(eventType) != null) {
                return tenant2Uris.get(tenantId).get(eventType).keySet();
            }
        }
        return null;
    }

    /**
     * Processes notification generation for the specified event.
     * @param dpn - Dataplane Node that originated the event.
     * @param ddn - Downlink Data Notification
     */
    public static void processEvent(FpcDpnId dpn, DownlinkDataNotification ddn) {
    	ArrayList<Uri> uris = new ArrayList<Uri>();
    	if(FpcServiceImpl.getNotificationUri(ddn.getClientId()) != null){
    		uris.add(FpcServiceImpl.getNotificationUri(ddn.getClientId()));
        	Notifier.issueDownlinkDataNotification(uris, ddn);
    	}


    }

    /**
     * Processes notification generation for the specified event.
     * @param dpn - Dataplane Node (DPN) that originated the event.
     * @param dpnStatus - DPN status
     */
    public static void processEvent(FpcDpnId dpn, DPNStatusIndication dpnStatus) {
        // TODO - We need further guidance DPNStatusIndication on use cases
    	if(dpnStatus.getStatus() == DPNStatusIndication.Status.HELLO){
            TenantManager.getTenantManager(new FpcIdentity(FpcProvider.getInstance().getConfig().getDefaultTenantId())).addDpnToDataStore(dpnStatus.getKey().split("/")[0], dpnStatus.getKey().split("/")[1]);
    	}
    	if(dpnStatus.getStatus() == DPNStatusIndication.Status.BYE){
            TenantManager.getTenantManager(new FpcIdentity(FpcProvider.getInstance().getConfig().getDefaultTenantId())).removeDpnFromDataStore(dpnStatus.getKey().split("/")[0], dpnStatus.getKey().split("/")[1]);

    	}
    }
}
