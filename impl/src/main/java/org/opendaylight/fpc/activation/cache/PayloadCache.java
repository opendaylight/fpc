/*
 * Copyright © 2016 - 2017 Copyright (c) Sprint, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.fpc.activation.cache;

import java.util.List;

import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.payload.Contexts;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.payload.Ports;

/**
 * Provides the Payload associated values in the Cache.
 *
 * A cache MAY contain values developed during the Assignment Phase but are not a part of the Payload's original data, e.g.
 * Clones may be in the Cache.  This interface provides mechanisms for retreiving the original value.
 *
 */
public interface PayloadCache extends Cache {
    /**
     * Return the Ports from the original Operation.
     * @return A list of Ports that were present in the original Operation.
     */
    public List<Ports> getPayloadPorts();

    /**
     * Return the Contexts from the original Operation.
     * @return A list of Context that were present in the original Operation.
     */
    public List<Contexts> getPayloadContexts();
}
