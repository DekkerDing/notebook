systemctl daemon-reload
systemctl restart kube-apiserver
sleep 2
systemctl status kube-apiserver
journalctl -u kube-apiserver -f
tail -f kube-apiserver.err | grep "E1210"
systemctl status kube-apiserver | grep Active
systemctl enable kube-apiserver
