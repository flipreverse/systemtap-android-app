#!/system/bin/sh
PATH=`pwd`
STORAGE_PATH="/sdcard"
CONFIG_FILE="stap.conf"
LOG_OUTPUT_EXTENSION="txt"

COUNT=1
while read line;
do
	case "$COUNT" in
	1) MODULE_NAME=$line;;
	2) MODULE_DIR=$line;;
	3) OUTPUT_NAME=$line;;
	4) OUTPUT_DIR=$line;;
	5) LOG_OUTPUT_DIR=$line;;
	*) ;;
	esac
	let COUNT=COUNT+1
done < "$PATH/$CONFIG_FILE";

if [ $COUNT -le 5 ];
then
	echo "Insufficient parameters"
	exit 1
fi

LOG_OUTPUT=$LOG_OUTPUT_DIR"/"$OUTPUT_NAME"."$LOG_OUTPUT_EXTENSION

export SYSTEMTAP_STAPRUN=$PATH"/staprun"
export SYSTEMTAP_STAPIO=$PATH"/stapio"

echo "Loaded kernel module: "$MODULE_NAME.ko > $LOG_OUTPUT
echo "Output file: "$OUTPUT_NAME".*" >> $LOG_OUTPUT

echo "$SYSTEMTAP_STAPRUN -o $OUTPUT_DIR"/"$OUTPUT_NAME -S 256 $MODULE_DIR"/"$MODULE_NAME.ko >> $LOG_OUTPUT 2>&1 &"
