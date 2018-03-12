### 1.安装mysql 并将xunwu.sql 数据导入
### 2.安装zookeeper (http://zookeeper.apache.org) 启动
### 3.安装kafka（http://kafka.apache.org） 启动并创建 xunwu_topic 主题
### 4.安装ElasticSearch（version=5.6.1），设置集群名称为xunwu-es，并安装elasticsearch-analysis-ik 插件，启动es（如果需要使用head，需要在配置文件中设置跨域）
### 5.向 http://localhost:9200/xunwu 发送json请求，请求内容为xunwu-web/src/main/resources/db/house_index_with_suggest_mapping.json
### 6.安装redis并启动
### 7.执行build_online.sh 打包阿里手机验证码的相关工具包
### 8.修改application-*.yml文件配置
### 9.执行build.sh启动项目
