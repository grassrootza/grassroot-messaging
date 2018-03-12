# deploy.sh
#! /bin/bash

# DEFINE VARIABLES
SHA1=$1
ENVIRONMENT=$2
EBENVIRONMENT=$3
S3BUCKET=grassroot-circleci
S3REGION=eu-west-1

# GET GIT LATEST COMMIT COMMENT TO SET AS APP VERSION FOR BEANSTALK #
COMMIT_MESSAGE=$(git rev-parse --short HEAD && date +"%m-%d-%Y-%H-%M")
export COMMIT_MESSAGE

# EDIT DOCKERRUN TO TAG THE ENVIRONMENT JUST PUSHED TO DOCKER HUB
DOCKERRUN_FILE=Dockerrun.aws.json
sed -i "s/<TAG>/$ENVIRONMENT/" .deploy/$DOCKERRUN_FILE

# DOWNLOAD ENVIRONMENT VARIABLES AND CREDENTIALS FROM S3
mkdir -p environment
mkdir -p log
aws s3 cp s3://$S3BUCKET/environment-variables.messaging.$ENVIRONMENT environment/environment-variables.messaging.production --region $S3REGION
aws s3 cp s3://$S3BUCKET/aws-credentials.$ENVIRONMENT environment/aws-credentials --region $S3REGION
#aws s3 cp s3://$S3BUCKET/jwt_keystore.jks.$ENVIRONMENT environment/jwt_keystore.jks --region $S3REGION
touch log/grassroot-msg.log

# STORE DEPLOYMENT DETAILS FOR FURTHER DEBUG
echo $COMMIT_MESSAGE > deploy_status.txt

# COMMIT
# thats just a "virtual" commit since we do not send anything back to the repository, but this is requred for beasntalke to get the latest staged version to be deployed
git config --global user.email "grassroot@grassroot.com"
git config --global user.name "GRASSROOT"
git add .
git commit -m "$ENVIRONMENT-$COMMIT_MESSAGE"

# DEPLOY APP
mv .deploy/.elasticbeanstalk .elasticbeanstalk
mv .deploy/.ebextensions .ebextensions
mv .deploy/.ebignore .ebignore
mv .deploy/Dockerrun.aws.json Dockerrun.aws.json
mv .deploy/log_files.yml log_files.yml

eb use $EBENVIRONMENT
eb deploy $EBENVIRONMENT --label "$ENVIRONMENT-$COMMIT_MESSAGE" --timeout 20
