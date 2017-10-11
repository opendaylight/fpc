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
import signal
import argparse
import thread
import os

parser = argparse.ArgumentParser(
                    description="Subscribes and counts ZMQ messages on the SB. Displays message types on exit",
                    formatter_class=argparse.RawDescriptionHelpFormatter)
parser.add_argument('--quiet', action='store_true', default=False,
                    dest='quiet_boolean_switch',
                    help='Don\'t print every message, but still maintain a count')
results = parser.parse_args()

def hexdump(src):
    ret = ""
    i = 0
    while (i < len(src)):
        bytes = ""
        chars = ""
        line = i
        while True:
            if (i >= len(src)):
                break;
            bytes += "%02x " % (ord(src[i]))
            chars += (src[i] if (src[i]).isalnum() else ".")

            i += 1
            if i % 16 == 0:
                break;
            if i % 8 == 0:
                bytes += "   "
                chars += " "
        ret += "[%04x]  %-52s  %s\n" % (line, bytes, chars)
    return ret


msgstrings = ["RESERVED" for x in range(255)]
msgstrings[1] = "CREATE_SESSION"
msgstrings[2] = "MODIFY_BEARER"
msgstrings[3] = "DELETE_SESSION"
msgstrings[4] = "DPN_RESPONSE"
msgstrings[5] = "DDN"
msgstrings[6] = "DDN_ACK"
msgstrings[10] = "ASSIGN_TOPIC"
msgstrings[11] = "ASSIGN_CONFLICT"
msgstrings[12] = "DPN_STATUS_INDICATION"
msgstrings[13] = "DPN_STATUS_ACK"
msgstrings[14] = "CONTROLLER_STATUS_INDICATION"
msgstrings[15] = "GENERATE_CDR"
msgstrings[16] = "GENERATE_CDR_ACK"
msgstrings[17] = "ADC_RULE"
msgstrings[18] = "PCC_RULE"
msgstrings[19] = "METER_RULE"
msgstrings[20] = "SDF_RULE"
msgcounts = [0 for x in range(255)]
msglens = [0 for x in range(255)]
msglens[1]  = 40 
msglens[2]  = 31
msglens[3]  = 19
msglens[4]  = 11
msglens[5]  = 18
msglens[6]  = 14
totalcount = 0
port = "5560"
# Socket to talk to server
context = zmq.Context()
socket = context.socket(zmq.SUB)
print "Collecting updates from server... "
socket.connect ("tcp://localhost:%s" % port)
topicfilter = ""
socket.setsockopt(zmq.SUBSCRIBE, topicfilter)
print "Listening to port ", port


def print_totals():
    ts = time.time()
    st = datetime.datetime.fromtimestamp(ts).strftime('%Y-%m-%d %H:%M:%S')
    print "Printing total message counts:", st
    for x in range(255):
        if msgcounts[x] == 0:
            continue;
        print "\t%3d-%-30s: %d" % (x, msgstrings[x], msgcounts[x])
    print "\t%4s%-30s: %d" % ("", "total", totalcount)

def catch_signal(signal, frame):
    socket.close()
    print_totals()
    quit()
    
def print_totals_while_quiet():
    while True:
        print_totals()
        time.sleep(1)
        os.system('clear')
 
if results.quiet_boolean_switch:
    try:
        thread.start_new_thread(print_totals_while_quiet, ())
    except:
        print "Error: unable to print thread"


signal.signal(signal.SIGINT, catch_signal)

