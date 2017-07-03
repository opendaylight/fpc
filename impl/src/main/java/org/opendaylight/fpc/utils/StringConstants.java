/*
 * Copyright © 2016 - 2017 Copyright (c) Sprint, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.fpc.utils;

public class StringConstants {
	public static String bindClient =
			"{\n" +
			"    \"input\": {\n" +
			"        \"client-id\": \"0\",\n" +
			"        \"tenant-id\": \"default\",\n" +
			"        \"supported-features\": [\n" +
			"            \"urn:ietf:params:xml:ns:yang:fpcagent:fpc-bundles\",\n" +
			"            \"urn:ietf:params:xml:ns:yang:fpcagent:operation-ref-scope\",\n" +
			"            \"urn:ietf:params:xml:ns:yang:fpcagent:fpc-agent-assignments\",\n" +
			"            \"urn:ietf:params:xml:ns:yang:fpcagent:instruction-bitset\"\n" +
			"        ]\n" +
			"    }\n" +
			"}\n";

	public static String context =
			"{\n" +
			"   \"input\": {\n" +
			"        \"op-id\": \"0\",\n" +
			"        \"contexts\": [\n" +
			"            {\n" +
			"                \"instructions\": {\n" +
			"                    \"instr-3gpp-mob\": \"session uplink\"\n" +
			"                },\n" +
			"                \"context-id\": \"123\",\n" +
			"                \"dpn-group\": \"site1-l3\",\n" +
			"                \"delegating-ip-prefixes\": [\n" +
			"                    \"100.100.100.100/32\"\n" +
			"                ],\n" +
			"                \"ul\": {\n" +
			"                    \"tunnel-local-address\": \"10.0.0.1\",\n" +
			"                    \"tunnel-remote-address\": \"10.0.0.2\",\n" +
			"                    \"mobility-tunnel-parameters\": {\n" +
			"                        \"tunnel-type\": \"ietf-dmm-threegpp:gtpv1\",\n" +
			"                        \"tunnel-identifier\": \"5555\"\n" +
			"                    },\n" +
			"                    \"dpn-parameters\": {}\n" +
			"                },\n" +
			"                \"dl\": {\n" +
			"                    \"tunnel-local-address\": \"10.0.0.1\",\n" +
			"                    \"tunnel-remote-address\": \"10.0.0.2\",\n" +
			"                    \"mobility-tunnel-parameters\": {\n" +
			"                        \"tunnel-type\": \"ietf-dmm-threegpp:gtpv1\",\n" +
			"                        \"tunnel-identifier\": \"6666\"\n" +
			"                    },\n" +
			"                    \"dpn-parameters\": {}\n" +
			"                },\n" +
			"                \"dpns\": [\n" +
			"                    {\n" +
			"                        \"dpn-id\": \"0123\",\n" +
			"                        \"direction\": \"uplink\",\n" +
			"                        \"dpn-parameters\": {}\n" +
			"                    }\n" +
			"                ],\n" +
			"                \"imsi\": \"1234567890\",\n" +
			"                \"ebi\": \"5\",\n" +
			"                \"lbi\": \"5\"\n" +
			"            }\n" +
			"        ],\n" +
			"        \"client-id\": \"0\",\n" +
			"        \"session-state\": \"complete\",\n" +
			"        \"admin-state\": \"enabled\",\n" +
			"        \"op-type\": \"create\",\n" +
			"        \"op-ref-scope\": \"op\"\n" +
			"    }\n" +
			"}\n";

	public static String contextDelete =
			"{\n" +
			"   \"input\": {\n" +
			"        \"op-id\": \"1\",\n" +
			"        \"targets\": [\n" +
			"            {\n" +
			"                \"target\": \"/ietf-dmm-fpcagent:tenants/tenant/default/fpc-mobility/contexts/123\"\n" +
			"            }\n" +
			"        ],\n" +
			"        \"client-id\": \"0\",\n" +
			"        \"session-state\": \"complete\",\n" +
			"        \"admin-state\": \"enabled\",\n" +
			"        \"op-type\": \"delete\",\n" +
			"        \"op-ref-scope\": \"none\"\n" +
			"    }\n" +
			"}\n";

	public static String unbindClient =
			"{\n" +
			"	\"input\": {\n" +
			"		\"client-id\": \"0\"\n" +
			"	}\n" +
			"} \n";
}
