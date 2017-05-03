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

port = "5570"
context = zmq.Context()
socket = context.socket(zmq.DEALER)
socket.connect("tcp://127.0.0.1:%s" % port)

topic = 1
   
with open('data.txt', 'r') as myfile:
    messagedata=myfile.read().replace('\n', '')

poll = zmq.Poller()
poll.register(socket, zmq.POLLIN)
reqs = 0

print "%s %s" % (topic, messagedata)
socket.send_string("%s" % (messagedata))

sockets = dict(poll.poll(1000))
if socket in sockets:
    msg = socket.recv()
    print('\n\nClient received: %s' % (msg))

socket.close()
context.term()