for update_nbr in range(900000):
    #ts = time.time()
    #st = datetime.datetime.fromtimestamp(ts).strftime('%Y-%m-%d %H:%M:%S')
    string = socket.recv()
 
    topic,msgnum = struct.unpack('!BB',string[:2])

    totalcount += 1
    msgcounts[msgnum] += 1

    if results.quiet_boolean_switch:
        continue

    print "\t%-20s %d" % ("topic", topic)
    print "\t%-20s %d (#%d/%d) %s" % ("msgnum", msgnum, msgcounts[msgnum], totalcount, msgstrings[msgnum])
    
    if (msglens[msgnum] != 0 and len(string) < msglens[msgnum]):
        print "\tExpecting %d bytes for message - got %d - cannot unpack" % (msglens[msgnum], len(string))
    elif msgnum == 1:
        
        topic,msgnum,imsi,default_ebi,ue_ip,squ_sgw_gtpu_teid,s1u_sgw_gtpu_ipv4,sess_id,controller_topic,client_id,opid = struct.unpack("!BBQBIIIQBII",string[:40])
        ipa = socketlib.inet_ntoa(struct.pack('!L',ue_ip))
        s1u_sgw_gtpu_ipv4a = socketlib.inet_ntoa(struct.pack('!L',s1u_sgw_gtpu_ipv4))

        print '\timsi                ', imsi
        print '\tdefault_ebi         ', default_ebi
        print '\tue_ip               ', ipa
        print '\tsqu_sgw_gtpu_teid   ', squ_sgw_gtpu_teid
        print '\ts1u_sgw_gtpu_ipv4   ', s1u_sgw_gtpu_ipv4a
        print '\tsess_id             ', sess_id
        print '\tcontroller_topic    ', controller_topic
        print '\tclient_id           ', client_id
        print '\topid                ', opid
    elif msgnum == 2:
        
        topic,msgnum,s1u_sgw_ipv4,s1u_enodeb_teid,s1u_enodeb_ipv4,session_id,controller_topic,client_id,op_id = struct.unpack("!BBIIIQBII", string[:31])
        s1u_sgw_ipv4a = socketlib.inet_ntoa(struct.pack('!L',s1u_sgw_ipv4))
        s1u_enodeb_ipv4a = socketlib.inet_ntoa(struct.pack('!L',s1u_enodeb_ipv4))

        print '\ts1u_sgw_ipv4        ',s1u_sgw_ipv4a  
        print '\ts1u_enodeb_teid     ',s1u_enodeb_teid  
        print '\ts1u_enodeb_ipv4     ',s1u_enodeb_ipv4a     
        print '\tsession_id          ',session_id       
        print '\tcontroller_topic    ',controller_topic 
        print '\tclient_id           ',client_id        
        print '\top_id               ',op_id            
        
    elif msgnum == 3:
        
        topic,msgnum,session_id,controller_topic,client_id,op_id = struct.unpack("!BBQBII", string[:19])

        print '\tsession_id          ', session_id
        print '\tcontroller_topic    ', controller_topic
        print '\tclient_id           ', client_id
        print '\top_id               ', op_id                
    elif msgnum == 4:
        
        topic,msgnum,status,client_id,op_id = struct.unpack("!BBBII", string[:11])

        print '\tstatus              ', status
        print '\tclient_id           ', client_id
        print '\top_id               ', op_id
        
    elif msgnum == 5:

        topic,msgnum,session_id,client_id,opid = struct.unpack("!BBQII", string[:18])

        print '\tsession_id          ',session_id       
        print '\tclient_id           ',client_id        
        print '\top_id               ',op_id            

    elif msgnum == 6:
        
        topic,msgnum,duration,count,controller_topic,client_id,op_id = struct.unpack("!BBBHbII", string[:14])
        
        print '\tduration            ', duration
        print '\tcount               ', count
        print '\tcontroller_topic    ', controller_topic
        print '\tclient_id           ', client_id
        print '\top_id               ', op_id        
        
    print hexdump(string)
    print '================'
#    print 'Total = ', count, 'msgnum1 count', msgnum1count, 'msgnum2 count', msgnum2count, 'msgnum3 count', msgnum3count, 'msgnum4 count', msgnum4count,'msgnum5 count', msgnum5count, 'msgnum6 count', msgnum6count
socket.close()
