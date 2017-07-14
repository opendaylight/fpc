/*
 * Copyright © 2016 Copyright (c) Sprint, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.fpc.policy;

import java.util.Map;
import java.util.Set;
import java.util.HashMap;
import java.util.HashSet;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.fpc.activation.ActivationManager;
import org.opendaylight.fpc.activation.ActivatorFactory;
import org.opendaylight.fpc.dpn.DpnHolder;
import org.opendaylight.fpc.tenant.TenantManager;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.tenants.tenant.fpc.policy.Actions;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.tenants.tenant.fpc.policy.Descriptors;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.tenants.tenant.fpc.policy.Policies;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcbase.rev160803.FpcDpnControlProtocol;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcbase.rev160803.FpcIdentity;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcbase.rev160803.fpc.policy.Rules;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.fpc.rev150105.ZmqDpnControlProtocol;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author samstanley
 *
 */
public class BasePortManager extends PortManager implements AutoCloseable{
	private static final Logger LOG = LoggerFactory.getLogger(BasePolicyManager.class);
	
	private final TenantManager tenantMgr;
    private boolean initialized;
	 /**
     * Primary Constructor.
     * @param tenantMgr - Tenant Manager Assigned to the Activation Manager.
     * @param dataBroker - Data Broker
     */
    public BasePortManager(TenantManager tenantMgr,
            DataBroker dataBroker) {
        super();
        initialized = false;
        this.tenantMgr = tenantMgr;
        init(dataBroker, tenantMgr.getTenant());
    }

}
