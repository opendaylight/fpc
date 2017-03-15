/*
 * Copyright © 2016 Copyright (c) Sprint, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.fpc.activation.impl.dpdkdpn;

import java.util.ArrayList;
import java.util.List;

import org.opendaylight.fpc.activation.Activator;
import org.opendaylight.fpc.activation.ActivatorFactory;
import org.opendaylight.fpc.dpn.DpnHolder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.tenants.tenant.fpc.topology.Dpns;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcbase.rev160803.FpcDpnControlProtocol;

import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.fpc.rev150105.ZmqDpnControlProtocol;

/**
 * DPDKImpl Factory.
 */
public class DpdkImplFactory implements ActivatorFactory {
    static private List<Class<? extends FpcDpnControlProtocol>> controlProtocols;

    static {
        controlProtocols = new ArrayList<Class<? extends FpcDpnControlProtocol>>();
        controlProtocols.add(ZmqDpnControlProtocol.class);
    }

    /**
     * Default Constructor.
     */
    public DpdkImplFactory() {
    }

    @Override
    public Activator newInstance(DpnHolder dpnHolder) {
        return new DpdkImpl(dpnHolder);
    }

    @Override
    public Activator newInstance() {
        return new DpdkImpl();
    }

    @Override
    public List<Class<? extends FpcDpnControlProtocol>> getSupportedControlProtocols() {
        return controlProtocols;
    }
}
