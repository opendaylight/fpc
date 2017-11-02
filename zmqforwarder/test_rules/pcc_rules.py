#!/usr/bin/python
#coding: utf8
#Copyright © 2016 - 2017 Copyright (c) Sprint, Inc. and others.  All rights 
#reserved.
#
#This program and the accompanying materials are made available under the
#terms of the Eclipse Public License v1.0 which accompanies this distribution,
#and is available at http://www.eclipse.org/legal/epl-v10.html
#
############################################################################
# File : pcc_rules.py
#
# Comments :
# Read PCC Rules config file from './config/' and get parameters
# values as per the PCC Rule Table.
# As per PCC Rule table, formed a structure and parse values in structure,
# and finally pack a structure and send over the zmq(PUB/SUB) socket to DP.
#
# Reference : message_sdn_dp.docx
# Section : Table No.12 PCC Rule
############################################################################

import sys
import os
import time
import struct
import socket

from configparser import ConfigParser

parser = ConfigParser()

def parse_pcc_values(pub_socket,topicId):
	# TBD: Need to handle exception
	parser.read('./config/pcc_rules.cfg')
	print "\n ---> Reading Values from PCC config file <--- \n"
	print "\n ---> Sending PCC Rules <---"
	MSG_TYPE = 18
	RULE_ID = 0

	# Create a struture for PCC rule and parse the values in that.
	for val in parser.sections():
		if val != 'GLOBAL':
			# TBD: Need to handle exception
			#RULE_ID = int(parser.get(val, 'RULE_ID'))
			RULE_ID += 1
			RULE_NAME = str(parser.get(val, 'RULE_NAME'))
			RATING_GROUP = int(parser.get(val, 'RATING_GROUP'))
			SERVICE_ID = int(parser.get(val, 'SERVICE_ID'))
			RULE_STATUS = int(parser.get(val, 'RULE_STATUS'))
			GATE_STATUS = int(parser.get(val, 'GATE_STATUS'))
			SESSION_CONT = int(parser.get(val, 'SESSION_CONT'))
			REPORT_LEVEL = int(parser.get(val, 'REPORT_LEVEL'))
			CHARGING_MODE = int(parser.get(val, 'CHARGING_MODE'))
			METERING_METHOD = int(parser.get(val, 'METERING_METHOD'))
			MUTE_NOTIFY = int(parser.get(val, 'MUTE_NOTIFY'))
			MONITORING_KEY = int(parser.get(val, 'MONITORING_KEY'))
			SPONSOR_ID = str(parser.get(val, 'SPONSOR_ID'))
			REDIRECT_INFO = int(parser.get(val, 'REDIRECT_INFO'))
			PRECEDENCE = int(parser.get(val, 'PRECEDENCE'))
			DROP_PKT_COUNT = int(parser.get(val, 'DROP_PKT_COUNT'))
			UL_MBR_MTR_PROFILE_IDX = int(parser.get(val, \
						'UL_MBR_MTR_PROFILE_IDX'))
			DL_MBR_MTR_PROFILE_IDX = int(parser.get(val, \
						'DL_MBR_MTR_PROFILE_IDX'))

			var = struct.Struct('!BBBBHBBBLLBBQHHBI'+str(\
						len(RULE_NAME))+'sI'\
						+str(len(SPONSOR_ID))+'s')

			values = (topicId, MSG_TYPE, METERING_METHOD, \
					CHARGING_MODE, RATING_GROUP, \
					RULE_STATUS, GATE_STATUS, SESSION_CONT,\
					MONITORING_KEY, PRECEDENCE, \
					REPORT_LEVEL, MUTE_NOTIFY, \
					DROP_PKT_COUNT, \
					UL_MBR_MTR_PROFILE_IDX, \
					DL_MBR_MTR_PROFILE_IDX, \
					REDIRECT_INFO,\
					len(RULE_NAME), RULE_NAME, \
					len(SPONSOR_ID), SPONSOR_ID)

			# TBD: Need to handle exception
			# Pack the structure and send over the zmq socket to dp

			pub_socket.send("%s" % (var.pack(*values)))
			time.sleep(1)

			print "\nPCC Rule Values for %s ::\nRULE_ID :%s \
					\nRULE_NAME :%s\nRATING_GROUP :%s\
					\nSERVICE_ID :%s\nRULE_STATUS :%s\
					\nGATE_STATUS :%s\nSESSION_CONT :%s\
					\nREPORT_LEVEL :%s \nCHARGING_MODE :%s\
					\nMETERING_METHOD :%s\nMUTE_NOTIFY :%s\
					\nMONITORING_KEY :%s\nSPONSOR_ID :%s\
					\nREDIRECT_INFO :%s\nPRECEDENCE :%s\
					\nDROP_PKT_COUNT :%s\
					\nul_mbr_DROP_PKT_COUNT :%s\
					\ndl_mbr_DROP_PKT_COUNT :%s\n\n" % \
					(val, RULE_ID, RULE_NAME, RATING_GROUP,\
					SERVICE_ID, RULE_STATUS, GATE_STATUS, \
					SESSION_CONT, REPORT_LEVEL, \
					CHARGING_MODE, METERING_METHOD, \
					MUTE_NOTIFY, MONITORING_KEY, \
					SPONSOR_ID, REDIRECT_INFO, PRECEDENCE,\
					DROP_PKT_COUNT, UL_MBR_MTR_PROFILE_IDX,\
					DL_MBR_MTR_PROFILE_IDX)

			print '\n ---># PCC Rule Successfully sent..#<---\n'
	parser.clear()
