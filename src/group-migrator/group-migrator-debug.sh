#!/bin/bash

java -jar GroupMigrator.jar \
	"-checkpointInterval" "3600"\
	"-keyStore" "/Users/elan/projects/movobluewave/src/group-migrator/keystore"\
	"-keyStorePass" "movomovo"\
	"-trustStore" "/Users/elan/projects/movobluewave/src/group-migrator/truststore"\
	"-trustStorePass" "movomovo"\
	"-useSSL"\
	"-FirebaseURL" "https://movowave-debug.firebaseio.com/"\
	"-FirebaseSecret" "3HFJlhjThUhC9QrP4zAq4PNcaXH8IWYqM8cCWmnR"\
	"-MysqlURL" "jdbc:mysql://173.194.247.177:3306/movogroups?user=root&useSSL=true"\
	"-sqlBatchSize" "10"\
	"-sqlBatchDelay" "1000"\
	"-sqlMaxBatchWait" "1000"\
	
	