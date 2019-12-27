#!/usr/bin/python3

import common
from common import printerr

import socket
import sys

def loop():
    message = input("Command: ")
    if message == 'q':
        exit(0)
    
    sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    sock.connect(sadr) 

    try:
        sock.sendall(message.encode())
        response = sock.recv(1)
        printerr("Response:", response.decode(), '\n')
    finally:
        sock.close()

if __name__ == "__main__":
    host = 'localhost' if len(sys.argv) < 2 else sys.argv[1]
    sadr = (host, common.PORT)
    printerr('Connecting to {}:{}'.format(sadr[0], sadr[1]))
    printerr('Possible commands are', ['u', 'a', 'o', 'e', 'm'], "or q for quitting the client", '\n')
    
    while True:
        loop()
