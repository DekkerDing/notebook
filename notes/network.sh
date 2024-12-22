# 优化TCP SYN重传次数 tcp_syn_retries 的默认值是 6
net.ipv4.tcp_syn_retries = 2

# 优化半连接队列 系统默认值为128
net.ipv4.tcp_max_syn_backlog = 16384

#优化TCP SYNACK的重传次数 系统默认值为5
net.ipv4.tcp_synack_retries = 2

# 开启SYN Cookies 可以防止部分 SYN Flood 攻击
net.ipv4.tcp_syncookies = 1

# 优化全连连队列 系统默认值为1024
net.core.somaxconn = 16384

#全连接溢出是否通知客户端
net.ipv4.tcp_abort_on_overflow = 0

# 优化FIN超时时间 设置了这个状态的超时时间 tcp_fin_timeout，默认为 60s，超过这个时间后就会自动销毁该连接
net.ipv4.tcp_fin_timeout = 2

# 配置TIME_WAIT 状态端口复用 由于 TCP 端口最多只有 65536 出现端口被占用而无法创建新连接的情况
net.ipv4.tcp_tw_reuse = 1

# 优化TCP发送缓冲区 默认值为 4096  16384  4194304
net.ipv4.tcp_wmem = 8192 65536 16777216

# 优化套接字发送缓冲区 默认值为212992
net.core.wmem_max = 16777216

#优化TCP接收缓冲区
net.ipv4.tcp_rmem = 8192 65536 16777216

#优化套接字接收缓冲区 默认值为212992
net.core.wmem_max = 16777216

#优化TCP内存消耗限制 默认值为88053  117407  176106
net.ipv4.tcp_mem = 8388608 12582912 16777216
