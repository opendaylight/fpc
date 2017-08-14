#!/bin/sh
# ------------------------------------------------------------------
#  Copyright © 2016 - 2017 Copyright (c) Sprint, Inc. and others.  All rights reserved.
#
#  This program and the accompanying materials are made available under the
#  terms of the Eclipse Public License v1.0 which accompanies this distribution,
#  and is available at http://www.eclipse.org/legal/epl-v10.html
# ------------------------------------------------------------------

trap ctrl_c INT

function ctrl_c() {
	echo ""
}

if [[ $1 ]]
then
	PORT=$1
else
	PORT=9997
fi

PROCESS=$(lsof -i :${PORT} | awk 'NR!=1 {print $2}' )
if [[ $PROCESS ]]
then
	echo 'Process with PID' $PROCESS 'is using port' $PORT'. Could not start http server on this port.'
	exit
fi

python server.py 127.0.0.1 $PORT > ../../karaf/target/assembly/data/log/http_server_$PORT.log 2>&1 & httpID=$!

python forwarder_subscriber.py > ../../karaf/target/assembly/data/log/forwarder_subscriber.log 2>&1 & subID=$!


tail -f ../../karaf/target/assembly/data/log/forwarder_subscriber.log -f ../../karaf/target/assembly/data/log/http_server_$PORT.log

kill -9 $httpID
kill -9 $subID