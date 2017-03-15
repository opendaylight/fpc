/*
 * Copyright © 2016 Copyright (c) Sprint, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.fpc.activation;

import java.util.List;

import org.opendaylight.fpc.dpn.DpnHolder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcbase.rev160803.FpcDpnControlProtocol;

/**
 * Simple interface for Activator factories.
 */
public interface ActivatorFactory {
    /**
     * Creates a new Activator instance.
     * @param dpnHolder - DPN configuration to be applied to the Activator
     * @return - An Activator with the dpn assigned to it.
     */
    public Activator newInstance(DpnHolder dpnHolder);

    /**
     * Creates a new Activator instance.
     * @return - An Activator with no dpn assigned.
     */
    public Activator newInstance();

    /**
     * Returns the DPN Control Protocols supported by this factory.
     * @return - A list of DPN Control Protocols
     */
    public List<Class<? extends FpcDpnControlProtocol>> getSupportedControlProtocols();

    /**
     * Registers this factory to the Activation Manager <b>IF</b> for the DPN protocols supported
     * by the factory.
     *
     * @param am - Activation Manager to which the factory must register
     */
    default public void register(ActivationManager am) {
        if (getSupportedControlProtocols() == null) return;
        for (Class<? extends FpcDpnControlProtocol> proto : getSupportedControlProtocols()) {
            am.addActivatorFactory(proto, this);
        }
    }

    /**
     * De-registers this factory from the Activation Manager.
     *
     * @param am - Activation Manager to which the factory must de-register
     */
    default public void deRegister(ActivationManager am) {
        if (getSupportedControlProtocols() == null) return;
        for (Class<? extends FpcDpnControlProtocol> proto : getSupportedControlProtocols()) {
            if (am.getActivatorFactoriess().get(proto) == this)
                am.removeActivatorFactory(proto);
        }
    }
}
