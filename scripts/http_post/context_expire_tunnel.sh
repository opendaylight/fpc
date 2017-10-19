#!/bin/bash
# ------------------------------------------------------------------
#  Copyright © 2016 - 2017 Copyright (c) Sprint, Inc. and others.  All rights reserved.
#
#  This program and the accompanying materials are made available under the
#  terms of the Eclipse Public License v1.0 which accompanies this distribution,
#  and is available at http://www.eclipse.org/legal/epl-v10.html
# ------------------------------------------------------------------

dpnIdFile="./../DpnIdtxt"
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
        'op-id': '2',
        'contexts': [
            {
                'instructions': {
                    'instr-3gpp-mob': 'downlink'
                },
                'context-id': 'imsi-9135551234',
                'dl': {
		    'lifetime' : '0',
                    'mobility-tunnel-parameters': {
                        'tunnel-type': 'ietf-dmm-threegpp:gtpv1',
                        'tunnel-identifier': '4444'
                    }
                },
                'dpns': [
                    {
                        'dpn-id': $dpnId,
                        'direction': 'downlink'
                    }
                ]
            }
        ],
        'client-id': '1',
        'session-state': 'complete',
        'admin-state': 'enabled',
        'op-type': 'update',
        'op-ref-scope': 'op'
    }
}" \
http://localhost:8181/restconf/operations/ietf-dmm-fpcagent:configure

echo ""
