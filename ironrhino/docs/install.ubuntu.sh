#!/bin/sh

#must run with sudo
if [ ! -n "$SUDO_USER" ];then
echo please run sudo $0
exit 1
else
USER="$SUDO_USER"
fi

#install packages
apt-get update
apt-get --force-yes --yes install openjdk-7-jdk ant mysql-server subversion nginx chkconfig sysv-rc-conf fontconfig xfonts-utils unzip wget iptables make gcc

#config mysql
if [ -f "/etc/mysql/my.cnf" ] && ! $(more /etc/mysql/my.cnf|grep collation-server >/dev/null 2>&1) ; then
sed -i '32i collation-server = utf8_general_ci' /etc/mysql/my.cnf
sed -i '32i character-set-server = utf8' /etc/mysql/my.cnf
service mysql restart
fi

#install simsun font
if [ -f "simsun.ttf" ]; then
mv simsun.ttf /usr/share/fonts/truetype
chmod 644 /usr/share/fonts/truetype/simsun.ttf
cd /usr/share/fonts
mkfontscale
mkfontdir
fc-cache -fv
fi

#install tomcat
if [ ! -d tomcat8080 ];then
if ! $(ls -l apache-tomcat-*.tar.gz >/dev/null 2>&1) ; then
wget http://archive.apache.org/dist/tomcat/tomcat-7/v7.0.37/bin/apache-tomcat-7.0.37.tar.gz
fi
tar xvf apache-tomcat-*.tar.gz >/dev/null && rm -rf apache-tomcat-*.tar.gz
rename s/^apache-tomcat.*$/tomcat/g apache-tomcat-*
cd tomcat && rm -rf bin/*.bat && rm -rf webapps/* && cd ..
cat>tomcat/conf/server.xml<<EOF
<?xml version='1.0' encoding='utf-8'?>
<Server port="\${port.shutdown}" shutdown="SHUTDOWN">
  <Service name="Catalina">
    <Connector port="\${port.http}" protocol="org.apache.coyote.http11.Http11NioProtocol" connectionTimeout="20000" redirectPort="8443" URIEncoding="UTF-8" useBodyEncodingForURI="true" enableLookups="false" bindOnInit="false" server="ironrhino" maxPostSize="4194304"/>
    <Engine name="Catalina" defaultHost="localhost">
      <Host name="localhost" appBase="webapps" unpackWARs="true" autoDeploy="false">
      </Host>
    </Engine>
  </Service>
</Server>
EOF
sed -i '99i export SPRING_PROFILES_DEFAULT' tomcat/bin/catalina.sh
sed -i '99i SPRING_PROFILES_DEFAULT="dual"' tomcat/bin/catalina.sh
sed -i '99i CATALINA_OPTS="-server -Xms128m -Xmx1024m -Xmn80m -Xss256k -XX:PermSize=128m -XX:MaxPermSize=512m -XX:+UseConcMarkSweepGC -XX:+UseCMSCompactAtFullCollection -XX:+UseParNewGC -XX:CMSMaxAbortablePrecleanTime=5"' tomcat/bin/catalina.sh
cp -R tomcat tomcat8080
cp -R tomcat tomcat8081
rm -rf tomcat
sed -i '99i CATALINA_PID="/tmp/tomcat8080_pid"' tomcat8080/bin/catalina.sh
sed -i '99i JAVA_OPTS="-Dport.http=8080 -Dport.shutdown=8005"' tomcat8080/bin/catalina.sh
sed -i '99i CATALINA_PID="/tmp/tomcat8081_pid"' tomcat8081/bin/catalina.sh
sed -i '99i JAVA_OPTS="-Dport.http=8081 -Dport.shutdown=8006"' tomcat8081/bin/catalina.sh
chown -R $USER:$USER tomcat*
fi


if [ ! -f /etc/init.d/tomcat8080 ]; then
cat>/etc/init.d/tomcat8080<<EOF
#!/bin/sh
#
# Startup script for the tomcat
#
# chkconfig: 345 80 15
# description: Tomcat
user=$USER

case "\$1" in
start)
       su \$user -c "/home/$USER/tomcat8080/bin/catalina.sh start"
       ;;
stop)
       su \$user -c "/home/$USER/tomcat8080/bin/catalina.sh stop -force"
       ;;
restart)
       su \$user -c "/home/$USER/tomcat8080/bin/catalina.sh stop -force"
       su \$user -c "/home/$USER/tomcat8080/bin/catalina.sh start"
       ;;
*)
       echo "Usage: \$0 {start|stop|restart}"
       esac

exit 0
EOF
chmod +x /etc/init.d/tomcat8080
update-rc.d tomcat8080 defaults
fi

if [ ! -f /etc/init.d/tomcat8081 ]; then
cat>/etc/init.d/tomcat8081<<EOF
#!/bin/sh
#
# Startup script for the tomcat
#
# chkconfig: 345 80 15
# description: Tomcat
user=$USER

case "\$1" in
start)
       su \$user -c "/home/$USER/tomcat8081/bin/catalina.sh start"
       ;;
stop)
       su \$user -c "/home/$USER/tomcat8081/bin/catalina.sh stop -force"
       ;;
restart)
       su \$user -c "/home/$USER/tomcat8081/bin/catalina.sh stop -force"
       su \$user -c "/home/$USER/tomcat8081/bin/catalina.sh start"
       ;;
*)
       echo "Usage: \$0 {start|stop|restart}"
       esac

exit 0
EOF
chmod +x /etc/init.d/tomcat8081
update-rc.d tomcat8081 defaults
fi


#config nginx
if [ -f /etc/nginx/sites-enabled/default ] && ! $(more /etc/nginx/sites-enabled/default|grep backend >/dev/null 2>&1) ; then
rm -rf /etc/nginx/sites-enabled/default
fi
if [ ! -f /etc/nginx/sites-enabled/default ]; then
cat>/etc/nginx/sites-enabled/default<<EOF
gzip_min_length  1024;
gzip_types       text/xml text/css text/javascript application/x-javascript;
upstream  backend  {
    server   localhost:8080;
    server   localhost:8081;
}
server {
     listen   80 default_server;
     location ~ ^/assets/ {
             root   /home/$USER/tomcat8080/webapps/ROOT;
             expires      max;
             add_header Cache-Control public;
             charset utf-8;
     }
     location  / {
             proxy_pass  http://backend;
             proxy_redirect    off;
             proxy_set_header  X-Forwarded-For  \$proxy_add_x_forwarded_for;
             proxy_set_header  X-Real-IP  \$remote_addr;
             proxy_set_header  Host \$http_host;
     }
}
EOF
service nginx restart
fi


#generate deploy.sh
if [ ! -f deploy.sh ]; then
cat>deploy.sh<<EOF
if [ "\$1" = "" ];  then
    echo "please run \$0 name"
    exit 1
elif [ ! -d "\$1" ]; then
    echo "directory \$1 doesn't exists"
    exit 1
fi
app="\$1"
if [[ "\$app" =~ "/" ]] ; then
app="\${app:0:-1}"
fi
cd ironrhino
OLDLANGUAGE=\$LANGUAGE
LANGUAGE=en
svnupoutput=\`svn up\`
LANGUAGE=\$OLDLANGUAGE
echo "\$svnupoutput"
if \$(echo "\$svnupoutput"|grep Updated >/dev/null 2>&1) ; then
ant dist
fi
if ! \$(ls -l target/ironrhino*.jar >/dev/null 2>&1) ; then
ant dist
fi
cd ..
cd \$app && svn up
ant -Dserver.home=/home/$USER/tomcat8080 -Dwebapp.deploy.dir=/home/$USER/tomcat8080/webapps/ROOT deploy
ant -Dserver.home=/home/$USER/tomcat8081 -Dserver.shutdown.port=8006 -Dserver.startup.port=8081 shutdown
rm -rf /home/$USER/tomcat8081/webapps
mkdir -p /home/$USER/tomcat8081/webapps
cp -R /home/$USER/tomcat8080/webapps/ROOT /home/$USER/tomcat8081/webapps
ant -Dserver.home=/home/$USER/tomcat8081 -Dserver.shutdown.port=8006 -Dserver.startup.port=8081 startup
EOF
chown $USER:$USER deploy.sh
chmod +x deploy.sh
fi

#generate rollback.sh
if [ ! -f rollback.sh ]; then
cat>rollback.sh<<EOF
if [ "\$1" = "" ];  then
    echo "please run \$0 name"
    exit 1
elif [ ! -d "\$1" ]; then
    echo "directory \$1 doesn't exists"
    exit 1
fi
app="\$1"
if [[ "\$app" =~ "/" ]] ; then
app="\${app:0:-1}"
fi
cd \$app
ant -Dserver.home=/home/$USER/tomcat8080 -Dwebapp.deploy.dir=/home/$USER/tomcat8080/webapps/ROOT rollback
ant -Dserver.home=/home/$USER/tomcat8081 -Dwebapp.deploy.dir=/home/$USER/tomcat8081/webapps/ROOT -Dserver.shutdown.port=8006 -Dserver.startup.port=8081 rollback
EOF
chown $USER:$USER rollback.sh
chmod +x rollback.sh
fi

#generate backup.sh
if [ ! -f backup.sh ]; then
cat>backup.sh<<EOF
date=`date +%Y-%m-%d`
backupdir=/home/$USER/backup/\$date
if test ! -d \$backupdir
then  mkdir -p \$backupdir
fi
cp -r /var/lib/mysql/xiangling \$backupdir
cp -r /home/$USER/web/assets/upload \$backupdir
mysql -u root -D ironrhino -e "optimize table user;"
olddate=`date +%F -d"-30 days"`
rm -rf /home/$USER/backup/\$olddate*
chown -R $USER:$USER /home/$USER/backup
EOF
chown $USER:$USER backup.sh
chmod +x backup.sh
fi


#iptables
if [ ! -f /etc/init.d/iptables ]; then
cat>/etc/init.d/iptables<<EOF
#!/bin/sh
#
# Startup script for the tomcat
#
# chkconfig: 345 80 15
# description: Tomcat
user=$USER

case "\$1" in
start)
	iptables -A INPUT -s 127.0.0.1 -d 127.0.0.1 -j ACCEPT
	iptables -A INPUT -p tcp --dport 8080 -j DROP
	iptables -A INPUT -p tcp --dport 8081 -j DROP
	iptables -A INPUT -p tcp --dport 8005 -j DROP
	iptables -A INPUT -p tcp --dport 8006 -j DROP
       ;;
stop)
	iptables -F
	iptables -X
	iptables -Z
       ;;
*)
       echo "Usage: \$0 {start|stop}"
       esac

exit 0
EOF
chmod +x /etc/init.d/iptables
update-rc.d iptables defaults
service iptables start
fi

#install or upgrade redis
if ! which redis-server > /dev/null && ! $(ls -l redis-*.tar.gz >/dev/null 2>&1) ; then
wget http://redis.googlecode.com/files/redis-2.6.10.tar.gz
fi
if $(ls -l redis-*.tar.gz >/dev/null 2>&1) ; then
tar xvf redis-*.tar.gz >/dev/null && rm -rf redis-*.tar.gz
rename s/^redis.*$/redis/g redis-*
cd redis && make > /dev/null && make install > /dev/null
cd utils && ./install_server.sh
cd ../../
rm -rf redis
sed -i '31i bind 127.0.0.1' /etc/redis/6379.conf
fi

#svn checkout ironrhino
if [ ! -d ironrhino ];then
svn checkout http://ironrhino.googlecode.com/svn/trunk/ironrhino
chown -R $USER:$USER ironrhino
fi

