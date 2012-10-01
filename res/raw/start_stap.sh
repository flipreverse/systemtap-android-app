#!/system/bin/sh
STORAGE_PATH="/sdcard"
LOG_OUTPUT_EXTENSION=".txt"
PID_EXTENSION=".pid"
KERNEL_MODULE_EXTENSION=".ko"

line=","
while [ $line != ":q!" ];
do
	read line
	first=${line%=*}
	second=${line#*=}
	case "$first" in
	"modulename") MODULE_NAME=$second;;
	"moduledir") MODULE_DIR=$second;;
	"outputname") OUTPUT_NAME=$second;;
	"outputdir") OUTPUT_DIR=$second;;
	"logdir") LOG_DIR=$second;;
	"rundir") RUN_DIR=$second;;
	"stapdir") STAP_DIR=$second;;
	*) ;;
	esac
done;

LOG_OUTPUT=$LOG_DIR"/"$OUTPUT_NAME$LOG_OUTPUT_EXTENSION

export SYSTEMTAP_STAPRUN=$STAP_DIR"/staprun"
export SYSTEMTAP_STAPIO=$STAP_DIR"/stapio"

echo "Loaded kernel module: "$MODULE_NAME.ko > $LOG_OUTPUT
echo "Output file: "$OUTPUT_NAME".*" >> $LOG_OUTPUT

$SYSTEMTAP_STAPRUN -M $RUN_DIR"/"$MODULE_NAME$PID_EXTENSION -o $OUTPUT_DIR"/"$OUTPUT_NAME -S 256 $MODULE_DIR"/"$MODULE_NAME$KERNEL_MODULE_EXTENSION >> $LOG_OUTPUT 2>&1 &
