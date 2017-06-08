

------------------------------------------------------------------
Copyright © 2016 - 2017 Copyright (c) Sprint, Inc. and others.  All rights reserved.

This program and the accompanying materials are made available under the
terms of the Eclipse Public License v1.0 which accompanies this distribution,
and is available at http://www.eclipse.org/legal/epl-v10.html
------------------------------------------------------------------

# FPC Agent Scripts
This directory contains scripts to execute against the FPC Agent and simulate the DPN. 

You can execute the following general scripts at any time. 

* agent-info.sh provides agent information. 

* getstats.sh shows the transactions and state entry counts. 



## Run a test script
To run any of the remaining test scripts (not including agent-info.sh and getstats.sh), perform the following steps in order.

1. Run the bindclient.sh script to map the client ID used by other scripts to bind the default tenant. You must execute this script before running any of the scripts in this directory, except agent-info.sh or getstats.sh.
2. Start the [python listener] (..fpc/zmqforwarder/forwarder_subscriber_with_ACK.py).

3. Verify the addition of the DPN by looking at the result of get-topology.sh.
4. Edit the DpnId.txt file to make sure the dpn-id is set to the DPN you want to use. Change the DPN Id as needed and save the file.
5. Run a test script from the FPC Agent scripts directory.

In addition, note that you can manipulate contexts in order, as shown in the following example. 
1. context_create.sh
2. context_update.sh
3. context_delete.sh

To use context_get.sh correctly:
1. Make sure that the HTTP Notifier is configured [refer to the Agent Configuration parameters in the impl-blueprint.xml file] (..fpc/impl/src/main/resources/org/opendaylight/blueprint/impl-blueprint.xml). 
2. Make sure the HTTP notification server is running. For instructions on starting the northbound HTTP notification server, refer to the README.md file on the root of this repo. 

## Cleanup after testing
Run unbindclient.sh after testing to free the client identity mapping.
