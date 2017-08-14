/*
 * Copyright © 2016 - 2017 Copyright (c) Sprint, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.fpc.utils;

/**
 * Helper to Interchange Strings and Longs that are IPv4 addresses.
 */
public class IPToDecimal {
	
	/**
	 * Default Constructor.
	 */
    public IPToDecimal() {
    }

    /**
     * Converts IPv4 string to a long
     * @param ipAddress - IPv4 String
     * @return long value of ipv4 address
     */
    public static long ipv4ToLong(String ipAddress) {
        long result = 0;

        String[] ipAddressInArray = ipAddress.split("\\.");

        for (int i = 3; i >= 0; i--) {

            long ip = Long.parseLong(ipAddressInArray[3 - i]);

            // left shifting 24,16,8,0 and bitwise OR

            // 1. 192 << 24
            // 1. 168 << 16
            // 1. 1 << 8
            // 1. 2 << 0
            result |= ip << (i * 8);
        }

        return result;
    }

    /**
     * Converts a long value to an IPv4 string.
     * @param i - long
     * @return IPv4 address string
     */
    public static String longToIpv4(long i) {
        return ((i >> 24) & 0xFF) + "." + ((i >> 16) & 0xFF) + "." + ((i >> 8) & 0xFF) + "." + (i & 0xFF);
    }

    /**
     * Determines the CIDR base
     * @param cidr - CIDR entry 
     * @return long value representing the CIDR base
     */
    public static long cidrBase(String cidr)  {
          String[] parts = cidr.split("/");
        long result = 0;
        int prefix = (parts.length < 2) ? 0 : Integer.parseInt(parts[1]);
        long netmask = 0x0FFFFFFFFL << (32 - prefix);

        String[] ipAddressInArray = parts[0].split("\\.");
        for (int i = 3; i >= 0; i--) {
            long ip = Long.parseLong(ipAddressInArray[3 - i]);
            result |= ip << (i * 8);
        }

        return result &= netmask;
    }

    /**
     * Determines the size (number of addresse/entries) in the CIDR
     * @param cidr - CIDR
     * @return number of addresses contained in the CIDR value
     */
    public static long getSize(String cidr) {
          String[] parts = cidr.split("/");
          int prefix = (parts.length < 2) ? 0 : Integer.parseInt(parts[1]);
        return (long) Math.pow(2, 32 - prefix);
    }
}
