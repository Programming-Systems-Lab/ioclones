#!/bin/sh

BASEDIR=$(pwd)
IO_REPO="$BASEDIR/iorepo"
LOGS="$BASEDIR/logs"

echo 'Executing HitoshiIO crowd execution'
if [ -d "$IO_REPO" ];
then
	echo "Confirm $IO_REPO"
else
	echo "Creating $IO_REPO"
	mkdir $IO_REPO
fi

if [ -d "$LOGS" ];

then
	echo "Confirm $LOGS"
else
	echo "Creating $LOGS"
	mkdir $LOGS
fi

echo "Confirm codebase: $1"
echo "Confirm problem set: $2"

java -cp "target/CloneDetector-0.0.1-SNAPSHOT.jar" edu.columbia.cs.psl.ioclones.driver.CrowdDriver $1 $2
