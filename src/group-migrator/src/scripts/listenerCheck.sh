#!/usr/bin/env bash

NOW=$(date +%s%N | cut -b1-13);
HEART=`cat dbheartbeat.txt`
MAIN=`cat mainheartbeat.txt`
diffh=$(($NOW - $HEART))
diffm=$(($NOW - $MAIN))

if [ "${diffh:-0}" -ge 1200000 ]; then
    curl -X POST --data "alertname=database_thread&alerttype=database&message=GroupMonitor%20database%20thread%20is%20down&check=f565e56c-4cc4-49e3-b53c-21757c0f1c29" https://movoalerts.appspot.com/alerts
fi

if [ "${diffm:-0}" -ge 1200000 ]; then
    curl -X POST --data "alertname=main_thread&alerttype=database&message=GroupMonitor%20main%20thread%20is%20down&check=f565e56c-4cc4-49e3-b53c-21757c0f1c29" https://movoalerts.appspot.com/alerts
fi
echo `date`

ahern@firebaseparser:~/realtime$ cat listenerCheck.sh
#!/usr/bin/env bash
JAVAPROCESS=`ps auwwx | grep GroupMigrator | grep -v grep`

if [ -z "$JAVAPROCESS" ]; then
    curl -X POST --data "alertname=java_process&alerttype=javaprocess&message=GroupMonitor%20java%20thread%20is%20down&check=f565e56c-4cc4-49e3-b53c-21757c0f1c29" https://movoalerts.appspot.com/alerts
    /home/ahern/realtime/runOnProdDB.sh
fi
echo `date`