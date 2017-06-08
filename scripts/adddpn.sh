#!/bin/bash
# ------------------------------------------------------------------
#  Copyright © 2016 - 2017 Copyright (c) Sprint, Inc. and others.  All rights reserved.
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
--data '{
    "dpns": [
        {
            "dpn-id": "dpn1",
            "dpn-name": "site1-anchor1",
            "dpn-groups": [
                "foo"
            ],
            "node-id": "node1",
            "network-id": "network1"
        }
    ]
}' \
http://localhost:8181/restconf/config/ietf-dmm-fpcagent:tenants/tenant/default/fpc-topology/dpns/dpn1

echo ""
