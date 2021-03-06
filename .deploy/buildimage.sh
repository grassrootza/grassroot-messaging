#! /bin/sh

# DEFINE VARIABLES
SHA1=$1
ENVIRONMENT=$2
S3BUCKET=grassroot-circleci
S3REGION=eu-west-1

# GET THE COMMIT ID TO SET AS PART OF THE INSTANCE NAME
COMMITID=$(git rev-parse --short HEAD)
sed -i "s/<BUILDID>/$COMMITID/" .deploy/startgrassroot.sh.$ENVIRONMENT

# PREPARE MODIFIED IMAGE TO DOCKER HUB
mv .deploy/Dockerfile Dockerfile
mv .deploy/startgrassroot.sh.$ENVIRONMENT startgrassroot.sh
mv .deploy/stopgrassroot.sh stopgrassroot.sh
#mv .deploy/build-jar.sh build-jar.sh
#chmod +x build-jar.sh
chmod +x startgrassroot.sh
chmod +x stopgrassroot.sh

# DEPLOY MODIFIED IMAGE TO DOCKER HUB
docker build --rm=false -t grassrootdocker/gr-msg:$ENVIRONMENT .
docker login -u $DOCKER_USER -p $DOCKER_PASS
docker push grassrootdocker/gr-msg:$ENVIRONMENT
