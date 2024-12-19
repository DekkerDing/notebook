KUBE_CONTROLLER_MANAGER_OPTS="
--v=4 \

--service-cluster-ip-range=10.96.0.0/12 \
--cluster-cidr=10.105.0.0/16 \
--allocate-node-cidrs=true \
--cluster-name=kubernetes \

--cloud-provider=external \

--controllers=*,bootstrapsigner,tokencleaner \
--leader-elect=true \
--use-service-account-credentials=true \
--feature-gates RotateKubeletServerCertificate=true \

--node-monitor-grace-period=40s \
--node-monitor-period=5s \
--horizontal-pod-autoscaler-sync-period=10s \

--bind-address=0.0.0.0 \
--secure-port=10257 \

--root-ca-file=/data/kubernetes/ssl/kubernetes-ca.pem \
--cluster-signing-cert-file=/data/kubernetes/ssl/kubernetes-ca.pem \
--cluster-signing-key-file=/data/kubernetes/ssl/kubernetes-ca-key.pem \

--tls-cert-file=/data/kubernetes/ssl/controller-manager.pem \
--tls-private-key-file=/data/kubernetes/ssl/controller-manager-key.pem \

--requestheader-client-ca-file=/data/kubernetes/ssl/kubernetes-ca.pem \

--service-account-private-key-file=/data/kubernetes/ssl/kubernetes-ca-key.pem \
--kubeconfig=/data/kubernetes/config/controller-manager.kubeconfig \
--cluster-signing-duration=876000h"
