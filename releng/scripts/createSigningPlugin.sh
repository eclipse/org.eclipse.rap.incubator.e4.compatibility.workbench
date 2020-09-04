#!/bin/bash
# This is a script with commands that helps to create the Maven signing
# plug-in for eclipse.org. It is *not* a sophisticated script that checks 
# for errors.
#
# It clones the source code from the local eclipse.org Git repository,
# builds the jar file, and puts the result into the local Maven repository
# that is used in the RAP build process.

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

######################################################################
# clone from CBI Git repository
git clone git://git.eclipse.org/gitroot/cbi/org.eclipse.cbi.maven.plugins.git

######################################################################
# compile everything
cd org.eclipse.cbi.maven.plugins/eclipse-jarsigner-plugin/
${MVN} clean install

######################################################################
# push results into local Maven repository eclipse-jarsigner-plugin-
${MVN} install:install-file \
  -Dfile=target/eclipse-jarsigner-plugin-1.0.0-SNAPSHOT.jar \
  -DgroupId=org.eclipse.cbi.maven.plugins \
  -DartifactId=eclipse-jarsigner-plugin \
  -Dversion=1.0.0-SNAPSHOT \
  -Dpackaging=maven-plugin \
  -Dmaven.repo.local=${MAVEN_LOCAL_REPO_PATH}

