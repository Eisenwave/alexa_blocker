#!/usr/bin/python3

import sys

def printerr(*args, **kwargs):
    print(*args, file=sys.stderr, **kwargs)
    
PORT = 11444
BUFFER_SIZE=256
