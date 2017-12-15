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
	parser.read('./config/adc_rules.cfg')
	print "\n ---> Reading Values from ADC rules file <---"
	print "\n ---> Sending ADC rules   <---"
	MSG_TYPE = 17

	# Create a structure for ADC rule and parse values in that.
	for val in parser.sections():
		if val != 'GLOBAL':
			# TBD: Need to handle exception
			ADC_TYPE = int(parser.get(val, 'ADC_TYPE'))
			if ADC_TYPE == 0:
				DOMAIN = str(parser.get(val, 'DOMAIN'))
			if ADC_TYPE == 1:
				IP = struct.unpack('!L', \
						socket.inet_aton(str(parser.get(val, \
						'IP'))))
			if ADC_TYPE == 2:
				IP = struct.unpack('!L', \
						socket.inet_aton(str(parser.get(val, \
						'IP'))))
				PREFIX = int(parser.get(val, 'PREFIX'))

			#TBD:: Need to handle exception
			# Pack the structure and send over the zmq socket to DP
			if ADC_TYPE == 0:
				pub_socket.send("%s" % (struct.pack('!BBBB'+ \
						str(len(DOMAIN))+'s',\
						topicId, MSG_TYPE, ADC_TYPE, \
						len(DOMAIN), DOMAIN)))
				time.sleep(1)
				print "\nADC Rule Values for ::\
						\nADC_TYPE :%s \nDOMAIN :%s" %\
						(ADC_TYPE, DOMAIN)

			if ADC_TYPE == 1:
				pub_socket.send("%s" % (struct.pack('!BBBL', 
						topicId, MSG_TYPE, ADC_TYPE, \
						IP[0])))

				print "\nADC Rule Values for ::\
						\nADC_TYPE :%s \nIP :%s" % \
						(ADC_TYPE, IP[0])

			if ADC_TYPE == 2:
				pub_socket.send("%s" % (struct.pack('!BBBLH', \
						topicId, MSG_TYPE, ADC_TYPE, \
						IP[0], PREFIX)))

				print "\nADC Rule Values for ::\
						\nADC_TYPE :%s \nIP :%s \
						\nPREFIX :%s" \
						 % (ADC_TYPE, IP[0], PREFIX)


			print '\n ---># ADC Rule Successfully sent...#<---\n'
	parser.clear()

