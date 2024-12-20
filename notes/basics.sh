apt-get -y install apt-transport-https ca-certificates curl gnupg-agent software-properties-common

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
