## PULL UBUNTU IMAGE WITH OPENJDK 8 SETUP
FROM openjdk:8
WORKDIR /usr/src/grassroot-msg

# SETUP SUPERVISORD TO RUN JAVA AS A PROCESS + OTHER FANCY THINGS SUPERVISORD PROVIDES
RUN apt-get update && apt-get install supervisor vim nano -y
COPY .deploy/supervisord.conf /etc/supervisor/conf.d/supervisord.conf

# DEFINE ENVIRONMENT VARIABLES
ENV GRADLE_USER_HOME=/usr/src/grassroot-msg/.gradle

# SETUP ENVIRONMENT AND LOGS FOLDER THAT WILL BE SHARED WITH THE HOST
RUN mkdir -p /usr/src/grassroot-msg/environment && mkdir -p /usr/src/grassroot-msg/log

# COPY ALL SOURCE CODE, ALTERNATIVELY WE COULD BE COPYING JUST THE JAR FILES (TO REVIEW LATER)
# DECIDED TO COPY ONLY THE JAR FILES
COPY . /usr/src/grassroot-msg

# BUILD JAR FILE
# COPY .deploy/build-jar.sh /usr/src/grassroot-msg/build-jar.sh
RUN /usr/src/grassroot-msg/build-jar.sh

# START SUPERVISORD
CMD ["supervisord", "-n"]
