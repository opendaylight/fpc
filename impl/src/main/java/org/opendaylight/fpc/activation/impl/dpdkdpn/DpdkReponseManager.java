/*
 * Copyright © 2016 Copyright (c) Sprint, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.fpc.activation.impl.dpdkdpn;

import org.opendaylight.fpc.activation.ResponseManager;
import org.opendaylight.fpc.activation.cache.Cache;
import org.opendaylight.fpc.activation.cache.transaction.Transaction;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.payload.Contexts;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcbase.rev160803.targets.value.Targets;

/**
 * DPDK DPN Response Manager.
 */
public class DpdkReponseManager implements ResponseManager {

    @Override
    public void enqueueChange(Contexts context, Cache cache, Transaction tx) {
        //Just submit it as done.
        tx.complete(System.currentTimeMillis(), 1);
    }

    @Override
    public void enqueueDelete(Targets target, Transaction tx) {
        //Just submit it as done.
        tx.complete(System.currentTimeMillis(), 1);
    }
}
