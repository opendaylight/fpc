#!/bin/bash

export MVN_REPO=$HOME/.m2/repository
export CLASSPATH=$MVN_REPO/org/opendaylight/fpc/fpc-impl/0.1.0-SNAPSHOT/fpc-impl-0.1.0-SNAPSHOT.jar:$MVN_REPO/commons-cli/commons-cli/1.3.1/commons-cli-1.3.1.jar:$MVN_REPO/org/opendaylight/mdsal/model/ietf-inet-types-2013-07-15/1.1.0-SNAPSHOT/ietf-inet-types-2013-07-15-1.1.0-SNAPSHOT.jar:$MVN_REPO/com/google/guava/guava/19.0/guava-19.0.jar:$MVN_REPO/org/apache/karaf/org.apache.karaf.client/3.0.7/org.apache.karaf.client-3.0.7.jar:$MVN_REPO/org/slf4j/slf4j-api/1.7.21/slf4j-api-1.7.21.jar

java  org.opendaylight.fpc.activation.impl.dpdkdpn.DpnCmd2 -method delete_session -ip 127.0.0.1 -port 5559 -default_ebi 6 -s1u_sgw_gtpu_teid 45123
