#!/bin/bash
# ------------------------------------------------------------------
#  Copyright © 2016 - 2017 Copyright (c) Sprint, Inc. and others.  All rights reserved.
#
#  This program and the accompanying materials are made available under the
#  terms of the Eclipse Public License v1.0 which accompanies this distribution,
#  and is available at http://www.eclipse.org/legal/epl-v10.html
# ------------------------------------------------------------------


dpnIdFile="./DpnId.txt"
while IFS='' read -r line || [[ -n "$line" ]];
do
    IFS='=' read -a myarray <<< "$line"
    if [ "${myarray[0]}" == "dpn-id" ]; then
        dpnId=${myarray[1]}
    fi
done < "$dpnIdFile"

echo ""
curl -i \
--header "Content-type: application/json" \
--request POST \
-u admin:admin \
--data "{
    'input': {
        'op-id': '1',
        'contexts': [
            {
                'instructions': {
                    'instr-3gpp-mob': 'session uplink'
                },
                'context-id': 202374885,
                'dpn-group': 'foo',
                'delegating-ip-prefixes': [
                    '192.168.1.5/32'
                ],
                'ul': {
                    'tunnel-local-address': '192.168.1.1',
                    'tunnel-remote-address': '10.1.1.1',
                    'mobility-tunnel-parameters': {
                        'tunnel-type': 'ietf-dmm-threegpp:gtpv1',
                        'tunnel-identifier': '1111'
                    },
                    'dpn-parameters': {}
                },
                'dl': {
                    'tunnel-local-address': '192.168.1.1',
                    'tunnel-remote-address': '10.1.1.1',
                    'mobility-tunnel-parameters': {
                        'tunnel-type': 'ietf-dmm-threegpp:gtpv1',
                        'tunnel-identifier': '2222'
                    },
                    'dpn-parameters': {}
                },
                'dpns': [
                    {
                        'dpn-id': $dpnId,
                        'direction': 'uplink',
                        'dpn-parameters': {}
                    }
                ],
                'imsi': '9135551234',
                'ebi': '5',
                'lbi': '5'
            }
        ],
        'client-id': '1',
        'session-state': 'complete',
        'admin-state': 'enabled',
        'op-type': 'create',
        'op-ref-scope': 'op'
    }
}" \
http://localhost:8181/restconf/operations/ietf-dmm-fpcagent:configure

echo ""
