# coding: utf8
#!/usr/bin/python

#Copyright © 2016 - 2017 Copyright (c) Sprint, Inc. and others.  All rights reserved.
#
#This program and the accompanying materials are made available under the
#terms of the Eclipse Public License v1.0 which accompanies this distribution,
#and is available at http://www.eclipse.org/legal/epl-v10.html

from SocketServer import ThreadingMixIn
import BaseHTTPServer, SimpleHTTPServer
import SocketServer
import logging
import cgi
import ssl
import json
import sys
import datetime
import time
import requests
import thread
from multiprocessing.pool import ThreadPool
import signal
import threading

RUN = True

def signal_handler(signal, frame):
    global RUN
    RUN = False
    time.sleep(2)
    sys.exit(0)

signal.signal(signal.SIGINT, signal_handler)

###########################################################
# Change these three values depending on your test case
NO_OF_UEs = 10000
SESSION_ID_START = 1
DPN_ID = "dpn1"
###########################################################

NO_OF_RESPONSES = 0
NO_OF_NOTIFS = 0
CLIENT_ID = None
httpd = None
PORT = 9996

def responseStream():
    global CLIENT_ID 
    global NO_OF_RESPONSES
    global RUN
    count = 0
    print "Response Stream initiated"
    s = requests.Session()
    jsn = {}
    jsn['client-uri'] = "http://127.0.0.1:9996/request"
    headers = {'Connection':'keep-alive','Accept':'*/*'}
    r = requests.post('http://127.0.0.1:8070/response', headers=headers, stream=True,data=json.dumps(jsn),timeout=500)
    for line in r.iter_lines():
        decoded_line = line.decode('utf-8')
        if "supported-features" in decoded_line:
            count = count - 1
            decoded_line = (decoded_line.split(':',1)[1]).strip()
            output = json.loads(decoded_line)
            CLIENT_ID = output['output']['client-id']
            print("Client id = "+str(CLIENT_ID))
            continue
        count = count+1
        if(count % 2 == 0):
            NO_OF_RESPONSES = NO_OF_RESPONSES + 1
        if not RUN:
            break;
th = threading.Thread(target=responseStream)
th.start()

def stats():
    global NO_OF_RESPONSES
    global RUN
    count = 0
    while True:
        print count," | No of Responses: ",NO_OF_RESPONSES, "\tNo of Notifications: ",NO_OF_NOTIFS
        count = count + 1
        time.sleep(1)
        if not RUN:
            break


def notificationStream(clientId=None):
    global NO_OF_NOTIFS
    global RUN
    print "Notification Stream initiated"
    s = requests.Session()
    if clientId != None:
        jsn = {}
        jsn['client-id']=str(clientId)
        headers = {'Connection':'keep-alive','Accept':'*/*'}
        r = s.post('http://127.0.0.1:8070/notification', headers=headers, stream=True, data=json.dumps(jsn),timeout=500)
        count = 0
        for line in r.iter_lines():
            decoded_line = line.decode('utf-8')
            count = count + 1
            if count%2 == 0:
                NO_OF_NOTIFS = NO_OF_NOTIFS + 1
                if not RUN:
                    break
            #print(line)

class ThreadingServer(ThreadingMixIn, BaseHTTPServer.HTTPServer):
    pass

