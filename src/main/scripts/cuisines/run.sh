#!/bin/sh

# Set the global configuration

rundir="`dirname "$0"`"
rundir="`cd "$rundir"; pwd`"
. "$rundir"/conf.sh

# RUN SECTION
#############
cd $SPARK_HOME
./bin/spark-submit \
  --class "cn.edu.jlu.ccst.randomforests.novel.sparx.Main" \
  --name "CuisineDataPrediction" \
  --master $SPARK_MASTER_URL \
  $JAR_FILE \
  app.input.file.training="$APP_INPUT_FILE_TRAINING" \
  app.input.file.test="$APP_INPUT_FILE_TEST" \
  app.output.file.predictions="$APP_OUTPUT_FILE_PREDICTIONS" \
  app.model.dir="$APP_MODEL_DIR"

cd /usr/hadoop
bin/hdfs dfs -cat data/cuisines/predictions.json_RandomForestModel.csv > ~/cuisines_RandomForestModel.csv