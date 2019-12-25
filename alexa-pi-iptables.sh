#!/bin/bash

#!(ipv6.src == fe80::ca4c:75ff:fe77:17d9) and !arp and !icmp and !mdns and !igmp and !(tcp.flags.reset == 1 or tcp.flags.syn == 1)

# enable iptables with bridge
modprobe br_netfilter
echo '1' > /proc/sys/net/bridge/bridge-nf-call-iptables

iptables -F INPUT
iptables -F OUTPUT
iptables -F FORWARD

if [[ $1 = 'reset' ]]; then
    exit 0
fi

function drop-ips {
  for IP in $@; do
    printf "dropping $IP\n"
    iptables -I INPUT  -s "$IP" -j DROP
    iptables -I FORWARD -d "$IP" -j DROP
    iptables -I FORWARD -s "$IP" -j DROP
    iptables -A OUTPUT -d "$IP" -j DROP
  done
}

function drop-ips-responses {
  for IP in $@; do
    printf "dropping responses from $IP\n"
    iptables -I INPUT  -s "$IP" -j DROP
    iptables -I FORWARD -s "$IP" -j DROP
  done
}

# WHAT:  drop 54.165.25.10, 52.87.73.124, 52.55.98.56, 35.172.157.8
# DOMAIN: fireoscaptiveportal.com
# BRIEF: Amazon basic HTTPS server with nginx
#        Echo sends HTTP queries to this server
drop-ips 54.165.25.10 \
         52.87.73.124 \
         52.55.98.56 \
         35.172.157.8

# WHAT: drop 35.172.157.8
# DOMAIN: ec2-35-172-157-8.compute-1.amazonaws.com
drop-ips 35.172.157.8

# WHAT: 52.204.41.53
# DOMAIN: ec2-52-204-41-53.compute-1.amazonaws.com
drop-ips 52.204.41.53

# WHAT: drop 52.206.153.117
# DOMAIN: ec2-52-206-153-117.compute-1.amazonaws.com
drop-ips 52.206.153.117

# WHAT: 52.206.153.117
# DOMAIN: ec2-52-206-153-117.compute-1.amazonaws.com
drop-ips 52.206.153.117

# WHAT: drop 52.7.82.103
# DOMAIN: ec2-52-7-82-103.compute-1.amazonaws.com
drop-ips 52.7.82.103

# WHAT: drop 3.221.123.116
# DOMAIN: ec2-3-221-123-116.compute-1.amazonaws.com 
drop-ips 3.221.123.116

# WHAT: drop 52.95.122.231
# DOMAIN: ?
drop-ips 52.95.122.231

# WHAT: drop 52.46.133.39
#            52.46.159.66
#            52.46.145.58
# DOMAIN: device-metrics-us.amazon.com
drop-ips device-metrics-us.amazon.com \
         52.46.133.39 \
         52.46.159.66 \
         52.46.145.58 \
         54.239.19.125 \
         54.239.31.37 \
         52.46.159.73 \
         52.46.156.47 \
         52.94.229.76 \
         52.94.229.215
         
# WHAT: drop 52.94.232.195, 54.239.29.0
# DOMAIN: unagi-na.amazon.com
# BRIEF: one of the hot domains when talking to Echo (seems optional)
drop-ips 52.94.232.195 \
         54.239.29.0 \
         54.239.26.255
         
# WHAT: drop 143.204.214.231
# DOMAIN: d1gsg05rq1vjdw.cloudfront.net
drop-ips d1gsg05rq1vjdw.cloudfront.net \
         143.204.214.231
         
# WHAT: drop 99.86.5.195
# DOMAIN: d90nnyvqgmkzx.cloudfront.net
drop-ips 99.86.5.195
         
# WHAT: drop 34.199.205.170
# DOMAIN: prod.amcs-tachyon.com
drop-ips prod.amcs-tachyon.com \
         34.199.205.170 \
         52.4.218.158 \
         54.209.247.213 \
         3.217.70.30

if [[ "$1" == "obsolete" ]]; then
    printf 'Blocked only obsolete IPs\n'
    exit 0
fi

if [[ "$1" == responses ]]; then
    DROP_CMD='drop-ips-responses'
else
    DROP_CMD='drop-ips'
fi
 
# WHAT: drop 52.95.122.231
# BRIEF: one of the hot IPs when talking to Echo (seems to be necessary)
# (probably also bob-dispath)
$DROP_CMD 52.95.122.231 \
          52.95.117.89 \
         
         
# WHAT: drop
# DOMAIN: Source: bob-dispatch-prod-eu.amazon.com
# BRIEF: one of the hot domains when talking to Echo (seems necessary)
$DROP_CMD bob-dispatch-prod-eu.amazon.com \
          52.95.119.186 \
          52.95.117.89 \
          52.95.121.5 \
          52.95.115.208 \
          52.95.113.144
         
printf 'Blocked all known IPs\n' 

