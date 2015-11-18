#!/bin/bash
CLASSPATH=".:./lib/commons-cli-1.3.1.jar:./lib/commons-codec-1.7.jar:\
./lib/commons-logging-1.1.1.jar:./lib/firebase-client-jvm-2.4.0.jar:\
./lib/firebase-token-generator-2.0.0.jar:./lib/group-migrator.jar:\
./lib/httpclient-4.0.1.jar:./lib/httpcore-4.0.1.jar:\
./lib/jackson-annotations-2.2.2.jar:./lib/jackson-core-2.2.2.jar:\
./lib/jackson-databind-2.2.2.jar:\
./lib/jersey-client-1.8.jar:./lib/jersey-core-1.8.jar:\
./lib/json-20090211.jar:./lib/log4j-1.2.17.jar:\
./lib/mysql-connector-java-5.1.34.jar:./lib/tubesock-0.0.12.jar:"
/usr/lib/jvm/java-7-openjdk-amd64/bin/java -Djava.util.logging.config.file="/home/ahern/realtime/log.properties" \
-Xms512m -Xmx1152m -Xss512m -XX:MaxPermSize=256m -XX:MaxNewSize=256m \
-classpath $CLASSPATH com.sensorstar.movo.GroupMigrator \
-keyStore "/home/ahern/realtime/prodcerts/keystore" \
-MysqlURL "jdbc:mysql://173.194.241.127:3306/movogroups?user=root&useSSL=true" \
-trustStore "/home/ahern/realtime/prodcerts/truststore" -useSSL >> msg.txt 2>&1 &
echo $! > /var/lock/listener.pid