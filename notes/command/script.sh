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

docker exec -it elasticsearch bash -c "echo 'vm.max_map_count=262144'>>/etc/sysctl.conf && sysctl -p"

echo "vm.max_map_count=262144" >> /etc/sysctl.conf
sysctl -w vm.max_map_count=262144

#验证
http://IP:9200/_cat/nodes?v=true&pretty

docker run -itd \
--name kibana \
-p 5601:5601 \
--tty \
--user 0 \
--privileged=true \
--restart=always \
-e "TZ=Asia/Shanghai" \
-v /etc/localtime:/etc/localtime:ro \
-v /data/elasticsearch/config/kibana.yml:/usr/share/kibana/config/kibana.yml \
kibana:7.17.1

https://release.infinilabs.com/analysis-ik/stable/

unzip ./elasticsearch-analysis-ik-7.17.1.zip -d elasticsearch-analysis-ik-7.17.1
