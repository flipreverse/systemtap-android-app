#!/system/bin/sh
BUSYBOX="busybox"

line=","
while [ $line != ":q!" ];
do
	read line
	first=${line%=*}
	second=${line#*=}
	case "$first" in
	"pid") PID=$second;;
	"busyboxdir") BUSYBOX_DIR=$second;;
	*) ;;
	esac
done;

if [ $PID -eq -1 ];
then
	$BUSYBOX_DIR/$BUSYBOX killall -2 stapio
else
	$BUSYBOX_DIR/$BUSYBOX kill -2 $PID
fi
