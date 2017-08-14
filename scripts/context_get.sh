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
--request POST \
-u admin:admin \
--data '{
    "input": {
        "op-id": "9",
        "targets": [
            {
                "target": "/ietf-dmm-fpcagent:tenants/tenant/default/fpc-mobility/contexts/imsi-9135551234"
            }
        ],
        "client-id": "1",
        "session-state": "complete",
        "admin-state": "enabled",
        "op-type": "query",
        "op-ref-scope": "storage"
    }
}' \
http://localhost:8181/restconf/operations/ietf-dmm-fpcagent:configure
echo ""
echo "Remember this GET will be visible on the HTTP Notifier ONLY!"
echo ""
