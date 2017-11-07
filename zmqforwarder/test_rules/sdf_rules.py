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
# File : sdf_rules.py
#
# Comments :
# Read SDF Rules config file from './config/' and get parameters
# values as per the SDF Rule Table.
# As per SDF Rule table, formed a structure and parse values in structure,
# and finally pack a structure and send over the zmq socket to DP.
#
# Reference : message_sdn_dp.docx
# Section : Table No.14 SDF Rule
############################################################################

import sys
import os
import time
import struct
import socket

from netaddr import IPAddress
from configparser import ConfigParser

parser = ConfigParser()

def parse_sdf_values(pub_socket,topicId):
	# TBD: Need to handle exception
	parser.read('./config/sdf_rules.cfg')
	print "\n ---> Reading Values from SDF rules file <--- "
	print "\n ---> Sending SDF Rules <---"

	MSG_TYPE = 20
	# Initilize the parameters
	# Rule type set 0 if Rule String and 1 Five tuple
	RULE_TYPE = 0
	PCC_RULE_ID = 0
	DIRECTION = 'bidirectional'
	LOCAL_IP = '0.0.0.0'
	LOCAL_IP_MASK = 0
	IPV4_REMOTE = '0.0.0.0'
	IPV4_REMOTE_MASK = 0
	LOCAL_LOW_LIMIT_PORT = 0
	LOCAL_HIGH_LIMIT_PORT = 65535
	REMOTE_LOW_LIMIT_PORT = 0
	REMOTE_HIGH_LIMIT_PORT = 65535
	PRECEDENCE = 0
	PROTOCOL = hex(0)
	PROTOCOL_MASK = hex(0)

	# Create the structure for SDF rule and parse the values in that.
	for val in parser.sections():
		if val != 'GLOBAL':
			# TBD: Need to handle exception
			PCC_RULE_ID += 1
			if PCC_RULE_ID > 1:
				PROTOCOL_MASK = '0xff'

			if parser.has_option(val, 'RULE_TYPE'):
				RULE_TYPE = int(parser.get(val, \
						'RULE_TYPE'))

			if parser.has_option(val, 'RATING_GROUP'):
				RATING_GROUP = int(parser.get(val, \
						'RATING_GROUP'))

			if parser.has_option(val, 'DIRECTION'):
				DIRECTION = str(parser.get(val, \
						'DIRECTION'))

			if parser.has_option(val, 'PRECEDENCE'):
				PRECEDENCE = int(parser.get(val, \
						'PRECEDENCE'))

			if parser.has_option(val, 'LOCAL_IP'):
				LOCAL_IP = str(parser.get(val, \
						'LOCAL_IP'))

			if parser.has_option(val, 'LOCAL_IP_MASK'):
				LOCAL_IP_MASK = str(parser.get(val, \
						'LOCAL_IP_MASK'))

			if parser.has_option(val, 'IPV4_REMOTE'):
				IPV4_REMOTE = str(parser.get(val, \
						'IPV4_REMOTE'))

			if parser.has_option(val, 'IPV4_REMOTE_MASK'):
				IPV4_REMOTE_MASK = \
				IPAddress(struct.unpack('!L', \
					socket.inet_aton(\
					str(parser.get(val, \
					'IPV4_REMOTE_MASK'))))\
					[0]).netmask_bits()

			if parser.has_option(val, 'PROTOCOL'):
				PROTOCOL = hex(int(parser.get(val, \
						'PROTOCOL')))

			if parser.has_option(val, 'PROTOCOL_MASK'):
				PROTOCOL_MASK = int(parser.get(val, \
						'PROTOCOL_MASK'))

			if parser.has_option(val, 'LOCAL_LOW_LIMIT_PORT'):
				LOCAL_LOW_LIMIT_PORT = int(parser.get(val, \
						'LOCAL_LOW_LIMIT_PORT'))

			if parser.has_option(val, 'LOCAL_HIGH_LIMIT_PORT'):
				LOCAL_HIGH_LIMIT_PORT = int(parser.get(val, \
						'LOCAL_HIGH_LIMIT_PORT'))

			if parser.has_option(val, 'REMOTE_LOW_LIMIT_PORT'):
				REMOTE_LOW_LIMIT_PORT = int(parser.get(val, \
						'REMOTE_LOW_LIMIT_PORT'))

			if parser.has_option(val, 'REMOTE_HIGH_LIMIT_PORT'):
				REMOTE_HIGH_LIMIT_PORT = int(parser.get(val, \
						'REMOTE_HIGH_LIMIT_PORT'))

			if DIRECTION == 'bidirectional':
				RULE_STRING = \
				str("%s/%s %s/%s %s : %s %s : %s %s/%s") % \
					(IPV4_REMOTE, IPV4_REMOTE_MASK, \
					LOCAL_IP, LOCAL_IP_MASK, \
					REMOTE_LOW_LIMIT_PORT, \
					REMOTE_HIGH_LIMIT_PORT, \
					LOCAL_LOW_LIMIT_PORT, \
					LOCAL_HIGH_LIMIT_PORT, \
					PROTOCOL, PROTOCOL_MASK)

			elif DIRECTION == 'uplink_only':
				RULE_STRING = \
				str("%s/%s %s/%s %s : %s %s : %s %s/%s") % \
					(LOCAL_IP, LOCAL_IP_MASK, \
					IPV4_REMOTE, IPV4_REMOTE_MASK, \
					LOCAL_LOW_LIMIT_PORT, \
					LOCAL_HIGH_LIMIT_PORT, \
					REMOTE_LOW_LIMIT_PORT, \
					REMOTE_HIGH_LIMIT_PORT, \
					PROTOCOL, PROTOCOL_MASK)

			elif DIRECTION == 'downlink_only':
				RULE_STRING = \
				str("%s/%s %s/%s %s : %s %s : %s 0x%s/0x%s") % \
					(IPV4_REMOTE, IPV4_REMOTE_MASK, \
					LOCAL_IP, LOCAL_IP_MASK, \
					REMOTE_LOW_LIMIT_PORT, \
					REMOTE_HIGH_LIMIT_PORT, \
					LOCAL_LOW_LIMIT_PORT, \
					LOCAL_HIGH_LIMIT_PORT, \
					PROTOCOL, PROTOCOL_MASK)

			# TBD: Need to handle exception
			# Pack the structure and send over the zmq socket to DP
			pub_socket.send("%s" % (struct.pack('!BBLLBI'+\
				str(len(RULE_STRING))+'s',topicId, MSG_TYPE,\
				PCC_RULE_ID, PRECEDENCE, RULE_TYPE, \
				len(RULE_STRING), RULE_STRING)))
			time.sleep(1)
			print "\nSDF Rule Values for %s ::  \nPCC_RULE_ID :%s \
					\nPRECEDENCE :%s \nRULE_TYPE :%s \nRULE_STRING :%s"\
					 % (val, PCC_RULE_ID, PRECEDENCE, RULE_TYPE, RULE_STRING)
			print '\n ---># SDF Rule Successfully sent.. #<---\n'
	parser.clear()
