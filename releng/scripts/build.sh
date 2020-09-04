#!/bin/bash
#
# General build script for all RAP Incubator builds
#
#set -x

######################################################################
# initial argument checks
if [ $# -lt 1 ]
then
  echo "Usage: `basename $0` COMPONENT_NAME [N|S]"
  exit 1
fi
BUILD_TYPE=${2:-"N"}

######################################################################
# setup and initialization
SCRIPTS_DIR=$(dirname $(readlink -nm $0))
if [ -e "${SCRIPTS_DIR}/init-environment.sh" ]
then
  . ${SCRIPTS_DIR}/init-environment.sh
else
  echo "init-environment.sh script not found at ${SCRIPTS_DIR}"
  exit 1
fi

COMPONENT_NAME=${1}
REPOSITORY_NAME="org.eclipse.rap.incubator.${COMPONENT_NAME}"
BUILD_PROJECT_PATH="releng/org.eclipse.rap.${COMPONENT_NAME}.build"

SIGNPROFILE=""
if [ X"$BUILD_TYPE" = XS ]
then
  SIGNPROFILE="-Peclipse-sign"
fi

######################################################################
# arbitrary checks
# TODO mknauer - check if variable is set
test -d "${WORKSPACE}" || exit 1

######################################################################
# configuration check and debug
echo "***********************************************************************"
echo "Running `basename $0`"
echo "RAP component name: ${COMPONENT_NAME}"
echo "Repository name: ${REPOSITORY_NAME}"
echo "Build project path: ${BUILD_PROJECT_PATH}"
echo "Build type: ${BUILD_TYPE}"
echo "Sign Profile: ${SIGNPROFILE}"
echo "RAP Runtime library: ${RAP_REPOSITORY}"

######################################################################
# clean up local Maven repository to circumvent p2 cache problems
for II in .cache .meta p2 ; do
  echo "Remove directory ${MAVEN_LOCAL_REPO_PATH}/${II}" 
  rm -r ${MAVEN_LOCAL_REPO_PATH}/${II}
done

######################################################################
# setup tmp directory for JUnit tests and p2 operations
TEMPDIRECTORY="${WORKSPACE}/tmp"
mkdir -p "${TEMPDIRECTORY}"

######################################################################
# git clone build repository
RELENG_REPOSITORY_NAME="org.eclipse.rap.incubator.releng"
REPOSITORY=${GIT_INCUBATOR_BASE}/${RELENG_REPOSITORY_NAME}
echo "Git clone of releng repository ${REPOSITORY} into ${WORKSPACE}"
cd ${WORKSPACE}
rm -rf ${WORKSPACE}/${RELENG_REPOSITORY_NAME:-"DUMMYDIRECTORY"}
git clone --branch=${RELENG_GIT_BRANCH} ${REPOSITORY} ${RELENG_REPOSITORY_NAME}

######################################################################
# git clone
REPOSITORY=${GIT_INCUBATOR_BASE}/${REPOSITORY_NAME}
echo "Git clone of ${REPOSITORY} into ${WORKSPACE}"
cd ${WORKSPACE}
rm -rf ${WORKSPACE}/${REPOSITORY_NAME:-"DUMMYDIRECTORY"}
git clone --branch=${GIT_BRANCH} ${REPOSITORY} ${REPOSITORY_NAME}

######################################################################
# execute build
BUILD_DIRECTORY=${WORKSPACE}/${REPOSITORY_NAME}/${BUILD_PROJECT_PATH}
echo "Starting build in ${BUILD_DIRECTORY}"
cd ${BUILD_DIRECTORY}
${MVN} -e clean package $SIGNPROFILE -Dmaven.repo.local=${MAVEN_LOCAL_REPO_PATH} -Drap-repository=${RAP_REPOSITORY} -Djava.io.tmpdir="$TEMPDIRECTORY"
EXITCODE=$?
if [ "$EXITCODE" != "0" ]; then
  echo "Maven exited with error code " + ${EXITCODE}
  exit ${EXITCODE}
fi

######################################################################
# extract build timestamp
## TODO mknauer Find a better way to determine the TIMESTAMP
REPOSITORY_DIRECTORY=${WORKSPACE}/${REPOSITORY_NAME}/${BUILD_PROJECT_PATH}/repository/target/repository
VERSION=$(ls ${REPOSITORY_DIRECTORY}/features/org.eclipse.rap.*.feature_*.jar | sed 's/.*-v\([0-9]\+\-[0-9]\+\).*/\1/' | sort -nr | head -1)
TIMESTAMP=$(ls ${REPOSITORY_DIRECTORY}/features/org.eclipse.rap.*.feature_*.jar | sed 's/.*-v\([0-9]\+\-[0-9]\+\).*/\1/' | sort -nr | head -1)
echo "Version is ${VERSION}"
echo "Timestamp is ${TIMESTAMP}"
test -n "${VERSION}" || exit 1
test -n "${TIMESTAMP}" || exit 1
######################################################################
# copy repository to target location if new version available
COMPONENT_DIRECTORY=${REPOSITORY_BASE_PATH}/${COMPONENT_NAME}
echo "Copy new ${TIMESTAMP} repository of ${COMPONENT_NAME} to ${COMPONENT_DIRECTORY}" 
if [ -d "${COMPONENT_DIRECTORY}/${TIMESTAMP}" ] ; then
  echo "Build already exists in ${COMPONENT_DIRECTORY}. Nothing to do."
  echo "Stopping build ${TIMESTAMP} of ${COMPONENT_NAME} ${VERSION}."
  exit 0
fi
mkdir -p ${COMPONENT_DIRECTORY}
cd ${COMPONENT_DIRECTORY}
cp -r ${REPOSITORY_DIRECTORY} ${COMPONENT_DIRECTORY}/${TIMESTAMP}

######################################################################
# clean-up target location
echo "Removing old repositories from ${COMPONENT_DIRECTORY}, keeping the ${NUM_TO_KEEP} most recent"
cd ${COMPONENT_DIRECTORY}
II=0
for DIR in `ls -r ${COMPONENT_DIRECTORY} | grep '.*[0-9]$'`; do
  if [ -d ${COMPONENT_DIRECTORY}/${DIR} ]; then
    if [ $II -ge $NUM_TO_KEEP ]; then
      echo "Removing outdated repository ${DIR}"
      rm -r ${COMPONENT_DIRECTORY}/${DIR} || exit 1
    fi
    let II=II+1
  fi
done

######################################################################
# create final p2 repository
echo "Creating p2 repository in ${COMPONENT_DIRECTORY}"
cd ${COMPONENT_DIRECTORY}
rm -r content.jar artifacts.jar plugins/ features/
for II in `ls -dtr [0-9]*-[0-9]*`; do
  echo "Adding data from ${II} to ${COMPONENT_DIRECTORY}"
  p2AddContent ${COMPONENT_DIRECTORY}/${II} ${COMPONENT_DIRECTORY} ${COMPONENT_NAME}
done

######################################################################
# build done
echo "Build ${TIMESTAMP} of ${COMPONENT_NAME} ${VERSION} done."
echo "***********************************************************************"
