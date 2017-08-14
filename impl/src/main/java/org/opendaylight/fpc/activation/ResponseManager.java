/*
 * Copyright © 2016 - 2017 Copyright (c) Sprint, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.fpc.activation;

import org.opendaylight.fpc.activation.cache.Cache;
import org.opendaylight.fpc.activation.cache.transaction.Transaction;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.payload.Contexts;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcbase.rev160803.targets.value.Targets;

/**
 * A base interface the supports the Changes and Deletions that are awaiting responses from the DPN
 * prior to moving to the next part of the transaction life cycle.
 */
public interface ResponseManager {
    /**
     * Enqueues a Change (Context based) that is awaiting a DPN response.
     *
     * @param context - Context Changed
     * @param cache - Cache associated with the Change operation.
     * @param tx - associated transaction.
     */
    public void enqueueChange(Contexts context, Cache cache, Transaction tx);

    /**
     * Enqueues a deletion (Target based) that is awaiting a DPN response.
     *
     * @param target - Target awaiting deletion
     * @param tx - associated transaction.
     */
    public void enqueueDelete(Targets target, Transaction tx);
}
