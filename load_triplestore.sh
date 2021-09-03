#!/bin/bash

#Virtuoso example
./virtuoso/bin/isql 1111 dba dba exec="SPARQL LOAD <$1>; checkpoint;"
