#!/bin/sh

# sbt assembly

# 设置参数
####################
export SPARK_HOME="/usr/spark"
export SPARK_MASTER_URL="spark://spark-server:7077"

# hadoop hdfs
export ROOT_DIR="hdfs://spark-server:9000/user/hadoop"
# 数据类型
export APP_INPUT_FILE_TYPE="SogouC.reduced"
# 训练集
export APP_INPUT_FILE_TRAINING="$ROOT_DIR/data/AfterCombination/SogouC.reduced/Reduced"
# 测试集
export APP_INPUT_FILE_TEST="$ROOT_DIR/data/AfterCombination/SogouC.reduced/Reduced"
# 预测结果
export APP_OUTPUT_FILE_PREDICTIONS="$ROOT_DIR/data/SogouC.reduced/predictions.json"
# 模型
export APP_MODEL_DIR="$ROOT_DIR/data/SogouC.reduced/working_model"
# jar包
export JAR_FILE="$HOME/spark_jars/RandomForests-assembly-1.3.jar"
