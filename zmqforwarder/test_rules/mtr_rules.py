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
	# TBD: Needs to handle exception
        parser.read('./config/meter_config.cfg')
        print "\n ---> Reading Values from Meter config file <--- "
        print "\n ---> Hello Start Meter Rule Sending ....!!!!!"
        msg_type = 19

	# Formed struct for meter rule and parse values in that.
        for val in parser.sections():
		# TBD: Needs to handle exception
                mtr_profile_index = int(parser.get(val, 'mtr_profile_index'))
                cir = int(parser.get(val, 'CIR'))
                cbs = int(parser.get(val, 'CBS'))
                ebs = int(parser.get(val, 'EBS'))
                metering_method = int(parser.get(val, 'metering_method'))

		# Pack the struct and send over the zmq socket to dp.
                pub_socket.send("%s" % (struct.pack('!BBHQQQH',topicId, \
			msg_type, mtr_profile_index, cir, cbs, ebs, \
			metering_method)))

                print "\nPrint METER Rule Values for %s ::\ntopicId : %s \
			\nmsg_type : %s \nmtr_profile_index : %s \nCIR : %s \
			\nCBS : %s \nEBS : %s \nmetering_method : %s\n " % \
			(val, topicId, msg_type, mtr_profile_index, cir, cbs,\
			 ebs, metering_method)
                time.sleep(1)

                print '\n --->## Successfuly Send Meter Rule ##<---\n'
        parser.clear()
