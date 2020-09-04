# This script sets up common environment variables and defines functions 
# for the RAP builds.

echo "***********************************************************************"

export WORKSPACE=${WORKSPACE:-$PWD}
echo "Workspace location: ${WORKSPACE}"

export MVN=${MVN:-"/opt/public/common/apache-maven-3.2.1/bin/mvn"}
echo "Maven path: ${MVN}"

export ECLIPSE_HOME=${ECLIPSE_HOME:-"/shared/rt/rap/build-runtimes/eclipse"}
echo "Eclipse location: ${ECLIPSE_HOME}"

export SIGNING_LOCATION=${SIGNING_LOCATION:-"/opt/public/download-staging.priv/rt/rap"}
echo "Signing location: ${SIGNING_LOCATION}"

export GIT_BASE=${GIT_BASE:-"git://git.eclipse.org/gitroot/rap"}
echo "Git base URL: ${GIT_BASE}"

export GIT_INCUBATOR_BASE=${GIT_INCUBATOR_BASE:-"${GIT_BASE}/incubator"}
echo "Git incubator base URL: ${GIT_INCUBATOR_BASE}"

export GIT_BRANCH=${GIT_BRANCH:-"master"}
echo "Git branch: ${GIT_BRANCH}"

export RELENG_GIT_BRANCH=${RELENG_GIT_BRANCH:-"master"}
echo "Releng Git branch: ${RELENG_GIT_BRANCH}"

export RAP_REPOSITORY=${RAP_REPOSITORY:-"http://download.eclipse.org/rt/rap/nightly/runtime/"}
echo "RAP Runtime p2 repository: ${RAP_REPOSITORY}"

export REPOSITORY_BASE_PATH=${REPOSITORY_BASE_PATH:-"/shared/rt/rap/incubator/nightly"}
echo "p2 repository base path: ${REPOSITORY_BASE_PATH}"

export NUM_TO_KEEP=${NUM_TO_KEEP:-"5"}
echo "Number of p2 repositories to keep: ${NUM_TO_KEEP}"

export MAVEN_LOCAL_REPO_PATH=${MAVEN_LOCAL_REPO_PATH:-"/shared/rt/rap/m2/repository"}
echo "Local Maven repository location: ${MAVEN_LOCAL_REPO_PATH}"


######################################################################
# functions used in the build

p2AddContent() {
  if [ $# -lt 3 ]
  then
    echo "Usage: `p2AddContent` SOURCE_REPO_URL TARGET_REPO_URL REPO_NAME"
    exit 1
  fi

  SOURCE="${1}"
  DESTINATION="${2}"
  DESTINATION_NAME="${3}"

  ${ECLIPSE_HOME}/eclipse -nosplash -verbose \
    -application org.eclipse.equinox.p2.metadata.repository.mirrorApplication \
    -source ${SOURCE} \
    -destination ${DESTINATION} \
    -destinationName ${DESTINATION_NAME}
  ${ECLIPSE_HOME}/eclipse -nosplash -verbose \
    -application org.eclipse.equinox.p2.artifact.repository.mirrorApplication \
    -source ${SOURCE} \
    -destination ${DESTINATION} \
    -destinationName ${DESTINATION_NAME}
}

