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
#print "Collecting updates from server..."
socket.connect ("tcp://localhost:%s" % port)
topicfilter = str(sys.argv[1])
socket.setsockopt(zmq.SUBSCRIBE, topicfilter)
#print "Listening to port ", port
count = 0
msgnum1count = 0
msgnum2count = 0
msgnum3count = 0
msgnum4count = 0
msgnum5count = 0
msgnum6count = 0
print "-----------------"
print "| This is DPN",str(sys.argv[1]),"|"
print "-----------------"
while True:
    ts = time.time()
    st = datetime.datetime.fromtimestamp(ts).strftime('%Y-%m-%d %H:%M:%S')
    string = socket.recv()
    #print 'Receiving message', count, ' at ', st, ' ..... '
    count += 1
    #print 'length of message = ', len(string)
 
    topic,msgnum = struct.unpack('!cB',string[:2])

    #print 'topic =', topic
    #print 'msgnum =', msgnum

    if msgnum == 1:
        msgnum1count += 1
        topic,msgnum, imsi, default_ebi, ue_ip, s1u_sgw_gtpu_teid, s1u_sgw_gtpu_ipv4 = struct.unpack('!cBQBLLL',string[:23])    
        #print 'imsi = ', imsi
        ipa = socketlib.inet_ntoa(struct.pack('!L',ue_ip))
        #print 'ue_ip = ', ipa
        #print 'default_ebi = ', default_ebi
        s1u_sgw_gtpu_ipv4a = socketlib.inet_ntoa(struct.pack('!L',s1u_sgw_gtpu_ipv4))
        #print 's1u_sgw_gtpu_ipv4 = ', s1u_sgw_gtpu_ipv4a
        #print 's1u_sgw_gtpu_teid = ', s1u_sgw_gtpu_teid
        print "-------------------------------------------------------------"
        print "Create Session received for UE with IP - ",ipa
        print "-------------------------------------------------------------"
    elif msgnum == 2:
        msgnum2count += 1
        topic, msgnum, s1u_enb_gtpu_ipv4, s1u_enb_gtpu_teid, s1u_sgw_gtpu_teid  = struct.unpack("!cBLLL",string[:14])
        s1u_enb_gtpu_ipv4a = socketlib.inet_ntoa(struct.pack('!L',s1u_enb_gtpu_ipv4))
        #print 's1u_enb_gtpu_ipv4 = ', s1u_enb_gtpu_ipv4a
        #print 'dl s1u_enb_gtpu_teid = ', s1u_enb_gtpu_teid
        #print 'dl s1u_sgw_gtpu_teid = ', s1u_sgw_gtpu_teid
        print "Modify Bearer received for UE with IP - ",ipa
        print "-------------------------------------------------------------"
    elif msgnum == 3:
        msgnum3count += 1
        topic, msgnum, default_ebi, s1u_sgw_gtpu_teid = struct.unpack("!cBBL",string[:8])
        #print 'default_ebi = ', default_ebi
        #print 's1u_sgw_gtpu_teid = ', s1u_sgw_gtpu_teid
        print "Delete Session received for UE with IP - ",ipa
        print "-------------------------------------------------------------"
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

    elif msgnum == 17:
        print "-------------------------------------------------------------"
        print "ADC Rule received. Details:"
        topic,msgnum,selector_type = struct.unpack('!BBB',string[:3])
        if(selector_type == 0):
            domain_name_length, = struct.unpack('!B',string[3:4])
            domain_name, = struct.unpack('!'+str(domain_name_length)+'s',string[4:4+int(domain_name_length)])
            rule_id_index = 4+int(domain_name_length)
            print "Domain Name = ",domain_name

        if selector_type == 2:
            ip_prefix, = struct.unpack('!H',string[7:9])
            rule_id_index += 2

        rule_id, = struct.unpack('!L',string[rule_id_index:rule_id_index+4])
        print "Rule Id = ", rule_id

        rating_group,service_id,sponsor_id_length = struct.unpack('!LLB', string[rule_id_index+4:rule_id_index+4+9])
        print "Rating Group = ", rating_group
        print "Service Id = ", service_id
        #print "Sponsor Length = ", sponsor_id_length
        sponsor_id, = struct.unpack('!'+str(sponsor_id_length)+'s',string[rule_id_index+4+9:rule_id_index+4+9+int(sponsor_id_length)])
        print "Sponsor = ", sponsor_id 
        print "-------------------------------------------------------------"
        #precedence, = struct.unpack('!L',string[rule_id_index+4+9+int(sponsor_id_length):rule_id_index+4+9+int(sponsor_id_length)+4])
        #print "precedence = ", precedence

    #print '================'
    #print 'Total = ', count, 'msgnum1 count', msgnum1count, 'msgnum2 count', msgnum2count, 'msgnum3 count', msgnum3count, 'msgnum4 count', msgnum4count,'msgnum5 count', msgnum5count, 'msgnum6 count', msgnum6count
socket.close()




























