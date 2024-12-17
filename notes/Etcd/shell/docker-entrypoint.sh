   #!/bin/sh

   # 启动 etcd
   /usr/local/bin/etcd --config-file=/etc/etcd/etcd.conf.yml &

   # 等待 etcd 启动完成
   sleep 10

   # 执行初始化命令
   etcdctl --endpoints="192.168.10.109:2379,192.168.10.107:2379,192.168.10.120:2379" \
           --cacert=/data/etcd/ssl/etcd-ca.pem \
           --cert=/data/etcd/ssl/etcd.pem \
           --key=/data/etcd/ssl/etcd-key.pem \
           endpoint status --write-out=table

   # 等待 etcd 进程结束
   wait $!
