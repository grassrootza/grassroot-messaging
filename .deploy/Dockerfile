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

# START SUPERVISORD
CMD ["supervisord", "-n"]
