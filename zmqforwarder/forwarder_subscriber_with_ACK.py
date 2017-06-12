#!/usr/bin/python
# coding: utf8
#Copyright © 2016 - 2017 Copyright (c) Sprint, Inc. and others.  All rights reserved.
#
#This program and the accompanying materials are made available under the
#terms of the Eclipse Public License v1.0 which accompanies this distribution,
#and is available at http://www.eclipse.org/legal/epl-v10.html

import signal
import sys
import zmq
import struct
import socket as socketlib
import datetime
import time
import random
import thread
from multiprocessing.pool import ThreadPool

pool = ThreadPool(processes=1)

conflict = False
topicId = None
nodeId = "node1"
networkId = "network2"
source = random.randrange(0,65535)
topicId = random.randrange(4,255)

def signal_handler(signal, frame):
    print "\nExiting... Sending DPN Status Indication message with Status = GOODBYE"
    pub_socket.send("%s" % (struct.pack("!BBBBIB",2,12,topicId,2,source,len(nodeId)) + nodeId + struct.pack('!B',len(networkId)) + networkId))
    count = 0
    while True:
        time.sleep(1)
        sys.stdout.write("\r"+str(5-count)+" ")
        sys.stdout.flush()
        count += 1
        if(count > 5):
            print "\n"
            sys.exit(0)

signal.signal(signal.SIGINT, signal_handler)

def sendAssignId(pub_socket): 
    global conflict
    global topicId
    time.sleep(1)
    pub_socket.send("%s" % (struct.pack('!BBBIB',1,10,topicId,source,len(nodeId)) + nodeId + struct.pack('!B',len(networkId)) + networkId))
    count = 0
    while True:
        time.sleep(1)
        sys.stdout.write("\r"+str(9-count)+" ")
        sys.stdout.flush()
        count += 1
        if conflict:
            conflict = False
            sendAssignId(pub_socket)
            return
        if count > 9:
            break
    print "\nDPN Topic = ", topicId
    print "Node Id = ", nodeId
    print "Network Id = ", networkId
    print "Source Id = ", source
    print "Sending Hello Message . . ."
    pub_socket.send("%s" % (struct.pack("!BBBBIB",2,12,topicId,1,source,len(nodeId)) + nodeId + struct.pack('!B',len(networkId)) + networkId))

    print "Ready to receive messages. Press Ctrl+C when ready to exit."

rec_port = "5560"
send_port = "5559"
# Socket to talk to server
context = zmq.Context()
socket = context.socket(zmq.SUB)
pub_socket = context.socket(zmq.PUB)
socket.connect ("tcp://localhost:%s" % rec_port)
pub_socket.connect("tcp://localhost:%s" % send_port)
topicfilter = ""
controller_topic= 252
socket.setsockopt(zmq.SUBSCRIBE, topicfilter)
print "Listening to port ", rec_port
print "DPN Lifecycle start up . . . Please wait."

async_result = pool.apply_async(sendAssignId,(pub_socket,))

