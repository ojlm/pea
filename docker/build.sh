#!/usr/bin/env bash
cd ..
sbt clean dist
cd target/universal
unzip pea-*.zip
rm pea-*.zip
mv pea-* pea
cd ../../docker
_tag=$1
if [ -z "${_tag}" ]; then
    _tag=latest
fi
docker build --file ./Dockerfile -t "asurapro/pea:${_tag}" ../
docker push asurapro/pea
