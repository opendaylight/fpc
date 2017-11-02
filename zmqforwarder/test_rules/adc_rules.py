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

# Covert the string into num and stored into usigned 32 bit integer.
def name_to_num(name):
	num = 0
	for ch in name[::-1]:
		num = (num << 4) | (ord(ch) - ord('a'))
	return num & 0xffffffff

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

			GATE_STATUS = int(parser.get(val, 'GATE_STATUS'))
			RATING_GROUP = name_to_num(str(parser.get(val, \
							'RATING_GROUP')))
			SERVICE_ID = name_to_num(str(parser.get(val, \
							'SERVICE_ID')))
			PRECEDENCE = int(parser.get(val, 'PRECEDENCE'))
			MTR_PROFILE_INDEX = int(parser.get(val, \
							'MTR_PROFILE_INDEX'))
			SPONSOR = str(parser.get(val, 'SPONSOR'))

			#TBD:: Need to handle exception
			# Pack the structure and send over the zmq socket to DP
			time.sleep(1)
			if ADC_TYPE == 0:
				pub_socket.send("%s" % (struct.pack('!BBBB'+ \
						str(len(DOMAIN))+'sBLLLHB'+\
						str(len(SPONSOR))+'s', \
						topicId, MSG_TYPE, ADC_TYPE, \
						len(DOMAIN), DOMAIN, \
						GATE_STATUS, RATING_GROUP, \
						SERVICE_ID, PRECEDENCE, \
						MTR_PROFILE_INDEX, \
						len(SPONSOR), SPONSOR)))
				time.sleep(1)

				print "\nADC Rule Values for ::\
						\nADC_TYPE :%s \nDOMAIN :%s\
						\nGATE_STATUS :%s\
						\nrating group :%s \
						\nSERVICE_ID :%s \
						\nPRECEDENCE :%s \
						\nMTR_PROFILE_INDEX :%s\
						\nSPONSOR_LEN :%s\
						\nSPONSOR :%s" % \
						(ADC_TYPE, DOMAIN, GATE_STATUS,\
						RATING_GROUP, SERVICE_ID, \
						PRECEDENCE, MTR_PROFILE_INDEX, \
						len(SPONSOR), SPONSOR)

			if ADC_TYPE == 1:
				pub_socket.send("%s" % (struct.pack('!BBBLBLLLHB'+\
						str(len(SPONSOR))+'s',topicId, \
						MSG_TYPE, ADC_TYPE, IP[0], \
						GATE_STATUS, RATING_GROUP, \
						SERVICE_ID, PRECEDENCE, \
						MTR_PROFILE_INDEX, \
						len(SPONSOR), SPONSOR)))

				print "\nADC Rule Values for ::\
						\nADC_TYPE :%s \nIP :%s \
						\nGATE_STATUS :%s\
						\nRATING_GROUP :%s\
						\nSERVICE_ID :%s\
						\nPRECEDENCE :%s\
						\nMTR_PROFILE_INDEX :%s\
						\nSPONSOR_LEN :%s \
						\nSPONSOR :%s" % \
						(ADC_TYPE, IP[0], GATE_STATUS,\
						RATING_GROUP, SERVICE_ID, \
						PRECEDENCE, MTR_PROFILE_INDEX, \
						len(SPONSOR), SPONSOR)

			if ADC_TYPE == 2:
				pub_socket.send("%s" % (struct.pack('!BBBLHBL\
						LLHB'+str(len(SPONSOR))+'s', \
						topicId, MSG_TYPE, ADC_TYPE, \
						IP[0], PREFIX, GATE_STATUS, \
						RATING_GROUP, SERVICE_ID, \
						PRECEDENCE, MTR_PROFILE_INDEX, \
						len(SPONSOR), SPONSOR)))

				print "\nADC Rule Values for ::\
						\nADC_TYPE :%s \nIP :%s \
						\nPREFIX :%s\nGATE_STATUS :%s \
						\nRATING_GROUP:%s \
						\nSERVICE_ID :%s\
						\nPRECEDENCE :%s \
						\nMTR_PROFILE_INDEX :%s\
						\nSPONSOR_LEN :%s\
						\nSPONSOR :%s"\
						 % (ADC_TYPE, IP[0], \
						PREFIX, GATE_STATUS, \
						RATING_GROUP, SERVICE_ID, \
						PRECEDENCE, MTR_PROFILE_INDEX, \
						len(SPONSOR), SPONSOR)


			time.sleep(1)
			print '\n ---># ADC Rule Successfully sent...#<---\n'
	parser.clear()

