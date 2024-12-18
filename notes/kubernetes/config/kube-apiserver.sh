KUBE_APISERVER_OPTS="
--enable-admission-plugins=NamespaceLifecycle,LimitRanger,ServiceAccount,DefaultStorageClass,DefaultTolerationSeconds,NodeRestriction,ResourceQuota \

--secure-port=6443 \
--service-node-port-range=30000-50000 \
--bind-address=0.0.0.0 \
--advertise-address=192.168.10.107 \
--service-cluster-ip-range=10.96.0.0/12 \

--authorization-mode=Node,RBAC \
--anonymous-auth=true \
--allow-privileged=true \
--runtime-config=api/all=true \
--enable-bootstrap-token-auth=true \
--token-auth-file=/data/kubernetes/token/token.csv \

--kubelet-client-certificate=/data/kubernetes/ssl/apiserver.pem \
--kubelet-client-key=/data/kubernetes/ssl/apiserver-key.pem \
--kubelet-certificate-authority=/data/kubernetes/ssl/kubernetes-ca.pem \
--tls-cert-file=/data/kubernetes/ssl/apiserver.pem \
--tls-private-key-file=/data/kubernetes/ssl/apiserver-key.pem \
--client-ca-file=/data/kubernetes/ssl/kubernetes-ca.pem \

--service-account-key-file=/data/kubernetes/ssl/kubernetes-ca-key.pem \
--service-account-signing-key-file=/data/kubernetes/ssl/kubernetes-ca-key.pem \
--service-account-issuer=api \

--etcd-cafile=/data/etcd/ssl/etcd-ca.pem \
--etcd-certfile=/data/etcd/ssl/etcd.pem \
--etcd-keyfile=/data/etcd/ssl/etcd-key.pem \
--etcd-servers=http://192.168.10.107:2379 \

--requestheader-client-ca-file=/data/kubernetes/ssl/kubernetes-ca.pem \
--proxy-client-cert-file=/data/kubernetes/ssl/apiserver.pem \
--proxy-client-key-file=/data/kubernetes/ssl/apiserver-key.pem \

--requestheader-allowed-names=kubernetes \
--requestheader-extra-headers-prefix=X-Remote-Extra- \
--requestheader-group-headers=X-Remote-Group \
--requestheader-username-headers=X-Remote-User \
--enable-aggregator-routing=true \

--event-ttl=2h \
--v=4"
