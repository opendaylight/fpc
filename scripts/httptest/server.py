#!/usr/bin/python

#Copyright © 2016 Copyright (c) Sprint, Inc. and others.  All rights reserved.
#
#This program and the accompanying materials are made available under the
#terms of the Eclipse Public License v1.0 which accompanies this distribution,
#and is available at http://www.eclipse.org/legal/epl-v10.html

"""
Save this file as server.py
>>> python server.py 0.0.0.0 8001
serving on 0.0.0.0:8001

or simply

>>> python server.py
Serving on localhost:8000

>>> pyhton server.py ssl
Serving https server on localhost:8000

You can use this to test GET and POST methods.

"""

import BaseHTTPServer, SimpleHTTPServer
import SocketServer
import logging
import cgi
import ssl

import sys

if len(sys.argv) > 2:
    PORT = int(sys.argv[2])
    I = sys.argv[1]
elif len(sys.argv) > 1 and sys.argv[1] != 'ssl':
    PORT = int(sys.argv[1])
    I = ""
else:
    PORT = 9997
    I = ""

count = 0

class ServerHandler(SimpleHTTPServer.SimpleHTTPRequestHandler):

    def do_GET(self):
        logging.warning("======= GET STARTED =======")
        logging.warning(self.headers)
        SimpleHTTPServer.SimpleHTTPRequestHandler.do_GET(self)

    def do_POST(self):
        global count
        logging.warning("======= POST STARTED =======")
        logging.warning(self.headers)
        if self.headers['Content-Type']=='application/json; charset=UTF-8':
            logging.warning("======= POST BODY =======")
            content_len = int(self.headers.getheader('content-length', 0))
            post_body = self.rfile.read(content_len)
            logging.warning(post_body)
            logging.warning("======= POST END =======")
            count += 1
            logging.warning("POST msg count: "+str(count))
        else:
            logging.warning("Content-Type is not application/json; charset=UTF-8'")
        SimpleHTTPServer.SimpleHTTPRequestHandler.do_GET(self)
        
Handler = ServerHandler

if len(sys.argv) > 1 and sys.argv[1] == 'ssl':
    isSSL = 'https'
    httpd = BaseHTTPServer.HTTPServer(('', PORT),Handler)
    httpd.socket = ssl.wrap_socket (httpd.socket, certfile='./mycert.pem', server_side=True)
else:
    isSSL = 'http'
    httpd = SocketServer.TCPServer(("", PORT), Handler)

print "@rochacbruno Python http server version 0.1 (for testing purposes only)"
print "Serving at: %(protocol)s://%(interface)s:%(port)s" % dict(interface=I or "localhost", port=PORT, protocol=isSSL)
print "Ctrl + C to abort..."
httpd.serve_forever()
