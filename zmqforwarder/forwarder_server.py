# coding: utf8
#Copyright © 2016 Copyright (c) Sprint, Inc. and others.  All rights reserved.
#
#This program and the accompanying materials are made available under the
#terms of the Eclipse Public License v1.0 which accompanies this distribution,
#and is available at http://www.eclipse.org/legal/epl-v10.html

import zmq
import random
import sys
import time
import struct

port = "5559"
context = zmq.Context()
socket = context.socket(zmq.PUB)
socket.connect("tcp://localhost:%s" % port)
while True:
    topic = 1
    
    messagedata = struct.pack('!BQBLLL', 1,12345, 9, 2130706433, 1234, 167772160)
    
    print "%s %s" % (topic, messagedata)
    socket.send("%d %s" % (topic, messagedata))
    time.sleep(5)
