systemctl daemon-reload
systemctl restart kube-proxy
sleep 2
systemctl status kube-proxy
journalctl -u kube-proxy
tail -f kube-proxy.err | grep "E1210"
systemctl status kube-proxy | grep Active
systemctl enable kube-proxy
