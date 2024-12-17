docker run -itd \
    --restart always \
    --privileged \
    --tty \
    --user 0 \
    -p 2379:2379 \
    -p 2380:2380 \
    --name etcd \
    -e ETCDCTL_API=3 \
    -e TZ=Asia/Shanghai \
    -v /data/etcd:/var/lib/etcd \
    -v /etc/localtime:/etc/localtime \
    -v /data/etcd/data:/data/etcd/data \
    -v /data/etcd/ssl:/data/etcd/ssl \
    -v /data/etcd/script:/data/etcd/script \
    -v /data/etcd/config/etcd.conf.yml:/etc/etcd/etcd.conf.yml \
    quay.io/coreos/etcd:v3.5.16 \
    /usr/local/bin/etcd \
        --config-file=/etc/etcd/etcd.conf.yml
