#!/bin/bash

#Virtuos example
/virtuoso/virtuoso-opensource/bin/isql 1111 dba dba exec="ld_dir($dir, $file, 'http://example.com') ; rdf_loader_run(); checkpoint; SPARQL SELECT COUNT(*) FROM <http://example.com> {?s ?p ?o} "
