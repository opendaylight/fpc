#!/bin/bash
mvn clean install
sed -i -e '85s/getValue().//' api/target/generated-sources/mdsal-binding/org/opendaylight/yang/gen/v1/urn/ietf/params/xml/ns/yang/fpcbase/rev160803/FpcIdentity.java
cd api
mvn install
cd ..
mvn clean install -rf :fpc-impl
