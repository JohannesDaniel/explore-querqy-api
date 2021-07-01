#!/bin/bash

# DOC: solr-precreate does not work here as external libs are not considered

docker run \
--publish 8983:8983 \
--mount type=bind,source="$(pwd)"/docker/querqy,target=/solr/querqy \
--mount type=bind,source="$(pwd)"/docker/conf,target=/solr/sample-index/conf \
--rm \
--name explore_querqy \
solr:8.9 \
bash -c "\
((sleep 5; bin/solr create -c sample-index -d /solr/sample-index) &) && \
solr-foreground"
