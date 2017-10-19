#!/bin/bash
# ------------------------------------------------------------------
#  Copyright © 2016 - 2017 Copyright (c) Sprint, Inc. and others.  All rights reserved.
#
#  This program and the accompanying materials are made available under the
#  terms of the Eclipse Public License v1.0 which accompanies this distribution,
#  and is available at http://www.eclipse.org/legal/epl-v10.html
# ------------------------------------------------------------------

if [ "$#" -ne 1 ]; then
    echo "Illegal number of parameters. Please specify the client-id you wish to unbind."
    exit 1
fi
echo ""
curl -i \
--header "Content-type: application/json" \
--request POST \
-u admin:admin \
--data '{
    "input": {
        "client-id": "'"$1"'"
    }
}' \
http://localhost:8181/restconf/operations/fpc:deregister_client

echo ""
