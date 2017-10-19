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

from configparser import ConfigParser

parser = ConfigParser()

def parse_sdf_values(pub_socket,topicId):
	# TBD: Needs to handle exception
        parser.read('./config/sdf_config.cfg')
        print "\n ---> Reading Values from SDF config file <--- "
        print "\n ---> Hello Start SDF Rules Sending ....!!!!!"
        msg_type = 20

	# Formed the structure for SDF rule and parse the values in that.
        for val in parser.sections():
		# TBD: Needs to handle exception
                pcc_rule_id = int(parser.get(val, 'pcc_rule_id'))
                precedence = int(parser.get(val, 'precedence'))
                rule_type = int(parser.get(val, 'rule_type'))
                rule_string = str(parser.get(val, 'rule_string'))
		# TBD: Needs to handle exception
		# Pack the structure and send over the zmq socket to DP
                pub_socket.send("%s" % (struct.pack('!BBLLBI'+\
			str(len(rule_string))+'s',topicId, msg_type, \
			pcc_rule_id, precedence, rule_type, len(rule_string),\
			 rule_string)))
                time.sleep(1)
                print "\nPrint SDF Rule Values for %s ::  \npcc_rule_id :%s \
			\nprecedence :%s \nrule_type : %s \nrule_string :%s"\
			 % (val, pcc_rule_id, precedence, rule_type,rule_string)
                print '\n --->## Successfuly Send SDF Rule ##<---\n'
        parser.clear()

