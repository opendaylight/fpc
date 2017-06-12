/*
 * Copyright © 2016 - 2017 Copyright (c) Sprint, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.fpc.dpn;

import java.util.Map;

import org.opendaylight.fpc.activation.Activator;
import org.opendaylight.fpc.assignment.IPv4RangeManager;
import org.opendaylight.fpc.utils.Counter;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.tenants.tenant.fpc.topology.Dpns;

/**
 * A generic holder for DPN specific components, e.g. Activator,
 * IP Assignment Manager, TEID Assignment Manager.
 *
 * - ipv4 Pool Managers use a String as key.  This is often a IPv4 pool name.
 * - teid Managers use the String readable (quad notation) of the IPv4 address as key.
 */
public class DpnHolder {
    public Map<String, IPv4RangeManager> ipv4PoolManagers;
    public Map<String, Counter> teidManagers;
    public Activator activator;
    public Dpns dpn;

    /**
     * Default Constructor.
     * @param dpn -DPN
     */
    public DpnHolder(Dpns dpn) {
        this.dpn = dpn;
    }
}
