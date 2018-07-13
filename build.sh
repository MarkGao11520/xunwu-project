# xunwu-project 一键启动
echo "启动zk..."
zkServer.sh start

echo "启动kafka..."
$KAFKA_HOME/bin/kafka-server-start.sh -daemon $KAFKA_HOME/config/server.properties &

echo "启动es..."
nohup $ES_HOME/bin/elasticsearch &

echo "启动redis..."
nohup redis-server &

echo "打包项目..."
mvn clean package -DskipTests

echo "启动项目..."
nohup java -jar ./xunwu-web/target/xunwu-project.jar &
