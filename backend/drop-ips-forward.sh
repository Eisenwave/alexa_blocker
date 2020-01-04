#!/bin/bash

for IP in $@; do
    printf "dropping $IP\n"
    iptables -I FORWARD -d "$IP" -j DROP
    iptables -I FORWARD -s "$IP" -j DROP
done
