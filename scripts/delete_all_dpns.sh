#!/usr/bin/python

import subprocess
import json


topology=subprocess.check_output("curl -i \
--header \"Content-type: application/json\" \
--request GET \
-u admin:admin \
http://localhost:8181/restconf/config/ietf-dmm-fpcagent:tenants/tenant/default/fpc-topology 2>/dev/null | grep \{.*\}", shell=True)

#print topology

topology_json=json.loads(topology)

#print json.dumps(topology_json, indent=4)

fpc_topology_json=topology_json['fpc-topology']

#print json.dumps(fpc_topology_json, indent=4)

if len(fpc_topology_json) != 0:

    dpns=fpc_topology_json['dpns']

    for dpn_entry in dpns:
        #print json.dumps(dpn_entry, indent=4)
        dpn_id=dpn_entry['dpn-id']

        #print "Deleting dpn-id named %s" % (dpn_id)

        cmd="curl -i \
        --header \"Content-type: application/json\" \
        --request delete \
        -u admin:admin \
        --data '{\
            \"dpns\": [\
                {\
                    \"dpn-id\": %s,\
                    \"dpn-name\": \"site1-anchor1\",\
                    \"dpn-groups\": [\
                        \"foo\"\
                    ],\
                    \"node-id\": \"node23\",\
                    \"network-id\": \"network22\"\
                }\
            ]\
        }' \
        http://localhost:8181/restconf/config/ietf-dmm-fpcagent:tenants/tenant/default/fpc-topology/dpns/%s" % (dpn_id, dpn_id)

        #print cmd

        print subprocess.check_output(cmd, shell=True)

    topology=subprocess.check_output("curl -i \
    --header \"Content-type: application/json\" \
    --request GET \
    -u admin:admin \
    http://localhost:8181/restconf/config/ietf-dmm-fpcagent:tenants/tenant/default/fpc-topology 2>/dev/null | grep \{.*\}", shell=True)

    topology_json=json.loads(topology)

print json.dumps(topology_json, indent=4)

