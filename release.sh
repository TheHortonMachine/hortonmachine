rm -rf extras/export/libs
rm -rf extras/deploy/libs
rm -rf extras/deploy/*.tar.gz
mvn install -Dmaven.test.skip=true -Dmaven.javadoc.skip=true
cd apps
mvn install -Dmaven.test.skip=true -Dmaven.javadoc.skip=true
cd ..
groovy extras/deploy/release.groovy
