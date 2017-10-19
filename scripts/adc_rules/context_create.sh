#!/bin/bash
# ------------------------------------------------------------------
#  Copyright © 2016 - 2017 Copyright (c) Sprint, Inc. and others.  All rights reserved.
#
#  This program and the accompanying materials are made available under the
#  terms of the Eclipse Public License v1.0 which accompanies this distribution,
#  and is available at http://www.eclipse.org/legal/epl-v10.html
# ------------------------------------------------------------------


dpnIdFile="./../DpnId.txt"
while IFS='' read -r line || [[ -n "$line" ]];
do
    IFS='=' read -a myarray <<< "$line"
    if [ "${myarray[0]}" == "dpn-id" ]; then
        dpnId=${myarray[1]}
    fi
done < "$dpnIdFile"

echo ""
curl -i \
--header "Expect:" \
--header "Content-type: application/json" \
--request POST \
-u admin:admin \
--data "{
    'input': {
        'op-id': '2',
        'contexts': [
            {
                'context-id': "adc-context",
                'ul': {
                    'dpn-parameters': {}
                },
                'dl': {
                    'dpn-parameters': {}
                },
		        'ports': [
			        '2345'
		        ],
                'dpns': [
                    {
                        'dpn-id': $dpnId,
                        'direction': 'uplink',
                        'dpn-parameters': {}
                    }
                ]
            }
        ],
        'client-id': '0',
        'op-type': 'create'
    }
}" \
http://localhost:8181/restconf/operations/ietf-dmm-fpcagent:configure

echo ""
