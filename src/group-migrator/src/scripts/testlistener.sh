#!/bin/sh
#
# chkconfig: 35 90 12
# description: runOnProdDB server
#
# Get function from functions library

### BEGIN INIT INFO
# Provides:          test service
# Required-Start:    $remote_fs $syslog
# Required-Stop:     $remote_fs $syslog
# Default-Start:     2 3 4 5
# Default-Stop:      0 1 6
# X-Interactive:     true
# Short-Description: Starts a test service
# Description:       Starts a test service to see if starting stopping on compute engine works
#
### END INIT INFO

PATH=/sbin:/bin
. /lib/init/vars.sh
. /lib/init/tmpfs.sh
. /lib/lsb/init-functions
# Start the service GroupMigrator listener
start() {
        echo "Starting GroupMigrator server " >> /home/ahern/realtime/testmsg.txt
        /home/ahern/realtime/runTestService.sh
        ### NOTE: The pid file is created inside runTestService.sh ####
        ### Create the lock file ###
        touch /var/lock/runTestService.sh
        echo "GroupMigrator server up " >> /home/ahern/realtime/testmsg.txt
        echo
}
# Restart the service MAT
stop() {
        echo "Stopping GroupMigrator server " >> /home/ahern/realtime/testmsg.txt
        kill `echo /var/lock/testlistener.pid`
        ### Now, delete the pid file ###
        rm -f /var/lock/testlistener.pid
        ### Now, delete the lock file ###
        rm -f /var/lock/runTestService.sh
        echo "GroupMigrator server stopped" >> /home/ahern/realtime/testmsg.txt
        echo
}
### main logic ###
case "$1" in
  start)
        start
        ;;
  stop)
        stop
        ;;
  status)
        ps -auwwx | grep 'com.sensorstar.movo.GroupMigrator'
        ;;
  restart|reload|condrestart)
        stop
        start
        ;;
  *)
        echo $"Usage: $0 {start|stop|restart|reload|status}"
        exit 1
esac
exit 0