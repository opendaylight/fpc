#!/bin/bash
JMETERDIR=/Applications/apache-jmeter-3.0/bin
PORT_BASE=1099
for i in `seq 1 $1`
do 
	echo $i
	echo $PORT_BASE
	let PORT_BASE+=1
done
