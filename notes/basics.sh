ufw disable

mirrors.ustc.edu.cn

mirrors.aliyun.com

mirrors.tuna.tsinghua.edu.cn

sudo apt-get update && sudo apt-get upgrade

apt-get -y install apt-transport-https ca-certificates curl gnupg-agent software-properties-common wget iotop-c sysstat htop vim  preload unzip pciutils net-tools dnsutils gcc g++ sysstat powertop

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

echo "ulimit -HSn 65535" >> /etc/rc.local
echo "ulimit -HSn 65535" >> /etc/.bash_profile
echo "ulimit -SHn 65535" >> /etc/profile

cat <<EOF >> /etc/security/limits.conf
soft nofile 655360
hard nofile 131072
soft nproc 655350
hard nproc 655350
soft memlock unlimited
hard memlock unlimited
soft stack 10204
hard stack 32768
soft stack 10204
hard stack 32768
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
apt -y install iptables-services && systemctl start iptables && systemctl enable iptables && iptables -F && iptables-save > /etc/sysconfig/iptables

modprobe br_netfilter

mkdir -p /etc/sysconfig/modules

cat > /etc/sysconfig/modules/ipvs.modules <<EOF
#!/bin/bash
modprobe -- ip_vs
modprobe -- ip_vs_rr
modprobe -- ip_vs_wrr
modprobe -- ip_vs_sh
modprobe -- nf_conntrack
EOF
chmod 755 /etc/sysconfig/modules/ipvs.modules && bash /etc/sysconfig/modules/ipvs.modules && lsmod | grep -e ip_vs -e nf_conntrack

/etc/netplan
01-network-manager-all.yaml

# Let NetworkManager manage all devices on this system
network:
  version: 2
  renderer: networkd
  ethernets:
    enp2s0:
      dhcp4: no
      addresses: [192.168.10.109/24]
      gateway4: 192.168.10.1
      nameservers:
        addresses: [192.168.10.1,61.139.2.69]

sudo netplan apply


#设置history记录全部操作记录
# vim /etc/bash.bashrc #将下面这段内容添加到，并执行bash即可

# format history

# save in ~/.bashrc

USER_IP=`who -u am i 2>/dev/null| awk '{print $NF}'|sed -e 's/[()]//g'`

export HISTTIMEFORMAT="%Y-%m-%d %H:%M:%S  `whoami`@${USER_IP}: "

export HISTFILESIZE=1000000

export PROMPT_COMMAND="history -a; history -r;  $PROMPT_COMMAND"

shopt -s histappend

#bind '"\e[A": history-search-backward'

#bind '"\e[B": history-search-forward'

source /etc/bash.bashrc


# 使用Fail2Ban 来防止暴力破解攻击
sudo apt install fail2ban
sudo systemctl enable fail2ban
sudo systemctl start fail2ban

# 查看 分析 磁盘使用情况
sudo apt install ncdu
ncdu /

#删除7天前的日志
sudo journalctl --vacuum-time=7d

#删除日志文件大于500M的日志 限制为500MB
sudo journalctl --vacuum-size=500M

#vacuum-files选项会清除所有低于指定数量的日志文件 因此在上面的例子中 只有最后两个日志文件被保留其他的都被删除 同样这只对存档的文件有效
sudo journalctl --vacuum-files=3

#删除未使用的容器、网络、卷和镜像
docker system prune -a
#删除未使用的卷
docker volume prune
#清理未使用的镜像
docker image prune -a

#查看磁盘I/O消耗
iotop

#追踪文件系统操作
fatrace

#评估I/O性能的软件 sudo apt-get install sysstat
iostat

#shell 编辑器格式美化
cat <<EOF >>  ~/.bashrc
PS1='[\[\e[34;1m\]\u@\[\e[0m\]\[\e[32;1m\]\H\[\e[0m\]\[\e[31;1m\] \W\[\e[0m\]]# '
EOF
source ~/.bashrc
