#!/bin/bash
MVNEXEC=mvn
cd impl
$MVNEXEC clean install -DskipTests
cd ../karaf
$MVNEXEC clean install -DskipTests
#sed -i -e '85s/getValue().//' api/target/generated-sources/mdsal-binding/org/opendaylight/yang/gen/v1/urn/ietf/params/xml/ns/yang/fpcbase/rev160803/FpcIdentity.java
#cd api
#$MVNEXEC install
#cd ..
#$MVNEXEC clean install -rf :fpc-impl -DskipTests
