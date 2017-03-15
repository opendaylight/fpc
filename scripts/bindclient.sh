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
--request POST \
-u admin:admin \
--data '{
    "input": {
        "client-id": "1",
        "tenant-id": "default",
        "supported-features": [
            "urn:ietf:params:xml:ns:yang:fpcagent:fpc-bundles",
            "urn:ietf:params:xml:ns:yang:fpcagent:operation-ref-scope",
            "urn:ietf:params:xml:ns:yang:fpcagent:fpc-agent-assignments",
            "urn:ietf:params:xml:ns:yang:fpcagent:instruction-bitset"
        ],
        "endpoint-uri": "http://127.0.0.1:9997/"
    }
}' \
http://localhost:8181/restconf/operations/fpc:register_client

echo ""