class ServerHandler(SimpleHTTPServer.SimpleHTTPRequestHandler):

    def do_GET(self):
        global CLIENT_ID
        global RUN
        #SimpleHTTPServer.SimpleHTTPRequestHandler.do_GET(self)
        self.protocol_version = "HTTP/1.1"
        self.send_response(200)
        self.send_header('Content-Type', 'text/event-stream')
        self.send_header('Transfer-Encoding', 'chunked')
        self.end_headers()
        time.sleep(0.005)
        string = "event:"+"application/json;/restconf/operations/fpc:register_client\ndata:"+'{"input":{"client-id":"1","tenant-id":"default","supported-features":["urn:ietf:params:xml:ns:yang:fpcagent:fpc-bundles","urn:ietf:params:xml:ns:yang:fpcagent:operation-ref-scope","urn:ietf:params:xml:ns:yang:fpcagent:fpc-agent-assignments","urn:ietf:params:xml:ns:yang:fpcagent:instruction-bitset"]}}'+"\r\n"
        strlen = (hex(len(string)))[2:]
        self.wfile.write(strlen+"\r\n"+string+"\r\n")
        self.wfile.flush()
        time.sleep(0.005)
        while CLIENT_ID == None:
            time.sleep(0.005)
        th1 = threading.Thread(target=notificationStream,args=(CLIENT_ID,))
        th1.start()
        th5 = threading.Thread(target=stats)
        th5.start()
        for i in range(0,NO_OF_UEs):
            inputStr = "{'input':{'op-id':'"+str(i+SESSION_ID_START)+"','contexts':[{'instructions':{'instr-3gpp-mob':'session uplink'},'context-id':"+str(i+SESSION_ID_START)+",'dpn-group':'foo','delegating-ip-prefixes':['192.168.1.5/32'],'ul':{'tunnel-local-address':'192.168.1.1','tunnel-remote-address':'10.1.1.1','mobility-tunnel-parameters':{'tunnel-type':'ietf-dmm-threegpp:gtpv1','tunnel-identifier':'1111'},'dpn-parameters':{}},'dl':{'tunnel-local-address':'192.168.1.1','tunnel-remote-address':'10.1.1.1','mobility-tunnel-parameters':{'tunnel-type':'ietf-dmm-threegpp:gtpv1','tunnel-identifier':'2222'},'dpn-parameters':{}},'dpns':[{'dpn-id':'"+DPN_ID+"','direction':'uplink','dpn-parameters':{}}],'imsi':'9135551234','ebi':'5','lbi':'5'}],'client-id':'"+str(CLIENT_ID)+"','session-state':'complete','admin-state':'enabled','op-type':'create','op-ref-scope':'op'}}"
            string = "event:"+str(i)+"application/json;/restconf/operations/ietf-dmm-fpcagent:configure\ndata:"+inputStr+"\r\n"
            strlen = (hex(len(string)))[2:]
            self.wfile.write(strlen+"\r\n"+string+"\r\n")
            self.wfile.flush()
            time.sleep(0.00001)
            inputStr = "{'input':{'op-id':'"+str(i+(SESSION_ID_START+NO_OF_UEs))+"','contexts':[{'instructions':{'instr-3gpp-mob':'downlink'},'context-id':"+str(i+SESSION_ID_START)+",'dpn-group':'site1-l3','delegating-ip-prefixes':['192.168.1.5/32'],'ul':{'tunnel-local-address':'192.168.1.1','tunnel-remote-address':'10.1.1.2','mobility-tunnel-parameters':{'tunnel-type':'ietf-dmm-threegpp:gtpv1','tunnel-identifier':'3333'},'dpn-parameters':{}},'dl':{'tunnel-local-address':'192.168.1.1','tunnel-remote-address':'10.1.1.4','mobility-tunnel-parameters':{'tunnel-type':'ietf-dmm-threegpp:gtpv1','tunnel-identifier':'4444'},'dpn-parameters':{}},'dpns':[{'dpn-id':'"+DPN_ID+"','direction':'uplink','dpn-parameters':{}}],'imsi':'9135551234','ebi':'5','lbi':'5'}],'client-id':'"+str(CLIENT_ID)+"','session-state':'complete','admin-state':'enabled','op-type':'update','op-ref-scope':'op'}}"
            string = "event:"+str(i)+"application/json;/restconf/operations/ietf-dmm-fpcagent:configure\ndata:"+inputStr+"\r\n"
            strlen = (hex(len(string)))[2:]
            self.wfile.write(strlen+"\r\n"+string+"\r\n")
            self.wfile.flush()
            time.sleep(0.00001)
        time.sleep(5)
        for i in range(0,NO_OF_UEs):
            inputStr = '{"input":{"op-id":"'+str(i+(SESSION_ID_START+NO_OF_UEs+NO_OF_UEs))+'","targets":[{"target":"/ietf-dmm-fpcagent:tenants/tenant/default/fpc-mobility/contexts/'+str(i+SESSION_ID_START)+'"}],"client-id":"'+str(CLIENT_ID)+'","session-state":"complete","admin-state":"enabled","op-type":"delete","op-ref-scope":"none"}}'
            string = "event:"+str(i)+"application/json;/restconf/operations/ietf-dmm-fpcagent:configure\ndata:"+inputStr+"\r\n"
            strlen = (hex(len(string)))[2:]
            self.wfile.write(strlen+"\r\n"+string+"\r\n")
            self.wfile.flush()
            time.sleep(0.00001)
        time.sleep(5)
        string = "event:"+"application/json;/restconf/operations/fpc:deregister_client\ndata:"+'{"input":{"client-id":"'+str(CLIENT_ID)+'"}}'+"\r\n"
        strlen = (hex(len(string)))[2:]
        self.wfile.write(strlen+"\r\n"+string+"\r\n")
        self.wfile.flush()
        time.sleep(5)

def startServer():
    global httpd
    global RUN
    print "Request Stream initiated"
    httpd = ThreadingServer(("", PORT), ServerHandler)
    httpd.serve_forever()
    while RUN:
        pass
    httpd.server_close()
    sys.exit(0)

th2 = threading.Thread(target=startServer)
th2.start()