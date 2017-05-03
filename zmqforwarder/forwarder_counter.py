# coding: utf8
#Copyright © 2016 Copyright (c) Sprint, Inc. and others.  All rights reserved.
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

def run_program():
  try:
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
    while True:
        ts = time.time()
        st = datetime.datetime.fromtimestamp(ts).strftime('%Y-%m-%d %H:%M:%S')
        string = socket.recv()
        count += 1
        topic,msgnum = struct.unpack('!cB',string[:2])

        if msgnum == 1:
            msgnum1count += 1
        elif msgnum == 2:
            msgnum2count += 1
        elif msgnum == 3:
            msgnum4count += 1
        elif msgnum == 5:
            msgnum5count += 1
        elif msgnum == 6:
            msgnum6count += 1

        if (count % 10000 == 0):
            print 'Total = ', count
            print 'msgnum1 count', msgnum1count
            print 'msgnum2 count', msgnum2count
            print 'msgnum3 count', msgnum3count
            print 'msgnum4 count', msgnum4count
            print 'msgnum5 count', msgnum5count
            print 'msgnum6 count', msgnum6count
            print '==========================='

  except KeyboardInterrupt:
    print '==========================='
    print 'Final Data'
    print 'Total = ', count
    print 'msgnum1 count', msgnum1count
    print 'msgnum2 count', msgnum2count
    print 'msgnum3 count', msgnum3count
    print 'msgnum4 count', msgnum4count
    print 'msgnum5 count', msgnum5count
    print 'msgnum6 count', msgnum6count

if __name__ == '__main__':
    run_program()
