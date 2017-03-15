/*
 * Copyright © 2016 Copyright (c) Sprint, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.fpc.activation.cache;

import java.util.List;
import java.util.Map;

import org.opendaylight.fpc.tenant.TenantManager;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.tenants.tenant.fpc.mobility.Monitors;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.tenants.tenant.fpc.mobility.MonitorsBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.tenants.tenant.fpc.mobility.MonitorsKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcbase.rev160803.FpcContext;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcbase.rev160803.FpcIdentity;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcbase.rev160803.FpcPort;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcbase.rev160803.report.config.EventConfigValue;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcbase.rev160803.targets.value.Targets;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

/**
 * Utility Class for Writing Monitors to the Storage Cache.
 */
public class StorageCacheUtils {
    /**
     * Writes a Monitor to a Storage Cache.
     * @param cache - Storage Cache to be updated
     * @param monitorId - Monitor Identity
     * @param target - Target of the Monitor
     * @param value - Monitor Configuration
     */
    static public void writeMonitor(StorageCache cache,
            FpcIdentity monitorId,
            FpcIdentity target,
            EventConfigValue value) {
        MonitorsBuilder mb = new MonitorsBuilder()
                .setKey(new MonitorsKey(monitorId))
                .setMonitorId(monitorId)
                .setTarget(target)
                .setEventConfigValue(value);
        Map.Entry<YangInstanceIdentifier,NormalizedNode<?,?>> node = cache.codecs.getCodecRegistry().toNormalizedNode(
                cache.mobilityIid.child(Monitors.class, mb.getKey()), mb.build());
        cache.write(monitorId.toString(), node.getKey(), node.getValue());
    }

    /**
     * Removes a monitor (if it existed) from the Cache.
     * @param cache - Storage Cache to be updated
     * @param monitorId - Monitor Identity to be removed
     */
    static public void removeMonitor(StorageCache cache,
            FpcIdentity monitorId) {
        cache.remove(monitorId.toString());
    }

    /**
     * Reads Targets from the Tenant's Cache (Storage Cache).
     * @param targets - Targets to be read
     * @param tenant - Associated Tenant
     * @return - An OpCache containing all successfully read results
     * @throws Exception if the read fails during processing
     */
    static public OpCache read(List<Targets> targets, TenantManager tenant) throws Exception {
        if (targets == null) return null;
        OpCache bc = new OpCache();

        for (Targets target : targets) {
            DataObject dobj = (target.getTarget().getInstanceIdentifier() != null) ?
                    tenant.getSc().read(target.getTarget().getInstanceIdentifier()) :
                    tenant.getSc().read(((target.getTarget().getString() != null) ? target.getTarget().getString() :
                        target.getTarget().getUint32().toString()));

            if (dobj instanceof FpcContext) {
                bc.addContext((FpcContext) dobj);
            } else if (dobj instanceof FpcPort) {
                bc.addPort((FpcPort) dobj);
            }
        }
        return bc;
    }
}
