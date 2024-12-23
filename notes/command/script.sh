mkdir -p /data/elasticsearch/{config,data,logs,plugins,temp}

chmod -R 777 /data/elasticsearch/{config,data,logs,plugins,temp}


docker run -itd \
--name elasticsearch \
-p 9200:9200 \
-p 9300:9300 \
--tty \
--user 0 \
--ulimit memlock=-1:-1 \
--ulimit nproc=65535:65535 \
--sysctl vm.max_map_count=262144 \
--privileged=true \
--restart=always \
-e "TZ=Asia/Shanghai" \
-v /etc/localtime:/etc/localtime:ro \
-v /data/elasticsearch/plugins:/usr/share/elasticsearch/plugins \
-v /data/elasticsearch/data:/usr/share/elasticsearch/data \
-v /data/elasticsearch/logs:/usr/share/elasticsearch/logs \
-v /data/elasticsearch/config:/usr/share/elasticsearch/config \
-v /data/elasticsearch/temp:/usr/share/elasticsearch/temp \
elasticsearch:7.17.1

docker run -d \
--name kibana \
-e ELASTICSEARCH_HOSTS=http://es:9200 \
--network=es-net \
-p 5601:5601 \
kibana:7.17.1

https://release.infinilabs.com/analysis-ik/stable/

unzip ./elasticsearch-analysis-ik-7.17.1.zip -d elasticsearch-analysis-ik-7.17.1
