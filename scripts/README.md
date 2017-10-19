# FPC Agent Scripts
This directory contains scripts to execute against the FPC Agent and simulate the DPN. 
You can execute the following general scripts at any time. 
* agent-info.sh provides agent information. 
* getstats.sh shows the transactions and state entry counts. 
* get-topology.sh shows the DPNs within the FPC Agent's topology tree.
* delete_all_dpns.sh deletes all DPNs within the FPC Agent's topology tree.

## ADC Rule scripts
The adc_rules directory contains scripts to add ADC rules to the FPC Agent's config data store and to send these rules to a DPN.
The [create_port.sh](./adc_rules/create_port.sh) script is the driver script that uses the other scripts in the directory to create actions, descriptions, policies, policy groups, ports, and contexts. 

## DPN Mirror scripts
The dpn_mirror directory contains the scripts to add, delete, and configure abstract DPNs within the FPC Agent's topology tree. 
The [vdpnDemo.sh](./dpn_mirror/vdpnDemo.sh) script is the driver script that uses other scripts in the directory to create and delete abstract DPNs, and to add and remove DPNs to and from abstract DPNs.

## Note
At this time, do not run the scripts in the http_post directory because they send requests over http post. FPC Agent currently receives requests via http streams. The FPC Agent does not currently support requests over http posts but it will in future versions. 