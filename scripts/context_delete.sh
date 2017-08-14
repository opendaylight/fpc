#!/bin/bash
# ------------------------------------------------------------------
#  Copyright © 2016 - 2017 Copyright (c) Sprint, Inc. and others.  All rights reserved.
#
#  This program and the accompanying materials are made available under the
#  terms of the Eclipse Public License v1.0 which accompanies this distribution,
#  and is available at http://www.eclipse.org/legal/epl-v10.html
# ------------------------------------------------------------------

echo ""
curl -i -s \
--header "Content-type: application/json" \
--request POST \
-u admin:admin \
--data '{
    "input": {
        "op-id": "3",
        "targets": [
            {
                "target": "/ietf-dmm-fpcagent:tenants/tenant/default/fpc-mobility/contexts/202374885"
            }
        ],
        "client-id": "1",
        "session-state": "complete",
        "admin-state": "enabled",
        "op-type": "delete",
        "op-ref-scope": "none"
    }
}' \
http://localhost:8181/restconf/operations/ietf-dmm-fpcagent:configure
echo ""
