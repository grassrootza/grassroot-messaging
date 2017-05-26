#!/bin/bash
. /etc/environment
cd /opt/codedeploy-agent/deployment-root/${DEPLOYMENT_GROUP_ID}/${DEPLOYMENT_ID}/deployment-archive
cp /home/grassroot/config/application-production.properties ./src/main/resources/
./gradlew clean build -x test