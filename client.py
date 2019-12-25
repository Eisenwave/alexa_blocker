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
        message = 'u'
        printerr('sending "{}"'.format(message))
        sock.sendall(message.encode())

    finally:
        printerr('closing socket')
        sock.close()
