#!/bin/sh

# DEFINE VARIABLES
ENVIRONMENT=$1
S3BUCKET=grassroot-circleci
S3REGION=eu-west-1

# GET application-production.properties FROM S3, RENAME IT and CP TO RESOURCES FOLDER PRIOR BUILDING THE JAR
aws s3 cp s3://$S3BUCKET/grassroot-messaging-$ENVIRONMENT.properties src/main/resources/application-$ENVIRONMENT.properties --region $S3REGION

# BUILD JAR FILES
# ./gradlew clean build -x test -g /usr/src/grassroot/.gradle/tmp --configure-on-demand --parallel --daemon
./gradlew build -x test -g .gradle/tmp --configure-on-demand --parallel --daemon
