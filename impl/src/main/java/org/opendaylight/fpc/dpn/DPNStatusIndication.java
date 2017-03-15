/*
 * Copyright © 2016 Copyright (c) Sprint, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.fpc.dpn;

import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcbase.rev160803.FpcDpnId;

// TODO - Add the subtypes for Overload Indication and Control
/**
 * Provides basic status changes,
 */
public class DPNStatusIndication {
    static public final FpcDpnId UNKNONW = new FpcDpnId("UKASSIGNED");

    /**
     * Basic DPN Status
     */
    public enum Status {
        HELLO,
        BYE,
        OVERLOAD_INDICATION
    }

    private final Status status;
    private final FpcDpnId dpnId;
    public byte[] rawId;

    /**
     * Constructor providing the DPN and its associated Status.
     * @param status - DPN Status
     * @param id - DPN Identity
     */
    public DPNStatusIndication (Status status,
                                FpcDpnId id) {
        this.status = status;
        this.dpnId = id;
    }

    /**
     * Constructor providing the DPN and its associated Status.
     * @param status - DPN Status
     * @param rawId - DPN Identity (as raw bytes)
     */
    public DPNStatusIndication (Status status,
                                byte[] rawId) {
        this.status = status;
        this.dpnId = UNKNONW;
        this.rawId = rawId;
    }

    /**
     * Provides DPN Status
     * @return Status associated to the DPN.
     */
    public Status getStatus() {
        return status;
    }

    /**
     * Provides the DPN id
     * @return FpcDpnId
     */
    public FpcDpnId getId() {
        return dpnId;
    }

    /**
     * Provides the DPN id
     * @return byte array representing the DPN Id
     */
    public byte[] getRawId() {
        return rawId;
    }
}
