#/bin/sh

p1=`find sparqlify-debian-tomcat-common/target | grep deb$`
p2=`find sparqlify-debian-tomcat7/target | grep deb$`

sudo dpkg -i "$p1" "$p2"
