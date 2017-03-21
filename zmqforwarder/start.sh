#!/bin/sh
# ------------------------------------------------------------------
#  Copyright © 2016 Copyright (c) Sprint, Inc. and others.  All rights reserved.
#
#  This program and the accompanying materials are made available under the
#  terms of the Eclipse Public License v1.0 which accompanies this distribution,
#  and is available at http://www.eclipse.org/legal/epl-v10.html
# ------------------------------------------------------------------

PORT_1=5559
PROCESS_1=$(lsof -i :${PORT_1} | awk 'NR!=1 {print $2}' )

PORT_2=5560
PROCESS_2=$(lsof -i :${PORT_2} | awk 'NR!=1 {print $2}' )

if [[ $PROCESS_1 ]]
then
	echo 'Process with PID '$PROCESS_1 'is using port' $PORT_1'. Please free up this port and run this script again to start the forwarder_device.'
fi

if [[ $PROCESS_2 ]]
then
	echo 'Process with PID '$PROCESS_2 'is using port' $PORT_2'. Please free up this port and run this script again to start the forwarder_device.'
fi
if [[ $PROCESS_1 ]] || [[ $PROCESS_2 ]]
then
	exit
fi

if [ ! -d ../karaf/target/assembly/data/log ] 
then 
   mkdir ../karaf/target/assembly/data/log
fi 

if [ -e forwarder_pid.txt ]
then
	echo "a pid file is already present. please stop the server first"
else
	python forwarder_device.py &> ../karaf/target/assembly/data/log/fowarder_device.log & echo $! > forwarder_pid.txt
fi
