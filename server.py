#!/usr/bin/python3

import common
from common import printerr

import os
import socket
import sys

def blockCompletely():
    os.system("alexa-block.sh")

def onConnection(connection, client_address):
    try:
        # Receive the data in small chunks and retransmit it
        data = connection.recv(16)
        while data:
            printerr('received {}'.format(data))
            connection.sendall(data)
            data = connection.recv(16)
        printerr('no more data from ', client_address)
        
    finally:
        # Clean up the connection
        connection.close()

if __name__ == "__main__":
    sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)

    sadr = ('localhost', common.PORT)
    
    sock.bind(sadr)
    printerr('started server on {}:{}'.format(sadr[0], sadr[1]))

    sock.listen(1)

    while True:
        printerr('waiting for a connection')
        connection, cadr = sock.accept()
        printerr('connection from', cadr)
        onConnection(connection, cadr)
