docker run \
-itd \
--restart=always \
--privileged \
--tty \
--user 0 \
--name="portainer" \
-e TZ=Asia/Shanghai \
-p 19000:9000 \
-v /var/run/docker.sock:/var/run/docker.sock \
-v /data/portainer:/data \
6053537/portainer-ce
