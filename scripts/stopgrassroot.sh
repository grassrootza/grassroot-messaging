#!/bin/bash

if [[ -f /var/grassroot-messaging/.pid ]]; then
        #kill -TERM `cat /var/grassroot/.pid`;
        kill `cat /var/grassroot-messaging/.pid`;
	mypid=`cat /var/grassroot-messaging/.pid`;
	while [[ `ps -p $mypid > /dev/null;echo $?` -eq '0' ]]; do 
		echo -n '.'; 
		sleep 1; 
	done
        rm -f  /var/grassroot-messaging/.pid;
fi

echo STOPPING DONE