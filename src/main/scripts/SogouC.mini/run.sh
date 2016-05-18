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
  --name "SogouC.mini" \
  --master $SPARK_MASTER_URL \
  $JAR_FILE \
  app.input.file.type="$APP_INPUT_FILE_TYPE" \
  app.input.file.training="$APP_INPUT_FILE_TRAINING" \
  app.input.file.test="$APP_INPUT_FILE_TEST" \
  app.output.file.predictions="$APP_OUTPUT_FILE_PREDICTIONS" \
  app.model.dir="$APP_MODEL_DIR"

cd /usr/hadoop
bin/hdfs dfs -cat data/SogouC.mini/predictions.json_RandomForestModel.csv > ~/SogouC_mini_RandomForestModel.csv
bin/hdfs dfs -cat data/SogouC.mini/predictions.json_EnhancedRandomForestModel.csv > ~/SogouC_mini_EnhancedRandomForestModel.csv
