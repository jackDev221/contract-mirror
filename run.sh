#!/bin/sh

if [ ! $1 ];
then
    echo "Usage: sh run.sh [start|stop|restart|status] [projectname] [profile] [port] [kafka_group_id]"
    exit 1
fi

API_NAME=$2
PROFILE=$3
PORT=$4
KAFKA_GROUP_ID=$5

if [ ! $2 ]; then
    API_NAME='contract-mirror-1.0.0'
fi

if [ ! $3 ]; then
    PROFILE='localdev'
fi

if [ ! $4 ]; then
    PORT=10020
fi



JAR_NAME=$API_NAME\.jar
#PID  代表是PID文件
PID=$API_NAME\.pid




#使用说明，用来提示输入参数
usage() {
    echo "Usage: sh run.sh [start|stop|restart|status] [projectname] [profile] [port] [kafka_group_id]"
    exit 1
}

#检查程序是否在运行
is_exist(){
  pid=`ps -ef|grep $JAR_NAME|grep $PORT|grep -v grep|awk '{print $2}' `
  #如果不存在返回1，存在返回0
  if [ -z "${pid}" ]; then
   return 1
  else
    return 0
  fi
}

#启动方法
start(){
  is_exist
  if [ $? -eq "0" ]; then
    echo ">>> ${JAR_NAME} is already running PID=${pid} <<<"
  else
    nohup /opt/jdk-11.0.18/bin/java -Xms1g -Xmx8g -jar $JAR_NAME  --spring.profiles.active=$PROFILE --server.port=$PORT --kafka.groupId=contract_mirror_${KAFKA_GROUP_ID} >/dev/null 2>&1 &
    echo $! > $PID
    echo ">>> start $JAR_NAME successed PID=$! <<<"
   fi
  }

#停止方法
stop(){
  #is_exist
  pidf=$(cat $PID)
  #echo "$pidf"
  echo ">>> api PID = $pidf begin kill $pidf <<<"
  kill $pidf
  rm -rf $PID
  sleep 2
  is_exist
  if [ $? -eq "0" ]; then
    echo ">>> api 2 PID = $pid begin kill -9 $pid  <<<"
    kill -9  $pid
    sleep 2
    echo ">>> $JAR_NAME process stopped <<<"
  else
    echo ">>> ${JAR_NAME} is not running <<<"
  fi
}

#输出运行状态
status(){
  is_exist
  if [ $? -eq "0" ]; then
    echo ">>> ${JAR_NAME} is running PID is ${pid} <<<"
  else
    echo ">>> ${JAR_NAME} is not running <<<"
  fi
}

#重启
restart(){
  stop
  start
}

#根据输入参数，选择执行对应方法，不输入则执行使用说明
case "$1" in
  "start")
    start
    ;;
  "stop")
    stop
    ;;
  "status")
    status
    ;;
  "restart")
    restart
    ;;
  *)
    usage
    ;;
esac
exit 0


