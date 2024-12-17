docker run -itd \
  --restart=always \
  --privileged \
  --tty \
  --user 0 \
  -p 2379:2379 \
  -p 2380:2380 \
  --name etcd \
  -e TZ=Asia/Shanghai \
  -v /data/etcd:/var/lib/etcd \
  -v /etc/localtime:/etc/localtime \
  -v /data/etcd/data:/data/etcd/data \
  -v /data/etcd/ssl:/data/etcd/ssl \
  -v /data/etcd/script:/data/etcd/script \
  -v /data/etcd/config/etcd.conf.yml:/etc/etcd/etcd.conf.yml \
  --mount type=bind,source=/data/etcd/data,destination=/data/etcd/data \
  quay.io/coreos/etcd:v3.5.11 \
  /usr/local/bin/etcd \
  --name etcd \
  --data-dir /data/etcd/data \
  --listen-client-urls http://0.0.0.0:2379 \
  --advertise-client-urls http://0.0.0.0:2379 \
  --initial-advertise-peer-urls http://0.0.0.0:2380 \
  --log-level warn \
  --logger zap \
  --log-outputs stderr
