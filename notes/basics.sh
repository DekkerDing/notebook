ufw disable

apt-get -y install apt-transport-https ca-certificates curl gnupg-agent software-properties-common wget

curl -fsSL http://mirrors.aliyun.com/docker-ce/linux/ubuntu/gpg | sudo apt-key add -

sudo add-apt-repository "deb [arch=amd64] http://mirrors.aliyun.com/docker-ce/linux/ubuntu $(lsb_release -cs) stable"

curl -s https://mirrors.aliyun.com/kubernetes/apt/doc/apt-key.gpg | sudo apt-key add -

sudo add-apt-repository "deb [arch=amd64] http://mirrors.aliyun.com/docker-ce/linux/ubuntu $(lsb_release -cs) stable"

sudo apt-get update

apt-cache madison docker-ce
apt-cache madison docker-ce-cli

#安装最新版
sudo apt-get install -y docker-ce

#安装5:20.10.24~3-0~ubuntu-jammy版
sudo apt-get install -y docker-ce=5:20.10.24~3-0~ubuntu-jammy docker-ce-cli=5:20.10.24~3-0~ubuntu-jammy docker-buildx-plugin docker-compose-plugin

sudo apt-mark docker-ce
sudo apt-mark docker-ce-cli
sudo apt-mark docker-buildx-plugin
sudo apt-mark docker-compose-plugin

sudo apt-get install containerd.io



ulimit -SHn 65535

cat <<EOF >> /etc/security/limits.conf
soft nofile 655360
hard nofile 131072
soft nproc 655350
hard nproc 655350
soft memlock unlimited
hard memlock unlimited
EOF


timedatectl set-timezone Asia/Shanghai
#将当前的 UTC 时间写入硬件时钟
timedatectl set-local-rtc 0
#重启依赖于系统时间的服务
systemctl restart rsyslog
systemctl restart crond

ntpdate ntp1.aliyun.com


#关闭postfix
systemctl stop postfix && systemctl disable postfix

mkdir /var/log/journal
mkdir /etc/systemd/journald.conf.d
cat > /etc/systemd/journald.conf.d/99-prophet.conf <<EOF
[Journal]
#持久化保存到磁盘
Storage=persistent
#压缩历史日志
Compress=yes
SyncIntervalSec=5m
RateLimitInterval=30s
RateLimitBurst=1000
#最大占用空间10G
SystemMaxUse=10G
#单日志文件最大 200M
SystemMaxFileSize=200M
#日志保存时间2周
MaxRetentionsSec=2week
#不将日志转发到syslog
ForwardToSyslog=no
EOF

systemctl restart systemd-journald


#关闭firewalld配置iptabes
systemctl stop firewalld && systemctl disable firewalld
yum -y install iptables-services && systemctl start iptables && systemctl enable iptables && iptables -F && iptables-save > /etc/sysconfig/iptables

modprobe br_netfilter
cat > /etc/sysconfig/modules/ipvs.modules <<EOF
#!/bin/bash
modprobe -- ip_vs
modprobe -- ip_vs_rr
modprobe -- ip_vs_wrr
modprobe -- ip_vs_sh
modprobe -- nf_conntrack
EOF
chmod 755 /etc/sysconfig/modules/ipvs.modules && bash /etc/sysconfig/modules/ipvs.modules && lsmod | grep -e ip_vs -e nf_conntrack
