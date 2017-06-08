/*
 * Copyright © 2016 - 2017 Copyright (c) Sprint, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.fpc.assignment;

import java.math.BigInteger;
import java.net.InetAddress;
import java.net.UnknownHostException;

import org.opendaylight.fpc.utils.IPToDecimal;
import org.opendaylight.fpc.utils.Counter;
import org.opendaylight.fpc.utils.ErrorLog;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Prefix;

/**
 * Manages an IPv4 Pool (Range).
 */
public class IPv4RangeManager extends Counter {
    /**
     * Constructs an IPv4 Pool Manager
     * @param base - IPPrefix used for the basis of the Pool Manager
     * @return An IPv4RangeManager configured to allocate addresses based upon the provided base
     */
    public static IPv4RangeManager createIPv4RangeManager(IpPrefix base) {
        int baseVal = (int) IPToDecimal.cidrBase(base.getIpv4Prefix().toString());
        int upperVal = (int) (baseVal + IPToDecimal.getSize(base.getIpv4Prefix().toString()));
        return new IPv4RangeManager(baseVal, upperVal);
    }

    /**
     * Default Constructor
     * @param lo - Range lower bound
     * @param hi - Range upper bound
     */
    protected IPv4RangeManager(int lo, int hi) {
        super(lo,hi);
    }

    /**
     * Acquires the next available IP address from the pool
     * @return IpPrefix of the next avaiable IP address or null if there are no more addresses.
     */
    public IpPrefix getNextAddrAsPrefix() {
        try {
            byte[] bytes = BigInteger.valueOf(nextValue()).toByteArray();
            InetAddress address = InetAddress.getByAddress(bytes);
            Ipv4Prefix ipv4Pre = new Ipv4Prefix(address.getHostAddress());
            return new IpPrefix(ipv4Pre);
        } catch (IndexOutOfBoundsException outOfRangeExc) {
            // This implies the Address pool has been exhausted
        } catch (UnknownHostException e) {
            ErrorLog.logError("A value was produced that gennerated a UnknownHostException", e.getStackTrace());
        }
        return null;
    }
}
