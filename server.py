#!/usr/bin/python3

# local dependencies
import common
from common import printerr

# system dependencies
import os
import socket
import sys

# INTRODUCTION
#
# This server is meant to be run on the raspi acting as a network bridge.
# It implements a simple TCP protocol for blocking and unabling connections.
#
# For security reasons, the server only accepts commands from IP-addresses in the local network.
# Connections from localhost are also allowed for testing purposes.
#
# DO NOT run this server on a public wifi hotspot or otherwise unsafe network.
#
#
# PROTOCOL
#
# The protocol allows any client, such as an app to block and unblock their Alexa's network bridge.
# For this purpose, single, lower-case characters are transmitted over TCP for the commands.
#
# u: - Unblock
#    - unblocks all the blocked IP-addresses
# a: - All blocked
#    - blocks all IP-addresses relevant to Alexa
# e: - Essentials blocked
#    - blocks only the AVS-IP which is being used to provide the Alexa service
# o: - Obsoletes blocked
#    - blocks all IP-addresses that are not vital to Alexa's functions, but which Alexa uses
#
# The protocol is stateless.
# Before each command, all IPs are effectively unblocked and then re-blocked according to the command.

def unblock():
    os.system("./alexa-block.sh reset")

def block_all():
    os.system("./alexa-block.sh all")

def block_essentials():
    os.system("./alexa-block.sh essential")

def block_obsoletes():
    os.system("./alexa-block.sh obsolete")

def interpret(command):
    if command == 'u':
        unblock()
    elif command == 'a':
        block_all()
    elif command == 'e':
        block_essentials()
    elif command == 'o':
        block_obsoletes
    else:
        return False
    return True

def read_all_data(connection, client_address):
    result = bytearray()
    try:
        # Receive the data in small chunks and retransmit it
        data = connection.recv(common.BUFFER_SIZE)
        while data:
            result += data
            data = connection.recv(common.BUFFER_SIZE)
    except x:
        printerr('ERROR: "{}"'.format(x))
    finally:
        connection.close()
        return result

    
def parse_ip(ip_str):
    strings = ip_str.split('.')
    result = []
    for s in strings:
        result.append(int(s))
    return result


def is_loopback(ip_bytes):
    return ip_bytes[0] == 127


def is_private(ip_bytes):
    if ip_bytes[0] == 10:
        return True
    if ip_bytes[0] == 172 and (ip_bytes[1] in range(16, 32)):
        return True
    if ip_bytes[0] == 192 and ip_bytes[1] == 168:
        return True
    return False


def is_untrusted(ip_str):
    ip_bytes = parse_ip(ip_str)
    return not is_loopback(ip_bytes) and not is_private(ip_bytes)


def loop():
    connection, client_adr = sock.accept()
    
    ip_str, port = client_adr
    client_adr_str = ip_str + ":" + str(port)
    
    if is_untrusted(ip_str):
        printerr("rejected", client_adr_str)
        return None
    
    sys.stderr.write(client_adr_str + "$ ")
    
    command = read_all_data(connection, client_adr)
    if command != None:
        command_str = command.decode()
        printerr('"{}"'.format(command_str))
        if not interpret(command_str):
            printerr('error: unknown command')


if __name__ == "__main__":
    server_adr = ('localhost', common.PORT)
    
    sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    sock.bind(server_adr)
    sock.listen(1)
    
    printerr('started server on {}:{}'.format(server_adr[0], server_adr[1]))

    while True:
        loop()
