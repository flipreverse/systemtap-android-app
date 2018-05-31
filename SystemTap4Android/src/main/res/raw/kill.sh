#!/system/bin/sh
BUSYBOX="busybox"

PID=${1}; shift
BUSYBOX_DIR=${1}; shift

if [ $PID -eq -1 ];
then
	$BUSYBOX_DIR/$BUSYBOX killall -2 stapio
else
	$BUSYBOX_DIR/$BUSYBOX kill -2 $PID
fi
