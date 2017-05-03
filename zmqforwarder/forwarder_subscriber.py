# coding: utf8
#Copyright © 2016 Copyright (c) Sprint, Inc. and others.  All rights reserved.
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


port = "5560"
# Socket to talk to server
context = zmq.Context()
socket = context.socket(zmq.SUB)
print "Collecting updates from server..."
socket.connect ("tcp://localhost:%s" % port)
topicfilter = "1"
socket.setsockopt(zmq.SUBSCRIBE, topicfilter)
print "Listening to port ", port
count = 0
msgnum1count = 0
msgnum2count = 0
msgnum3count = 0
msgnum4count = 0
msgnum5count = 0
msgnum6count = 0
for update_nbr in range(900000):
    ts = time.time()
    st = datetime.datetime.fromtimestamp(ts).strftime('%Y-%m-%d %H:%M:%S')
    string = socket.recv()
    print 'Receiving message', count, ' at ', st, ' ..... '
    count += 1
    print 'length of message = ', len(string)
 
    topic,msgnum = struct.unpack('!cB',string[:2])

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
        print 's1u_enb_gtpu_ipv4 = ', s1u_enb_gtpu_ipv4a
        print 'dl s1u_enb_gtpu_teid = ', s1u_enb_gtpu_teid
        print 'dl s1u_sgw_gtpu_teid = ', s1u_sgw_gtpu_teid
    elif msgnum == 3:
        msgnum3count += 1
        topic, msgnum, default_ebi, s1u_sgw_gtpu_teid = struct.unpack("!cBBL",string[:8])
        print 'default_ebi = ', default_ebi
        print 's1u_sgw_gtpu_teid = ', s1u_sgw_gtpu_teid
    elif msgnum == 4:
        msgnum4count += 1
        topic, msgnum, s1u_enb_gtpu_ipv4, s1u_enb_gtpu_teid, s1u_sgw_gtpu_teid  = struct.unpack("!cBLLL",string[:14])
        s1u_enb_gtpu_ipv4a = socketlib.inet_ntoa(struct.pack('!L',s1u_enb_gtpu_ipv4))
        print 's1u_enb_gtpu_ipv4 = ', s1u_enb_gtpu_ipv4a
        print 'ul s1u_enb_gtpu_teid = ', s1u_enb_gtpu_teid
        print 'ul s1u_sgw_gtpu_teid = ', s1u_sgw_gtpu_teid
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
        topic,msgnum, teid = struct.unpack('!cBL',string[:6])
        print 'teid = ', teid

    print '================'
    print 'Total = ', count, 'msgnum1 count', msgnum1count, 'msgnum2 count', msgnum2count, 'msgnum3 count', msgnum3count, 'msgnum4 count', msgnum4count,'msgnum5 count', msgnum5count, 'msgnum6 count', msgnum6count
socket.close()
