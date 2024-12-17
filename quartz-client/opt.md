

## sign
+ use win cmd
certUtil -hashfile quartz-client-2.3.2.jar md5 >> quartz-client-2.3.2.jar.md5
certUtil -hashfile quartz-client-2.3.2.jar sha1 >> quartz-client-2.3.2.jar.sha1

certUtil -hashfile quartz-client-2.3.2-sources.jar md5 >> quartz-client-2.3.2-sources.jar.md5
certUtil -hashfile quartz-client-2.3.2-sources.jar sha1 >> quartz-client-2.3.2-sources.jar.sha1

certUtil -hashfile quartz-client-2.3.2-javadoc.jar md5 >> quartz-client-2.3.2-javadoc.jar.md5
certUtil -hashfile quartz-client-2.3.2-javadoc.jar sha1 >> quartz-client-2.3.2-javadoc.jar.sha1

certUtil -hashfile quartz-client-2.3.2.pom md5 >> quartz-client-2.3.2.pom.md5
certUtil -hashfile quartz-client-2.3.2.pom sha1 >> quartz-client-2.3.2.pom.sha1

## clear content

+ use git bash 
sed -i '1d;3d' quartz-client-2.3.2.jar.md5
sed -i '1d;3d' quartz-client-2.3.2.pom.md5
sed -i '1d;3d' quartz-client-2.3.2-javadoc.jar.md5
sed -i '1d;3d' quartz-client-2.3.2-sources.jar.md5

sed -i '1d;3d' quartz-client-2.3.2.jar.sha1
sed -i '1d;3d' quartz-client-2.3.2.pom.sha1
sed -i '1d;3d' quartz-client-2.3.2-javadoc.jar.sha1
sed -i '1d;3d' quartz-client-2.3.2-sources.jar.sha1

