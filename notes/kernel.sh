cat >> /etc/sysctl.conf << EOF
net.core.netdev_max_backlog = 32768
net.core.rmem_default = 8388608
net.core.somaxconn = 32768
net.core.wmem_default = 8388608
net.ipv4.conf.all.arp_ignore = 0
net.ipv4.conf.lo.arp_announce = 0
net.ipv4.conf.lo.arp_ignore = 0
net.ipv4.ip_local_port_range = 5000 65000
net.ipv4.tcp_fin_timeout = 30
net.ipv4.tcp_keepalive_intvl = 30
net.ipv4.tcp_keepalive_probes = 3
net.ipv4.tcp_keepalive_time = 300
net.ipv4.tcp_max_orphans = 3276800
net.ipv4.tcp_max_syn_backlog = 65536
net.ipv4.tcp_max_tw_buckets = 5000
net.ipv4.tcp_mem = 94500000 915000000 927000000
net.ipv4.tcp_syn_retries = 2
net.ipv4.tcp_synack_retries = 2
net.ipv4.tcp_syncookies = 1
net.ipv4.tcp_timestamps = 0
net.ipv4.tcp_tw_reuse = 1
vm.max_map_count = 655360
vm.overcommit_memory = 1
kernel.msgmnb = 65536
kernel.msgmax = 65536
net.ipv6.conf.all.disable_ipv6 = 0
kernel.unknown_nmi_panic = 0
kernel.sysrq = 1
fs.file-max = 1000000
vm.swappiness = 10
fs.inotify.max_user_watches = 10000000
net.core.wmem_max = 327679
net.core.rmem_max = 327679
net.ipv4.conf.all.send_redirects = 0
net.ipv4.conf.default.send_redirects = 0
net.ipv4.conf.all.secure_redirects = 0
net.ipv4.conf.default.secure_redirects = 0
net.ipv4.conf.all.accept_redirects = 0
net.ipv4.conf.default.accept_redirects = 0
fs.inotify.max_queued_events = 327679
kernel.shmmax = 68719476736
kernel.shmall = 4294967296
net.ipv4.neigh.default.gc_thresh1 = 2048
net.ipv4.neigh.default.gc_thresh2 = 4096
net.ipv4.neigh.default.gc_thresh3 = 8192
#net.netfilter.nf_conntrack_tcp_timeout_time_wait = 30
#net.nf_conntrack_max = 1000000
#net.netfilter.nf_conntrack_max = 1000000
net.ipv4.tcp_congestion_control = bbr
net.ipv4.tcp_sack = 1
net.ipv4.tcp_fack = 1
net.ipv4.tcp_dsack = 1
net.ipv4.tcp_window_scaling = 1
net.ipv4.tcp_retries1 = 3
net.ipv4.tcp_retries2 = 5
net.ipv4.tcp_fastopen = 3
net.ipv4.neigh.default.gc_stale_time = 90
EOF

sysctl -p

https://www.cnblogs.com/wangguishe/p/15409855.html
