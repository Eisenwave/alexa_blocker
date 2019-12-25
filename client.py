#!/usr/bin/python3

import common
from common import printerr

import socket
import sys

if __name__ == "__main__":
    sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)

    sadr = ('localhost', common.PORT)
    printerr('connecting to {}:{}'.format(sadr[0], sadr[1]))
    sock.connect(sadr) 

    try:
        message = 'This is the message.  It will be repeated.'
        printerr('sending "{}"'.format(message))
        sock.sendall(message.encode())

        amount_received = 0
        amount_expected = len(message)
        
        while amount_received < amount_expected:
            data = sock.recv(16)
            amount_received += len(data)
            printerr('received "{}"'.format(data))

    finally:
        printerr('closing socket')
        sock.close()
