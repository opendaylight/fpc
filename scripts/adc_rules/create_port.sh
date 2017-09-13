#!/bin/bash
# ------------------------------------------------------------------
#  Copyright © 2016 Copyright (c) Sprint, Inc. and others.  All rights reserved.
#
#  This program and the accompanying materials are made available under the
#  terms of the Eclipse Public License v1.0 which accompanies this distribution,
#  and is available at http://www.eclipse.org/legal/epl-v10.html
# ------------------------------------------------------------------


# 13.1.1.111 Sprint Internet Zero-Rat
# 13.1.1.114 Google Internet Zero-Rate
# 13.1.1.112/31 Sprint Management Zero-Rate
# sprint.com Sprint Internet Zero-Rate
# www.fcc.gov Sprint Internet Zero-Rate
# csd-01.sprinspectrum.com Sprint Internet Zero-Rate
# ns.sprintlabs.com Sprint-Labs Internet Zero-Rate
# 13.1.1.121 Sprint Provisioning Zero-Rate
# 13.1.1.122 Sprint Provisioning Zero-Rate
# 13.1.1.124/30 Intel      Management       Fake-Rate

./prefix-descriptor.sh put 1 13.1.1.111/32;
./rate-action.sh put 1 Sprint 1 0;
./policy.sh put 1 1 1;

./prefix-descriptor.sh put 2 13.1.1.114/32;
./rate-action.sh put 2 Google 1 0;
./policy.sh put 2 2 2;

./prefix-descriptor.sh put 3 13.1.1.112/31;
./rate-action.sh put 3 Sprint 2 0;
./policy.sh put 3 3 3;

./domain-descriptor.sh put 4 sprint.com;
./rate-action.sh put 4 Sprint 1 0;
./policy.sh put 4 4 4;

./domain-descriptor.sh put 5 www.fcc.gov;
./rate-action.sh put 5 Sprint 1 0;
./policy.sh put 5 5 5;

./domain-descriptor.sh put 6 csd-01.sprinspectrum.com;
./rate-action.sh put 6 Sprint 1 0;
./policy.sh put 6 6 6;

./domain-descriptor.sh put 7 ns.sprintlabs.com;
./rate-action.sh put 7 Sprint-Labs 1 0;
./policy.sh put 7 7 7;

./prefix-descriptor.sh put 8 13.1.1.121/32;
./rate-action.sh put 8 Sprint 3 0;
./policy.sh put 8 8 8;

echo "policies created";
sleep 3;

./policy-group.sh put 1 2 3 4 5 6 7 8;
echo "policy-group created"
sleep 1;

./port.sh post;
echo "port created (all done)";

