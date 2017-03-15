/*
 * Copyright © 2016 Copyright (c) Sprint, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.fpc.monitor;

import com.google.common.base.Preconditions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.fpc.agent.info.SupportedEvents;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.fpc.agent.info.SupportedEventsBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.fpc.agent.info.SupportedEventsKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcbase.rev160803.EventType;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcbase.rev160803.EventTypeId;

/**
 * Event Class.
 */
public class Events {
    private static final Set<Long> eventIds = new HashSet<Long>();
    private static final Map<Class<? extends EventType>, Long> eventMappingsByIdentity =
            new HashMap<Class<? extends EventType>, Long>();
    private static final Map<String, Long> eventMappingsByName = new HashMap<String, Long>();

    public static final long CONTROLLER_UP = 0L;
    public static final long CONTROLLER_DOWN= 1L;
    public static final long DPN_UP = 2L;
    public static final long DPN_DOWN = 3L;
    public static final long DPN_CANDIDATE_AVAILABLE = 4L;
    public static final long DOWNLINK_DATA_NOTIFY = 5L;
    public static final long OVERLOAD_INDICATION = 6L;

    static {
        eventIds.addAll(Arrays.asList(CONTROLLER_UP, CONTROLLER_DOWN, DPN_UP, DPN_DOWN, DPN_CANDIDATE_AVAILABLE,
                DOWNLINK_DATA_NOTIFY, OVERLOAD_INDICATION));
        String[] eventNames = { "CONTROLLER UP", "CONTROLLER DOWN", "DPN UP", "DPN DOWN", "DPN_CANDIDATE_AVAILABLE",
                     "DOWNLINK_DATA_NOTIFY", "OVERLOAD INDICATION" };
        Iterator<Long> it = eventIds.iterator();
        for (String val : eventNames) {
            eventMappingsByName.put(val, it.next());
        }
    }

    /**
     * Adds an Event type to the system.
     * @param value - Event Id value
     * @param ident - Yang Identity
     * @param displayName - Human readable display Name for the event.
     * @return true if the event was successfully add; false otherwises
     */
    static public boolean addEvent(Long value, Class<? extends EventType> ident, String displayName) {
        Preconditions.checkArgument((value != null), "Long value cannot be empty");
        if (!eventIds.add(value)) {
            return false;
        }
        if (displayName != null) {
            eventMappingsByName.put(displayName, value);
        }
        if (ident != null) {
            eventMappingsByIdentity.put(ident, value);
        }
        return true;
    }

    /**
     * Retrieves the numeric Identity associated with the Yang Identity.
     * @param ident - Yang Identity
     * @return long representing the numeric identity; null otherwise.
     */
    static public Long getIdentity(Class<? extends EventType> ident) {
        return eventMappingsByIdentity.get(ident);
    }

    /**
     * Retrieves the numeric Identity associated with the display name.
     * @param name - Display Name
     * @return long representing the numeric identity; null otherwise.
     */
    static public Long getName(String name) {
        return eventMappingsByName.get(name);
    }

    /**
     * Indicates if the Manager has the specified event identifier.
     * @param id - Event Id
     * @return true if the  Manager has the specified event identifier; false otherwise.
     */
    static public boolean hasEventId(Long id) {
        return eventIds.contains(id);
    }

    /**
     * Returns the supported Events.
     * @return A list of Supported Events;
     */
    static public List<SupportedEvents> getSupportedEvents() {
        SupportedEventsBuilder sb = new SupportedEventsBuilder();
        List<SupportedEvents> retVal = new ArrayList<SupportedEvents>();
        for (Map.Entry<Class<? extends EventType>, Long> val : eventMappingsByIdentity.entrySet()) {
            retVal.add(sb.setEvent(val.getKey())
            .setEventId(new EventTypeId(val.getValue()))
            .setKey(new SupportedEventsKey(val.getKey())).build());
        }
        return retVal;
    }

    /**
     * Removes an Event from the manager.
     * @param value - Event Identity (as long)
     * @param ident - Yang Identity
     * @param displayName - Display Name
     */
    static public void removeEvent(Long value, Class<? extends EventType> ident, String displayName) {
        Preconditions.checkArgument((value != null), "Long value cannot be empty");
        if (!eventIds.remove(value)) {
            return;
        }
        if (displayName != null) {
            eventMappingsByName.remove(displayName);
        }
        if (ident != null) {
            eventMappingsByIdentity.remove(ident);
        }
    }
}
