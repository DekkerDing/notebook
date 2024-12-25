ufw disable
sudo systemctl disable ufw.service
sudo systemctl stop ufw.service

#关闭swap分区
sudo swapoff -a
vim /etc/fstab
# UUID=xxxx-xxxx-xxxx-xxxx swap swap defaults 0 0
# /swap.img     none    swap    sw      0       0
sudo rm -f /swap.img


#禁用 Snap 服务
#保留 2 个最近版本，其余版本将被清理
sudo snap set system refresh.retain=2
sudo rm -rf /var/lib/snapd/cache
sudo du -sh /var/lib/snapd
sudo systemctl disable snapd.service
sudo systemctl stop snapd.service
sudo apt-get purge snapd
sudo rm -rf /var/cache/snapd
sudo rm -rf /var/lib/snapd
sudo rm -rf /snap
sudo rm -rf ~/snap

#禁止APT重新安装Snap
echo -e "Package: snapd\nPin: release a*\nPin-Priority: -10" | sudo tee /etc/apt/preferences.d/nosnap.pref

#关闭打印机
sudo systemctl disable cups.service
sudo systemctl stop cups.service

#关闭邮件服务器
sudo systemctl disable postfix.service
sudo systemctl stop postfix.service

sudo apt-get update && sudo apt-get upgrade

apt-get -y install apt-transport-https ca-certificates curl gnupg-agent software-properties-common wget iotop-c sysstat htop vim  preload unzip pciutils net-tools dnsutils gcc g++ sysstat powertop make deborphan lrzsz

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


systemctl daemon-reload
systemctl enable --now docker
systemctl status docker | grep Active
journalctl -u docker -f


systemctl daemon-reload
systemctl restart docker
sleep 2
systemctl status docker | grep Active
journalctl -u docker -f

systemctl enable docker

curl -L https://github.com/docker/compose/releases/download/1.29.2/docker-compose-`uname -s`-`uname -m` > /usr/local/bin/docker-compose
chmod +x /usr/local/bin/docker-compose

sudo apt-get install containerd.io


systemctl daemon-reload
systemctl restart cri-docker
sleep 2
systemctl status cri-docker
journalctl -u cri-docker -f

systemctl enable cri-docker


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

vim /etc/logrotate.conf

# see "man logrotate" for details

# global options do not affect preceding include directives

# rotate log files weekly 每天清理 系统日志
daily

# use the adm group by default, since this is the owning group
# of /var/log/syslog. 指定那些用户操作
su root ding

# keep 4 weeks worth of backlogs 保留文件数量
rotate 3

# create new (empty) log files after rotating old ones
create

# use date as a suffix of the rotated file
#dateext

# uncomment this if you want your log files compressed
#compress

# packages drop log rotation information into this directory
include /etc/logrotate.d

# system-specific logs may also be configured here.

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

cd /etc/netplan
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
vim /etc/bash.bashrc

USER_IP=`who -u am i 2>/dev/null| awk '{print $NF}'|sed -e 's/[()]//g'`

export HISTTIMEFORMAT="%Y-%m-%d %H:%M:%S  `whoami`@${USER_IP}: "

export HISTFILESIZE=1000000

export PROMPT_COMMAND="history -a; history -r;  $PROMPT_COMMAND"

shopt -s histappend

#bind '"\e[A": history-search-backward'

#bind '"\e[B": history-search-forward'

source /etc/bash.bashrc


# 使用Fail2Ban 来防止暴力破解攻击
sudo apt install -y fail2ban
sudo systemctl enable fail2ban
sudo systemctl start fail2ban

# 查看 分析 磁盘使用情况
sudo apt install -y ncdu
ncdu /

#查看未被任何程序依赖的库文件 来删除不再需要的孤立包
sudo deborphan
sudo apt-get remove --purge $(deborphan)


#删除7天前的日志
sudo journalctl --vacuum-time=7d

#删除日志文件大于500M的日志 限制为500MB
sudo journalctl --vacuum-size=500M

#vacuum-files选项会清除所有低于指定数量的日志文件 因此在上面的例子中 只有最后两个日志文件被保留其他的都被删除 同样这只对存档的文件有效
sudo journalctl --vacuum-files=3

#清理历史日志文件
sudo find /var/log -type f -name "*.log" -delete
sudo truncate -s 0 /var/log/syslog
sudo truncate -s 0 /var/log/auth.log
sudo rm -rf /var/log/journal
sudo rm -rf /var/log/installer
sudo rm -rf /var/lib/swcatalog
sudo rm -rf /var/cache/swcatalog
sudo rm -rf /var/cache/apt/*
sudo rm -rf /var/cache/snapd

sudo rm -rf /var/lib/snapd/cache

#检查日志文件大小
du -sh /var/log/* | sort -h


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

#经常清理 apt 缓存
sudo apt clean
sudo apt-get clean

#临时文件清理
sudo rm -rf /tmp/*
sudo rm -rf ~/.cache/*

#删除不再需要的包
sudo apt-get autoremove
#清除缓存
sudo apt-get clean
#删除过时的包文件
sudo apt-get autoclean
#删除过期的包
sudo apt-get autoremove --purge

#CCleaner
sudo apt install bleachbit
export DISPLAY=:0
sudo bleachbit --clean system.cache system.tmp


#列出目录占用情况
du -h --max-depth=1 / | sort -h

#Ubuntu卸载程序
sudo apt remove --purge xxx -y

sudo apt-get remove --purge xxx* -y

#清理相关的残留文件
sudo apt autoremove -y
sudo apt autoclean
sudo rm -rf /var/lib/xxx
sudo rm -rf /etc/xxx

#清理日志文件
sudo rm -rf /var/log/xxx/
