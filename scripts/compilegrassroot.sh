#!/bin/bash
. /etc/environment
cd /opt/codedeploy-agent/deployment-root/${DEPLOYMENT_GROUP_ID}/${DEPLOYMENT_ID}/deployment-archive
./gradlew clean build -x test