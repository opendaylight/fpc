#!/usr/bin/python
#coding: utf8
#Copyright © 2016 - 2017 Copyright (c) Sprint, Inc. and others. All rights 
#reserved.
#
#This program and the accompanying materials are made available under the
#terms of the Eclipse Public License v1.0 which accompanies this distribution,
#and is available at http://www.eclipse.org/legal/epl-v10.html
#
##############################################################################
#
# File : mtr_rules.py
#
# Comments :
# Read METER Rules config file from './config/' and get parameters
# values as per the METER Rule Table.
# As per METER Rule table, formed a structure and parse values in structure,
# and finally pack a structure and send over the zmq(PUB/SUB) socket to DP.
#
# Reference : message_sdn_dp.docx
# Section : Table No.13 METER Rule
##############################################################################

import sys
import os
import time
import struct
import socket

from configparser import ConfigParser

parser = ConfigParser()

def parse_mtr_values(pub_socket,topicId):
	# TBD: Need to handle exception
	parser.read('./config/meter_profile.cfg')
	print "\n ---> Reading Values from Meter profile file <--- "
	print "\n ---> Sending Meter Rules  <---"
	MSG_TYPE = 19
	RULE_ID = 0

	# Create struct for meter rule and parse values in that.
	for val in parser.sections():
		if val != 'GLOBAL':
			RULE_ID += 1
			# TBD: Need to handle exception
			CIR = int(parser.get(val, 'CIR'))
			CBS = int(parser.get(val, 'CBS'))
			EBS = int(parser.get(val, 'EBS'))
			MTR_PROFILE_IDX = int(parser.get(val, \
					'MTR_PROFILE_IDX'))

			METERING_METHOD = 0

			# Pack the struct and send over the zmq socket to dp.
			pub_socket.send("%s" % (struct.pack('!BBHQQQH',topicId,\
			       MSG_TYPE, MTR_PROFILE_IDX, CIR, CBS, EBS, \
			       METERING_METHOD)))

			print "\nMETER Rule Values for %s ::\nRULE_ID :%s\
					\nMSG_TYPE :%s \nCIR :%s \nCBS :%s \
					\nEBS :%s \nMTR_PROFILE_IDX :%s\
					\nMETERING_METHOD :%s\n " % \
					(val, RULE_ID, MSG_TYPE, CIR, \
					CBS, EBS, MTR_PROFILE_IDX, \
					METERING_METHOD)

			print '\n ---># Meter Rule Successfully sent..#<---\n'
	parser.clear()
