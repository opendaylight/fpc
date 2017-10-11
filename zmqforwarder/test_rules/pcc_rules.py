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
	# TBD: Needs to handle exception
        parser.read('./config/pcc_config.cfg')
        print "\n ---> Reading Values from PCC config file <--- \n"
        print "\n ---> Hello Start PCC Rule Sending ....!!!!!"
        msg_type = 18

	# Formed a struture for PCC rule and parse the values in that. 
        for val in parser.sections():
		# TBD: Needs to handle exception
                rule_id = int(parser.get(val, 'rule_id'))
                rule_name = str(parser.get(val, 'rule_name'))
                rating_group = int(parser.get(val, 'rating_group'))
                service_id = int(parser.get(val, 'service_id'))
                rule_status = int(parser.get(val, 'rule_status'))
                gate_status = int(parser.get(val, 'gate_status'))
                session_cont = int(parser.get(val, 'session_cont'))
                report_level = int(parser.get(val, 'report_level'))
                charging_mode = int(parser.get(val, 'charging_mode'))
                metering_method = int(parser.get(val, 'metering_method'))
                mute_notify = int(parser.get(val, 'mute_notify'))
                monitoring_key = int(parser.get(val, 'monitoring_key'))
                sponsor_id = str(parser.get(val, 'sponsor_id'))
                redirect_info = int(parser.get(val, 'redirect_info'))
                precedence = int(parser.get(val, 'precedence'))
                mtr_profile_index = int(parser.get(val, 'mtr_profile_index'))

                var = struct.Struct('!BBBBHBBLLBBBI'+str(len(rule_name))+'sI'\
			+str(len(sponsor_id))+'s')

                values = (topicId, msg_type, metering_method, charging_mode,\
				 rating_group, rule_status, gate_status, \
				monitoring_key, precedence, report_level, \
				mute_notify, redirect_info, len(rule_name)\
				, rule_name, len(sponsor_id), sponsor_id)

		# TBD: Needs to handle exception
		# Pack the structure and send over the zmq socket to dp
 
                pub_socket.send("%s" % (var.pack(*values)))
                time.sleep(1)

                print "\nPrint PCC Rule Values for %s ::\nrule_id :%s \
			\nrule_name : %s \nrating_group : %s\nservice_id : %s \
			\nrule_status : %s \ngate_status : %s \nsession_cont :\
			 %s \nreport_level : %s \ncharging_mode : %s \
			\nmetering_method : %s \nmute_notify : %s \
			\nmonitoring_key : %s \nsponsor_id : %s \
			\nredirect_info : %s \nprecedence : %s \
			\nmtr_profile_index : %s\n\n" % (val, rule_id, \
			rule_name, rating_group, service_id, rule_status, \
			gate_status, session_cont, report_level, \
			charging_mode, metering_method, mute_notify, \
			monitoring_key, sponsor_id, redirect_info, precedence\
			, mtr_profile_index)
                print '\n --->## Successfuly Send PCC Rule ##<---\n'
        parser.clear()

