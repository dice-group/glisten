#!/bin/bash

pkill -f fuseki
mv /glisten/main.hdt _main.hdt
rm /glisten/main.hdt.*

export JVM_ARGS=-Xmx90g
#export JAVA_HOME=/usr/lib/jvm/java-8-openjdk-amd64

rm /glisten/second.hdt
rm /glisten/second.hdt.*

if test -f "$1$2.hdt"; then
        echo "HDT file exists already. won't reload it"
        cd /glisten/hdt-java/hdt-java-cli/ && ./bin/hdtCat.sh -index $1$2.hdt /glisten/_main.hdt /glisten/main.hdt
else
        echo "Will create HDT files now."
        cd /glisten/hdt-java/hdt-java-cli/ && ./bin/rdf2hdt.sh $1$2 /glisten/second.hdt
        cd /glisten/hdt-java/hdt-java-cli/ && ./bin/hdtCat.sh -index  /glisten/second.hdt /glisten/_main.hdt /glisten/main.hdt
fi

export SERVER_MEM=90g
cd /glisten/hdt-java/hdt-fuseki/ && ./bin/hdtEndpoint.sh -q --timeout=320000 --hdt /glisten/main.hdt /ds > /dev/null &
cd /glisten/
bash -c 'while [[ "$(curl -s -o /dev/null -w ''%{http_code}'' http://localhost:3030/ds/sparql?query=SELECT+%3Fs+%7B%3Fs+%3Fp+%3Fo%7D+LIMIT+1)" != "200" ]]; do sleep 5; done'
curl http://localhost:3030/ds/sparql?query=SELECT+%28COUNT%28%2A%29+AS+%3Fco+%29+%7B%3Fs+%3Fp+%3Fo%7D

