#!/usr/bin/python
#coding: utf8
#Copyright © 2016 - 2017 Copyright (c) Sprint, Inc. and others. All rights 
#reserved.
#
#This program and the accompanying materials are made available under the
#terms of the Eclipse Public License v1.0 which accompanies this distribution,
#and is available at http://www.eclipse.org/legal/epl-v10.html
#
############################################################################
# File : rules_test.py
#
# Comments : 
# Establish the channel with DP (Script(PUB) -> Forwarder -> DP(SUB)),
# And listen and push the message over socket.
############################################################################

import signal
import sys
import zmq
import struct
import socket as socketlib
import datetime
import time

from adc_rules import *
from pcc_rules import *
from mtr_rules import *
from sdf_rules import *

conflict = False
topicId = None

# TBD: Needs to handle exception
# TBD: Needs to handle keyboard intrrupts

#ZMQ ports
rec_port = "5560"
send_port = "5559"
# Socket to talk to server
context = zmq.Context()
socket = context.socket(zmq.SUB)
pub_socket = context.socket(zmq.PUB)
# As of not test script runs from FPC-SDN only
socket.connect ("tcp://localhost:%s" % rec_port)
pub_socket.connect("tcp://localhost:%s" % send_port)
topicfilter = ""
controller_topic= 252
socket.setsockopt(zmq.SUBSCRIBE, topicfilter)
print "Listening to port ", rec_port
print "Publisher on port ", send_port
print "Ready to receive messages. Press Ctrl+C when ready to exit."

for update_nbr in range(900000):
	# TBD: Needs to handle exception
	string = socket.recv()
	ts = time.time()
	st = datetime.datetime.fromtimestamp(ts).strftime('%Y-%m-%d %H:%M:%S')

	topic, msgnum, ID = struct.unpack('!BBB', string[:3])
	print"\n topic,msg,ID:%s,%s,%s" % (topic, msgnum, ID)

	#Listen to topic	
	if topic == 1 and msgnum == 10:#Assign_Id
		top, msg, topId = struct.unpack('!BBB', string[:3])
		print "\n topId :", topId
		topicId = topId

	#Listen to ack	
	if topic == 2 and  msgnum == 12:
		top, msg, topId_t = struct.unpack('!BBB', string[:3])
	
		if topicId == topId_t:
			# TBD: Needs to handle exception

			parse_adc_values(pub_socket, topicId)
			time.sleep(1)
			parse_mtr_values(pub_socket, topicId)
			time.sleep(1)
			parse_pcc_values(pub_socket, topicId)
			time.sleep(1)
			parse_sdf_values(pub_socket, topicId)
			time.sleep(1)
		socket.close()
		pub_socket.close()
		sys.exit(0)

