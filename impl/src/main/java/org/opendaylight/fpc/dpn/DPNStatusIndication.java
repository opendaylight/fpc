/*
 * Copyright © 2016 - 2017 Copyright (c) Sprint, Inc. and others.  All rights reserved.
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
    /**
     * DpcDpnId with value UNASSIGNED
     */
    static public final FpcDpnId UNKNONW = new FpcDpnId("UNASSIGNED");

    /**
     * Basic DPN Status
     */
    public enum Status {
        /**
         * DPN HELLO
         */
        HELLO,
        /**
         * DPN GOODBYE
         */
        BYE,
        /**
         * DPN OVERLOAD INDICATION
         */
        OVERLOAD_INDICATION
    }

    private final Status status;
    private final String key; //nodeId +"/"+ networkId
    /**
     * Node Reference of the DPN
     */
    public Short nodeRef;

    /**
     * Constructor providing the DPN and its associated Status.
     * @param status - DPN Status
     * @param key - Combination of node id and network id
     */
    public DPNStatusIndication (Status status,
                                String key) {
        this.status = status;
        this.key = key;
    }


    /**
     * Provides DPN Status
     * @return Status associated to the DPN.
     */
    public Status getStatus() {
        return status;
    }

    /**
     * Provides the DPN key - nodeId +"/"+ networkId
     * @return FpcDpnId
     */
    public String getKey() {
        return this.key;
    }
}
