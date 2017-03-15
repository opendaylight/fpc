#!/bin/bash
# ------------------------------------------------------------------
#  Copyright © 2016 Copyright (c) Sprint, Inc. and others.  All rights reserved.
#
#  This program and the accompanying materials are made available under the
#  terms of the Eclipse Public License v1.0 which accompanies this distribution,
#  and is available at http://www.eclipse.org/legal/epl-v10.html
# ------------------------------------------------------------------

if [ "$#" -ne 1 ]; then
    echo "Illegal number of parameters"
    exit 1
fi

arg="$1"
case "$arg" in
	get|put|delete)
		;;
        *)
		echo "Values must be one of: 'put', 'get' or 'delete'"
		exit 1
esac

echo ""
curl -i \
--header "Content-type: application/json" \
--request $1 \
-u admin:admin \
--data '{ 
    "descriptors": [
        {
            "descriptor-type": "ietf-dmm-fpcbase:threegpp-tft-decriptor",
            "packet-filters": [
                {
                    "direction": "bidirectional",
                    "identifier": "1",
                    "evaluation-precedence": "0",
		    "contents": [
                        {
                            "component-type-identifier": "64",
                            "local-port": "22"
                        }
                    ]
                }
            ],
            "descriptor-id": "threegPort22"
        }
    ]
}' \
http://localhost:8181/restconf/config/ietf-dmm-fpcagent:tenants/tenant/default/fpc-policy/descriptors/threegPort22


echo ""
