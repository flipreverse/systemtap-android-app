#!/system/bin/sh

# Copyright 2012 Alexander Lochmann
#
# This file is part of SystemTap4Android.
#
# SystemTap4Android is free software: you can redistribute it and/or modify
# it under the terms of the GNU General Public License as published by
# the Free Software Foundation, either version 3 of the License, or
# (at your option) any later version.
#
# SystemTap4Android is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU General Public License for more details.
#
# You should have received a copy of the GNU General Public License
# along with SystemTap4Android.  If not, see <http://www.gnu.org/licenses/>.

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

$SYSTEMTAP_STAPRUN -U $RUN_DIR"/"$MODULE_NAME$PID_EXTENSION -o $OUTPUT_DIR"/"$OUTPUT_NAME -S 256 $MODULE_DIR"/"$MODULE_NAME$KERNEL_MODULE_EXTENSION >> $LOG_OUTPUT 2>&1 &
