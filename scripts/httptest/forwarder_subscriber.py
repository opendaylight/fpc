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
import logging

port = "5560"
# Socket to talk to server
context = zmq.Context()
socket = context.socket(zmq.SUB)
logging.warning("Collecting updates from server...")
socket.connect ("tcp://localhost:%s" % port)
topicfilter = "1"
socket.setsockopt(zmq.SUBSCRIBE, topicfilter)
logging.warning("Listening to port %s" % port)
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
    logging.warning('Receiving message %s at %s ..... ' % (count, st))
    count += 1
    logging.warning('length of message = %s' % (len(string)))
 
    topic,msgnum = struct.unpack('!cB',string[:2])

    logging.warning('apn = %s' % topic)
    logging.warning('msgnum = %s' % msgnum)

    if msgnum == 1:
        msgnum1count += 1
        topic,msgnum, imsi, default_ebi, ue_ip, s1u_sgw_gtpu_teid, s1u_sgw_gtpu_ipv4 = struct.unpack('!cBQBLLL',string[:23])    
        logging.warning('imsi = %s' % imsi)
        ipa = socketlib.inet_ntoa(struct.pack('!L',ue_ip))
        logging.warning('ue_ip = %s' % ipa)
        logging.warning('default_ebi = %s' % default_ebi)
        s1u_sgw_gtpu_ipv4a = socketlib.inet_ntoa(struct.pack('!L',s1u_sgw_gtpu_ipv4))
        logging.warning('s1u_sgw_gtpu_ipv4 = %s' % s1u_sgw_gtpu_ipv4a)
        logging.warning('s1u_sgw_gtpu_teid = %s' % s1u_sgw_gtpu_teid)
    elif msgnum == 2:
        msgnum2count += 1
        topic, msgnum, s1u_enb_gtpu_ipv4, s1u_enb_gtpu_teid, s1u_sgw_gtpu_teid  = struct.unpack("!cBLLL",string[:14])
        s1u_enb_gtpu_ipv4a = socketlib.inet_ntoa(struct.pack('!L',s1u_enb_gtpu_ipv4))
        logging.warning('s1u_enb_gtpu_ipv4 = %s' % s1u_enb_gtpu_ipv4a)
        logging.warning('dl s1u_enb_gtpu_teid = %s' % s1u_enb_gtpu_teid)
        logging.warning('dl s1u_sgw_gtpu_teid = %s' % s1u_sgw_gtpu_teid)
    elif msgnum == 3:
        msgnum3count += 1
        topic, msgnum, default_ebi, s1u_sgw_gtpu_teid = struct.unpack("!cBBL",string[:8])
        logging.warning('default_ebi = %s' % default_ebi)
        logging.warning('s1u_sgw_gtpu_teid = %s' % s1u_sgw_gtpu_teid)
    elif msgnum == 4:
        msgnum4count += 1
        topic, msgnum, s1u_enb_gtpu_ipv4, s1u_enb_gtpu_teid, s1u_sgw_gtpu_teid  = struct.unpack("!cBLLL",string[:14])
        s1u_enb_gtpu_ipv4a = socketlib.inet_ntoa(struct.pack('!L',s1u_enb_gtpu_ipv4))
        logging.warning('s1u_enb_gtpu_ipv4 = %s' % s1u_enb_gtpu_ipv4a)
        logging.warning('ul s1u_enb_gtpu_teid = %s' % s1u_enb_gtpu_teid)
        logging.warning('ul s1u_sgw_gtpu_teid = %s' % s1u_sgw_gtpu_teid)
    elif msgnum == 5:
        msgnum5count += 1
        topic,msgnum, imsi, default_ebi, dedicated_ebi, s1u_sgw_gtpu_ipv4, s1u_sgw_gtpu_teid = struct.unpack('!cBQBBLL',string[:20])
        logging.warning('imsi = %s' % imsi)
        logging.warning('default_ebi = %s' % default_ebi)
        logging.warning('dedicated_ebi = %s' % dedicated_ebi)
        s1u_sgw_gtpu_ipv4a = socketlib.inet_ntoa(struct.pack('!L',s1u_sgw_gtpu_ipv4))
        logging.warning('s1u_sgw_gtpu_ipv4 = %s' % s1u_sgw_gtpu_ipv4a)
        logging.warning('s1u_sgw_gtpu_teid = %s' % s1u_sgw_gtpu_teid)
    elif msgnum == 6:
        msgnum6count += 1
        topic,msgnum, teid = struct.unpack('!cBL',string[:6])
        logging.warning('teid = %s' % teid)

    logging.warning('================')
    logging.warning('Total = %s, msgnum1 count %s, msgnum2 count %s, msgnum3 count %s, msgnum4 count %s, msgnum5 count %s, msgnum6 count %s' % (count, msgnum1count, msgnum2count, msgnum3count, msgnum4count, msgnum5count, msgnum6count))
socket.close()
