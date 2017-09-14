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
--header "Content-type: application/json" \
--request $1 \
-u admin:admin \
--data "{
    'input': {
        'ports': [
            {
                'port-id': '2345',
                'policy-groups': [
                    '3'
                ]
            }
        ],
        'op-id': '1',
        'client-id': '0',
        'session-state': 'complete',
        'admin-state': 'enabled',
        'op-type': 'create',
        'op-ref-scope': 'op'
    }
}" \
http://localhost:8181/restconf/operations/ietf-dmm-fpcagent:configure

echo ""
