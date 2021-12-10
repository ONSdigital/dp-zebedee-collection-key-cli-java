#!/bin/bash -eux

pushd dp-logging
  make build
  cp -r Dockerfile.concourse target/* ../build/
popd