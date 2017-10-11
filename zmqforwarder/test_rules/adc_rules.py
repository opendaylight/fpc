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
# File : adc_rules.py
#
# Comments : 
# Read ADC Rules config file from './config/' and get parameters
# values as per the ADC Rule Table.
# As per ADC Rule table, formed a structure and parse values in struct,
# and finally pack a struct and send over the zmq(PUB/SUB) socket to DP.   
#
# Reference : message_sdn_dp.docx
# Section : Table No.11 ADC Rule
##############################################################################

import sys
import os
import time
import struct
import socket

from configparser import ConfigParser

parser = ConfigParser()

def parse_adc_values(pub_socket,topicId):
	try:
		parser.read('./config/adc_config.cfg')
		print "\n ---> Reading Values from ADC config file <---"
		print "\n ---> Hello Start ADC Rule Sending ....!!!!!"
		msg_type = 17

		# Formed a structure for ADC rule and parse values in that.
		for val in parser.sections():
			# TBD: Needs to handle exception
		        adc_type = int(parser.get(val, 'ADC_TYPE'))
		        if adc_type == 0:
		                domain = str(parser.get(val, 'DOMAIN'))
		        if adc_type == 1:
		                print "IP Is---->"
		                print parser.get(val, 'IP')
		                ip = struct.unpack('!L', \
					socket.inet_aton(str(parser.get(val, \
					'IP'))))
		        if adc_type == 2:
		                ip = struct.unpack('!L', \
					socket.inet_aton(str(parser.get(val, \
					'IP'))))
		                prefix = int(parser.get(val, 'PREFIX'))
		        gate = int(parser.get(val, 'GATE'))
		        rating_group = int(parser.get(val, 'RATING_GROUP'))
		        service_id = int(parser.get(val, 'SERVICE_ID'))
		        sponsor = str(parser.get(val, 'SPONSOR'))

			#TBD:: Needs to handle exception
			# Pack the structure and send over the zmq socket to DP
			time.sleep(1)		
		        if adc_type == 0:
		                pub_socket.send("%s" % (struct.pack('!BBBB'+ \
					str(len(domain))+'sBLLB'+\
					str(len(sponsor))+'s',topicId, \
					msg_type, adc_type, len(domain), \
					domain, gate, rating_group, service_id, \
					len(sponsor), sponsor)))
		                time.sleep(1)
		
		                print "\nPrint ADC Rule Values for \nadc_type \
					: %s \nadc_val : %s \ngate : %s \
					\nrating group : %s \nservice : %s \
					\nsponsor len : %s\nsponsor : %s" % \
					(adc_type, domain, gate, rating_group\
					, service_id, len(sponsor), sponsor)
		        if adc_type == 1:
		                pub_socket.send("%s" % (struct.pack('!BBBLBLLB'+\
					str(len(sponsor))+'s',topicId, \
					msg_type, adc_type, ip[0], gate, \
					rating_group, service_id, len(sponsor), \
					sponsor)))
		
		                print "\nPrint ADC Rule Values for ::\
					\nadc_type : %s \nadc_val : %s \ngate \
					: %s \nrating group : %s \nservice : \
					%s\nsponsor len : %s \nsponsor : %s" \
					% (adc_type, ip[0], gate, rating_group,\
					 service_id, len(sponsor), sponsor)
		
		        if adc_type == 2:
		                pub_socket.send("%s" % (struct.pack('!BBBLHBL\
					LB'+str(len(sponsor))+'s',topicId, \
					msg_type, adc_type, ip[0], prefix, \
					gate, rating_group, service_id, \
					len(sponsor), sponsor)))
		
		                print "\nPrint ADC Rule Values for ::\
					\nadc_type : %s \nIP : %s \nprefix : \
					%s\ngate : %s \nrating group : %s \n\
					service - %s\nsponsor len : %s \n\
					sponsor - %s" % (adc_type, ip[0], \
					prefix, gate, rating_group, \
					service_id, len(sponsor), sponsor)
		
		
		        time.sleep(1)
		        print '\n --->## Successfuly ADC Meter Rule ##<---\n'
		parser.clear()
	except:
		print "\n ---> Error, while parsing adc rules <---\n"	
