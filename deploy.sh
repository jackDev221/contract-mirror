#!/bin/bash -

set -o nounset                                  # Treat unset variables as an error

if [ ! ${1} ]; then
  echo "Usage sh deploy.sh [branch_name] [profile] [port]"
  exit 1
fi

if [ ! ${JAVA_HOME} ]; then
  export JAVA_HOME=/usr/lib/jvm/java-11-openjdk-11.0.18.0.10-1.el7_9.x86_64
fi

BRANCH_NAME=${1}
PROFILE=nile
PORT=10020
VERSION=`cat VERSION`

if [ $# -ge 2 ]; then
  PROFILE=${2}
fi

if [ $# -gt 3 ]; then
  PORT=${3}
fi

if [ ! ${VERSION} ]; then
  VERSION=1.0.0
fi
JAR_NAME=contract-mirror-${VERSION}.jar

# checkout branch & build
git checkout ${BRANCH_NAME}
./gradlew :mirror:bootJar

# stop exist server
if [ -d workdir ]; then
  cd workdir
  sh run.sh stop
  cd -
  rm -rf workdir
fi

# copy to workdir
mkdir -p workdir
cp mirror/build/libs/${JAR_NAME} run.sh VERSION workdir

# run
cd workdir
sh run.sh start ${PROFILE} ${PORT}
