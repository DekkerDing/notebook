systemctl daemon-reload
systemctl restart kubelet
sleep 2
systemctl status kubelet
journalctl -u kubelet
tail -f kubelet.err | grep "E1210"
systemctl status kubelet | grep Active
systemctl enable kubelet