count = 0
msgnum1count = 0
msgnum2count = 0
msgnum3count = 0
msgnum4count = 0
msgnum5count = 0
msgnum6count = 0
for update_nbr in range(900000):
    string = socket.recv()
    ts = time.time()
    st = datetime.datetime.fromtimestamp(ts).strftime('%Y-%m-%d %H:%M:%S')
 
    topic,msgnum = struct.unpack('!BB',string[:2])

    if topic == 1 and msgnum == 10: #Assign_Id
        top,msg,topId,src,nodeIdLen = struct.unpack('!BBBIB',string[:8])
        top,msg,topId,src,nodeIdLen,nodeId1,networkIdLen = struct.unpack('!BBBIB'+str(nodeIdLen)+'sB',string[:8+nodeIdLen+1])
        top,msg,topId,src,nodeIdLen,nodeId1,networkIdLen,networkId1 = struct.unpack('!BBBIB'+str(nodeIdLen)+'sB'+str(networkIdLen)+'s',string[:8+nodeIdLen+1+networkIdLen])
        #print nodeId1, networkId1
        if src != source and (topId == topicId or nodeId1 == nodeId):
            pub_socket.send("%s" % struct.pack('!BBBIBsBs',1,11,topicId,source,len(nodeId),nodeId,len(networkId),networkId))
        continue

    elif topic == 1 and msgnum == 11: #Assign_Conflict
        top,msg,topId,src,nodeIdLen = struct.unpack('!BBBIB',string[:8])
        top,msg,topId,src,nodeIdLen,nodeId1,networkIdLen = struct.unpack('!BBBIB'+str(nodeIdLen)+'sB',string[:8+nodeIdLen+1])
        top,msg,topId,src,nodeIdLen,nodeId1,networkIdLen,networkId1 = struct.unpack('!BBBIB'+str(nodeIdLen)+'sB'+str(networkIdLen)+'s',string[:8+nodeIdLen+1+networkIdLen])
        
        if src != source:
            if(nodeId == nodeId1):
                print "Received assign conflict for node id. Change the node id and restart this script."
                exit(0)
            if(networkId == networkId1):
                print "Received assign conflict for network id. Change the network id and restart this script."
                exit(0)
            if(top == topicId):
                print "Received assign conflict for topic id. Generating new topic id and resending Assign Topic Id Message."
                topicId = random.randrange(0,255)
            conflict = True
        continue

    elif topic == topicId and msgnum == 13:
        top, msg, controller_topic, controller_status = struct.unpack('!BBBB',string[:4])
        print "DPN Status ACK received. Controller Topic = ",controller_topic
        continue

    elif topic == 3 and msgnum == 14:
        top, msg, controller_topic, controller_status = struct.unpack('!BBBB',string[:4])
        if controller_status == 1:
            print "Received controller Hello. Controller Topic = ",controller_topic
            print "Sending Hello To Controller that has a topic id of ", controller_topic
            pub_socket.send("%s" % (struct.pack("!BBBBIB",controller_topic,12,topicId,1,source,len(nodeId)) + nodeId + struct.pack('!B',len(networkId)) + networkId))

        elif controller_status == 2:
            print "Received controller Goodbye. Controller Topic =  ",controller_topic

    if topic != topicId:
        continue
    print 'Receiving message', count, ' at ', st, ' ..... '
    count += 1
    print 'length of message = ', len(string)
    print 'topic =', topic
    print 'msgnum =', msgnum

    if msgnum == 1:
        msgnum1count += 1
        topic,msgnum, imsi, default_ebi, ue_ip, s1u_sgw_gtpu_teid, s1u_sgw_gtpu_ipv4, sessionid, ctopic, cid, opid = struct.unpack('!cBQBLLLQBLL',string[:40])    
        print 'imsi = ', imsi
        ipa = socketlib.inet_ntoa(struct.pack('!L',ue_ip))
        print 'ue_ip = ', ipa
        print 'default_ebi = ', default_ebi
        s1u_sgw_gtpu_ipv4a = socketlib.inet_ntoa(struct.pack('!L',s1u_sgw_gtpu_ipv4))
        print 's1u_sgw_gtpu_ipv4 = ', s1u_sgw_gtpu_ipv4a
        print 's1u_sgw_gtpu_teid = ', s1u_sgw_gtpu_teid
        print 'sessionid = ', sessionid
        print 'controller topic = ', ctopic
        print 'cid = ', cid
        print 'opid = ', opid
        responsedata = struct.pack('!BBBLL',controller_topic,4, 16, cid, opid)
        pub_socket.send("%s" % (responsedata))
        #uncomment the following lines to send a DDN for every create session message 
        #time.sleep(5)
        #pub_socket.send("%s" % (struct.pack('!BBQLLB'+str(len(nodeId))+'sB'+str(len(networkId))+'s',controller_topic,5,sessionid,cid,opid,len(nodeId),nodeId,len(networkId),networkId)))

    elif msgnum == 2:
        msgnum2count += 1
        topic, msgnum, s1u_enb_gtpu_ipv4, s1u_enb_gtpu_teid, s1u_sgw_gtpu_ipv4, sessionid, ctopic, cid, opid  = struct.unpack("!cBLLLQBLL",string[:31])
        s1u_enb_gtpu_ipv4a = socketlib.inet_ntoa(struct.pack('!L',s1u_enb_gtpu_ipv4))
        print 's1u_enb_gtpu_ipv4 = ', s1u_enb_gtpu_ipv4a
        print 'dl s1u_enb_gtpu_teid = ', s1u_enb_gtpu_teid
        print 'dl s1u_sgw_gtpu_ipv4 = ', socketlib.inet_ntoa(struct.pack('!L',s1u_sgw_gtpu_ipv4))
        print 'sessionid = ', sessionid
        print 'controller topic = ', ctopic
        print 'cid = ', cid
        print 'opid = ', opid
        responsedata = struct.pack('!BBBLL',controller_topic,4, 16, cid, opid)
        pub_socket.send("%s" % (responsedata))

    elif msgnum == 3:
        msgnum3count += 1
        topic, msgnum, sessionid, ctopic, cid, opid = struct.unpack("!cBQBLL",string[:19])
        print 'sessionid = ', sessionid
        print 'controller topic = ', ctopic
        print 'cid = ', cid
        print 'opid = ', opid
        responsedata = struct.pack('!BBBLL',controller_topic,4, 0, cid, opid)
        pub_socket.send("%s" % (responsedata))

    elif msgnum == 6:
        if(len(string)==14):
            #topic,msgnum,bufduration,bufcount,controller_topic,cid,opid = struct.unpack('!BBBHBLL',string[:14])
            topic,msgnum,controller_topic,cid,opid = struct.unpack('!BBBLL',string[:11])
            #print "dl-buffering-duration",bufduration
            #print "dl-buffering-suggested-count",bufcount
            print "Controller Topic = ",controller_topic
            print "Client id = ", cid
            print "Op Id = ", opid

    print '================'
    print 'Total = ', count, 'msgnum1 count', msgnum1count, 'msgnum2 count', msgnum2count, 'msgnum3 count', msgnum3count, 'msgnum4 count', msgnum4count,'msgnum5 count', msgnum5count, 'msgnum6 count', msgnum6count
socket.close()
