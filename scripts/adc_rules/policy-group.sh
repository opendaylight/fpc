#!/bin/bash
# ------------------------------------------------------------------
#  Copyright © 2016 Copyright (c) Sprint, Inc. and others.  All rights reserved.
#
#  This program and the accompanying materials are made available under the
#  terms of the Eclipse Public License v1.0 which accompanies this distribution,
#  and is available at http://www.eclipse.org/legal/epl-v10.html
# ------------------------------------------------------------------

if [ "$#" -ne 5 ]; then
    echo "Params incorrect, refer to the following template:"
    echo "./policy-group.sh request policyid(x4)"
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
curl -i -s \
--header "Content-type: application/json" \
--request $1 \
-u admin:admin \
--data '{
    "policy-groups": [
        {
            "policy-group-id": "3",
            "policies": [
                "'"$2"'",
                "'"$3"'",
                "'"$4"'",
                "'"$5"'"
            ]
        }
    ]
}' \
http://localhost:8181/restconf/config/ietf-dmm-fpcagent:tenants/tenant/default/fpc-policy/policy-groups/3

echo ""
