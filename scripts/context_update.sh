#!/bin/bash
# ------------------------------------------------------------------
#  Copyright © 2016 Copyright (c) Sprint, Inc. and others.  All rights reserved.
#
#  This program and the accompanying materials are made available under the
#  terms of the Eclipse Public License v1.0 which accompanies this distribution,
#  and is available at http://www.eclipse.org/legal/epl-v10.html
# ------------------------------------------------------------------

echo ""
curl -i \
--header 'Expect:' \
--header "Content-type: application/json" \
--request POST \
-u admin:admin \
--data '{
    "input": {
        "op-id": "2",
        "contexts": [
            {
                "instructions": {
                    "instr-3gpp-mob": "downlink"
                },
                "context-id": "imsi-9135551234",
                "dpn-group": "site1-l3",
                "delegating-ip-prefixes": [
                    "192.168.1.5/32"
                ],
                "ul": {
                    "tunnel-local-address": "192.168.1.1",
                    "tunnel-remote-address": "10.1.1.2",
                    "mobility-tunnel-parameters": {
                        "tunnel-type": "ietf-dmm-threegpp:gtpv1",
                        "tunnel-identifier": "3333"
                    },
                    "dpn-parameters": {}
                },
                "dl": {
                    "tunnel-local-address": "192.168.1.1",
                    "tunnel-remote-address": "10.1.1.4",
                    "mobility-tunnel-parameters": {
                        "tunnel-type": "ietf-dmm-threegpp:gtpv1",
                        "tunnel-identifier": "4444"
                    },
                    "dpn-parameters": {}
                },
                "dpns": [
                    {
                        "dpn-id": "vdpn1",
                        "direction": "uplink",
                        "dpn-parameters": {}
                    }
                ],
                "imsi": "9135551234",
                "ebi": "5",
                "lbi": "5"
            }
        ],
        "client-id": "1",
        "session-state": "complete",
        "admin-state": "enabled",
        "op-type": "update",
        "op-ref-scope": "op"
    }
}' \
http://localhost:8181/restconf/operations/ietf-dmm-fpcagent:configure

echo ""
