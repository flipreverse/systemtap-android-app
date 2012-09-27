#!/system/bin/sh
PATH=`pwd`
BUSYBOX="busybox"
CONFIG_FILE="kill.conf"

COUNT=1
while read line;
do
	case "$COUNT" in
	1) PID=$line;;
	*) ;;
	esac
	let COUNT=COUNT+1
done < "$PATH/$CONFIG_FILE";

if [ $COUNT -le 1 ];
then
	echo "Insufficient parameters"
	exit 1
fi

if [ $PID -eq -1 ];
then
	$PATH/$BUSYBOX killall -2 stapio
else
	$PATH/$BUSYBOX kill -2 
fi
