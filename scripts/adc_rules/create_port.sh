#!/bin/bash
# ------------------------------------------------------------------
#  Copyright © 2016 Copyright (c) Sprint, Inc. and others.  All rights reserved.
#
#  This program and the accompanying materials are made available under the
#  terms of the Eclipse Public License v1.0 which accompanies this distribution,
#  and is available at http://www.eclipse.org/legal/epl-v10.html
# ------------------------------------------------------------------


./../bindclient.sh 0;
sleep 1;

# 13.1.1.111 Sprint Internet Zero-Rat
# 13.1.1.114 Google Internet Zero-Rate
# 13.1.1.112/31 Sprint Management Zero-Rate -- DROP
# sprint.com Sprint Internet Zero-Rate  -- DROP

# creates a descriptor using an ip (prefix-descriptor)
# params: http method, descriptor-id, ip address/prefix
./prefix-descriptor.sh put 1 13.1.1.111/32;

# creates an action for rating (rate-action)
# params: http method, action-id, sponsor-id, service-id, rating-group
./rate-action.sh put 1 Sprint 1 0;

# creates a policy holding a certain rule (descriptor/action)
# params: http method, policy-id, action-id, descriptor-id
./policy.sh put 1 1 1;

./prefix-descriptor.sh put 2 13.1.1.114/32;
./rate-action.sh put 2 Google 1 0;
./policy.sh put 2 2 2;

./prefix-descriptor.sh put 3 13.1.1.112/31;
# same as rate-action but is used for dropping packets
./drop-action.sh put 3 Sprint 2 0;
./policy.sh put 3 3 3;

# same as prefix-descriptor but uses a domain
./domain-descriptor.sh put 4 sprint.com;
./drop-action.sh put 4 Sprint 1 0;
./policy.sh put 4 4 4;

echo "policies created";
sleep 1;

# creates a policy group storing multiple policies
# params: http method, policy-id x4
./policy-group.sh put 1 2 3 4;
echo "policy-group created"
sleep 1;

# creates a port that stores policy groups
# params: http method
./port.sh post;
echo "port created";
sleep 1;

./context_create.sh;
echo "context created (all done)"
# cd ..;
# sleep 3;
# ./unbindclient.sh 0;
# ./bindclient.sh 1;
# ./context_create.sh

