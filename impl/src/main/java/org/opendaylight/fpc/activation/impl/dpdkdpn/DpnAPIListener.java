/*
 * Copyright © 2016 Copyright (c) Sprint, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.fpc.activation.impl.dpdkdpn;

import java.math.BigInteger;
import java.util.AbstractMap;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.opendaylight.fpc.dpn.DPNStatusIndication;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.notify.value.DownlinkDataNotification;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.notify.value.DownlinkDataNotificationBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcbase.rev160803.FpcDpnId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.threegpp.rev160803.EbiType;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.threegpp.rev160803.ImsiType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * DPN Listener Interface for DPN initiated messages.
 */
public class DpnAPIListener {
    protected static final Logger LOG = LoggerFactory.getLogger(DpnAPIListener.class);
    private static byte DPN_HELLO = 0b0000_0001;
    private static byte DPN_BYE = 0b0000_0010;
    private static byte DOWNLINK_DATA_NOTIFICATION = 0b0000_0011;
    private static byte DPN_OVERLOAD_INDICATION = 0b0000_0100;

    public static String BROADCAST_TOPIC = "0";

    static private Map<Short, FpcDpnId> uplinkDpnMap = new HashMap<Short, FpcDpnId>();

    /**
     * Sets the mapping of a ZeroMQ topics to DPN Identities.
     * @param topic - ZeroMQ Topic
     * @param dpnId - DPN Identifier
     */
    static public void setUlDpnMapping(Short topic, FpcDpnId dpnId) {
        LOG.info("Adding DPN Mapping {} => {}", topic, dpnId);
        uplinkDpnMap.put(topic, dpnId);
    }

    /**
     * Removes a mapping of a ZeroMQ topic if it exists.
     * @param topic - ZeroMQ Topic
     */
    static public void removeUlDpnMapping(Short topic) {
        LOG.info("Removing DPN Mapping {}", topic);
        uplinkDpnMap.remove(topic);
    }

    /**
     * Gets the mapping for a ZeroMQ Topic
     * @param topic - ZeroMQ Topic
     * @return FpcDpnId mapped to the ZeroMQ Topic or null if a mapping is not present
     */
    static public FpcDpnId getMapping(Short topic) {
        return uplinkDpnMap.get(topic);
    }

    /**
     * Decodes a DPN message.
     * @param buf - message buffer
     * @return - A pair with the DPN Id and decoded Object
     */
    public Map.Entry<FpcDpnId, Object> decode(byte[] buf) {
        byte[] sender = new byte[1];
        if (buf[0] == DOWNLINK_DATA_NOTIFICATION) {
            return new AbstractMap.SimpleEntry<FpcDpnId, Object>(uplinkDpnMap.get(buf[1]), processDDN(buf));
        } else {
            DPNStatusIndication.Status status = null;
            sender[0] = buf[1];
            if (buf[0] ==  DPN_OVERLOAD_INDICATION) {
                status = DPNStatusIndication.Status.OVERLOAD_INDICATION;
            } else if (buf[0] ==  DPN_HELLO) {
                status = DPNStatusIndication.Status.HELLO;
            } else if (buf[0] ==  DPN_BYE) {
                status = DPNStatusIndication.Status.BYE;
            }
            return new AbstractMap.SimpleEntry<FpcDpnId, Object>(uplinkDpnMap.get(buf[1]), (uplinkDpnMap.containsKey(buf[1])) ?
                 new DPNStatusIndication(status, uplinkDpnMap.get(buf[1])) :
                 new DPNStatusIndication(status, sender));
        }
    }

    // Message format
    // [ Message - 1 Byte ] [ Source DPN - 1 Byte ] [ IMSI - 8 bytes  ]
    // [ EBI - 1 byte ] [ DSCP - 1 Byte ] [ Sender F-TEID - 4 bytes ]
    /**
     * Decodes a DownlinkDataNotification
     * @param buf - message buffer
     * @return DownlinkDataNotification or null if it could not be successfully decoded
     */
    public DownlinkDataNotification processDDN(byte[] buf) {
        DownlinkDataNotificationBuilder ddnB = new DownlinkDataNotificationBuilder();
        if (uplinkDpnMap.get(new Short(buf[1])) != null) {
            ddnB.setDpnId(uplinkDpnMap.get(new Short(buf[1])));
        }
        return ddnB.setImsi(new ImsiType(toBigInt(buf, 2)))
            .setEbi(new EbiType(toShort(buf,6)))
            .setDscp(toShort(buf,7))
            .setSenderFteid((long)toInt(buf,8))
            .build();
    }

    /**
     * Converts a byte array to BigInteger
     * @param source - byte array
     * @param offset - offset in the array where the 8 bytes begins
     * @return BigInteger representing a Uint64
     */
    public BigInteger toBigInt(byte[] source, int offset) {
        return new BigInteger(Arrays.copyOfRange(source, offset, offset+8));
    }

    /**
     * Decodes a Long value
     * @param source - byte array
     * @param offset - offset in the array where the 8 bytes begins
     * @return BigInteger representing a Uint64
     */
    public long toLong(byte[] source, int offset) {
        return new BigInteger(Arrays.copyOfRange(source, offset, offset+8)).longValue();
    }

    /**
     * Decodes a 32 bit value
     * @param source - byte array
     * @param offset - offset in the array where the 8 bytes begins
     * @return integer
     */
    public int toInt(byte[] source, int offset) {
        return new BigInteger(Arrays.copyOfRange(source, offset, offset+4)).intValue();
    }

    /**
     * Decodes a Short (byte) value
     * @param source - byte array
     * @param offset - offset in the array where the 8 bytes begins
     * @return Short
     */
    public short toShort(byte[] source, int offset) {
        return (short) source[offset];
    }
}
