#
# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
# 
#   http://www.apache.org/licenses/LICENSE-2.0
# 
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.
#

import os, common, sys, traceback
from proton import *
from threading import Thread, Event
from time import sleep, time
from common import Skipped
from messenger import Test

def clientAwol(address):
    client = Messenger()
    msg = Message()
    msg.address = address
    msg.body = "request from awol"
    client.put(msg)
    print "\nawol sends one"
    client.send(-1)
    for i in range(3):
      print "\n\nawol recvs one"
      client.recv(-1)
      client.get(msg)
      print "\n\nawol goes awol with", msg.body
      client.accept()
    sys.exit(1)
    print "\nawol is dead?"

def forkClientAwol(address):
    from multiprocessing import Process
    p = Process(target=clientAwol, args=(address,))
    p.start()
    print "\nStarted awol"
    return p


class DissapearingMessengerTest(Test):

    def dispatch(self, msg):
        if msg.reply_to:
            if msg.body == "request from awol":
              for a in range(10):
                msg2 = Message()
                msg2.address = msg.reply_to
                msg2.body = ("reply %d for awol " % a ) * 10
                print "server sends awol one", msg2.body
                t = self.server.put(msg2)
                self.trackers.append(t)
                pass
            else:
              msg.address = msg.reply_to
              msg.body = "reply to " + msg.body
              print "server sends one ", msg.body
              t = self.server.put(msg)
              self.trackers.append(t)
        old = self.trackers[:-10]
        if old:
          map(self.server.settle, old)
          self.trackers[:-10]=[]

    def testClientAwol(self):
        self.server.outgoing_window = 10
        self.server.incoming_window = 10
        self.server_credit = -1
        self.start()
        self.client.stop()
        self.trackers = []
        import time
        print "\nStarting awol"
        p = forkClientAwol(self.address)
        print "waitin awol to die"
        p.join();
        print "\nawol is dead"
        self.client = Messenger("client")
        self.client.start()
        for i in range(5):
          msg = Message()
          msg.address = self.address
          msg.body = "message %d from client" % i
          self.client.put(msg)
          self.client.send(1)
          self.client.recv(1)
          msg2 = Message()
          self.client.get(msg2)
          print "client sent", msg.body, "server replied with", msg2.body
        print "\n\ntry connecting to", self.address
        time.sleep(1)


