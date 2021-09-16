#!/bin/bash

#we need java 11 set if not set.
#export JAVA_HOME=/usr/lib/jvm/java-11-openjdk-amd64
export JVM_ARGS=-Xmx60g
pkill -f fuseki

echo "LOAD <file://$1$2>" > ./update123.query
./apache-jena-4.2.0/bin/tdbupdate --loc=./TESTDB --update=./update123.query
rm ./update123.query

./apache-jena-fuseki-4.2.0/fuseki-server -q --loc=./TESTDB/ /ds > /dev/null &

#Wait for fuseki to be ready
bash -c 'while [[ "$(curl -s -o /dev/null -w ''%{http_code}'' localhost:3030)" != "200" ]]; do sleep 5; done'
