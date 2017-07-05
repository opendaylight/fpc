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
        "abstract-dpn-id": "'"$1"'",
        "dpn-id": "'"$2"'",
        "operation": "'"$3"'"
    }
}' \
http://localhost:8181/restconf/operations/ietf-dmm-fpcagent:configure-dpn

echo ""
