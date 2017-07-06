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
    "policies": [
       {
      "policy-id": "policy-182",
            "rules": [
                {
                    "order": "0",
                    "descriptors": [
                        {
                            "direction": "uplink",
                            "descriptor-id": "all-traffic-example"
                        }
                    ],
                    "actions": [
                        {
                            "order": "1",
                            "action-id": "copy-forward"
                        }
                    ]
                }
            ]
        }
    ]
}' \
http://localhost:8181/restconf/config/ietf-dmm-fpcagent:tenants/tenant/default/fpc-policy/policies/policy-182

echo ""
