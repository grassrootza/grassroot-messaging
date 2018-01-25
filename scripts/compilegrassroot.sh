#!/bin/bash
. /etc/environment
. /etc/grassroot

cd /opt/codedeploy-agent/deployment-root/${DEPLOYMENT_GROUP_ID}/${DEPLOYMENT_ID}/deployment-archive
cp $CONFIG_FOLDER/application-$PROFILE.properties ./src/main/resources/
./gradlew clean build -x test