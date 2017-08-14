#!/usr/bin/python
# coding: utf8
#Copyright © 2016 - 2017 Copyright (c) Sprint, Inc. and others.  All rights reserved.
#
#This program and the accompanying materials are made available under the
#terms of the Eclipse Public License v1.0 which accompanies this distribution,
#and is available at http://www.eclipse.org/legal/epl-v10.html

import sys
import zmq
import struct
import socket as socketlib
import datetime
import time

msgnum1count = 0
msgnum2count = 0
msgnum3count = 0
msgnum5count = 0
msgnum6count = 0


port = "5560"
# Socket to talk to server
context = zmq.Context()
socket = context.socket(zmq.SUB)
print "Collecting updates from server..."
socket.connect ("tcp://localhost:%s" % port)
topicfilter = "1"
socket.setsockopt(zmq.SUBSCRIBE, topicfilter)
print "Listening to port ", port
#for update_nbr in range(10000):
while True:
    ts = time.time()
    st = datetime.datetime.fromtimestamp(ts).strftime('%Y-%m-%d %H:%M:%S')
    string = socket.recv()
    print 'Receiving message at ', st, ' ..... '
    print 'length of message = ', len(string)
 
    #topic, messagedata = string.split()
    #imsi,ue_ip, default_ebi, s1u_sgw_gtpu_ipv4, s1u_sgw_gtpu_teid = struct.unpack('>q',messagedata[:8])

    topic,msgnum = struct.unpack('!cB',string[:2])
    
    #topic,space,msgnum, imsi, default_ebi, ue_ip, s1u_sgw_gtpu_teid, s1u_sgw_gtpu_ipv4 = struct.unpack('!ccBQBLLL',string[:24])

    print 'apn =', topic
    print 'msgnum =', msgnum

    if msgnum == 1:
        msgnum1count += 1
        topic,msgnum, imsi, default_ebi, ue_ip, s1u_sgw_gtpu_teid, s1u_sgw_gtpu_ipv4 = struct.unpack('!cBQBLLL',string[:23])    
        print 'imsi = ', imsi
        ipa = socketlib.inet_ntoa(struct.pack('!L',ue_ip))
        print 'ue_ip = ', ipa
        print 'default_ebi = ', default_ebi
        s1u_sgw_gtpu_ipv4a = socketlib.inet_ntoa(struct.pack('!L',s1u_sgw_gtpu_ipv4))
        print 's1u_sgw_gtpu_ipv4 = ', s1u_sgw_gtpu_ipv4a
        print 's1u_sgw_gtpu_teid = ', s1u_sgw_gtpu_teid
    elif msgnum == 2:
        msgnum2count += 1 
        topic, msgnum, s1u_enb_gtpu_ipv4, s1u_enb_gtpu_teid, s1u_sgw_gtpu_teid  = struct.unpack("!cBLLL",string[:14])
        
        s1u_enb_gtpu_ipv4a = socketlib.inet_ntoa(struct.pack('!L',s1u_enb_gtpu_ipv4))
        print "s1u_enb_gtpu_ipv4 = ", s1u_enb_gtpu_ipv4a
        print 's1u_enb_gtpu_teid = ', s1u_enb_gtpu_teid
        print 's1u_sgw_gtpu_teid = ', s1u_sgw_gtpu_teid
    elif msgnum == 3:
        msgnum3count += 1 
        topic, msgnum, default_ebi, s1u_sgw_gtpu_teid = struct.unpack("!cBBL",string[:7])   
        print "default_ebi = ", default_ebi
        print "s1u_sgw_gtpu_teid = ", s1u_sgw_gtpu_teid
    
    elif msgnum == 5:
        msgnum5count += 1 
        topic,msgnum, imsi, default_ebi, dedicated_ebi, s1u_sgw_gtpu_ipv4, s1u_sgw_gtpu_teid = struct.unpack('!cBQBBLL',string[:20])
        print 'imsi = ', imsi
        print 'default_ebi = ', default_ebi
        print 'dedicated_ebi = ', dedicated_ebi
        s1u_sgw_gtpu_ipv4a = socketlib.inet_ntoa(struct.pack('!L',s1u_sgw_gtpu_ipv4))
        print 's1u_sgw_gtpu_ipv4 = ', s1u_sgw_gtpu_ipv4a
        print 's1u_sgw_gtpu_teid = ', s1u_sgw_gtpu_teid
    
    elif msgnum == 6:
        msgnum6count += 1 
	topic,msgnum, dedicated_ebi, s1u_sgw_gtpu_teid, s1u_enb_gtpu_ipv4, s1u_enb_gtpu_teid = struct.unpack('!cBBLLL',string[:15])
        print 'dedicated_ebi = ', dedicated_ebi
        print 's1u_sgw_gtpu_teid = ', s1u_sgw_gtpu_teid
        s1u_enb_gtpu_ipv4a = socketlib.inet_ntoa(struct.pack('!L',s1u_enb_gtpu_ipv4))
        print "s1u_enb_gtpu_ipv4 = ", s1u_enb_gtpu_ipv4a
	print 's1u_enb_gtpu_teid = ', s1u_enb_gtpu_teid
    print '[msgnum1 = '+ str(msgnum1count) + ', msgnum2 = ' + str(msgnum2count) + ', msgnum3 = ' + str(msgnum3count) + ', msgnum5 = ' + str(msgnum5count) + ', msgnum6 = ' + str(msgnum6count) +']'
    print '================'
    #topic, messagedata = string.split()
    #print topic, messagedata
socket.close()

