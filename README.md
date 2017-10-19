# FPC Agent Installation
This project is an implementation of an IETF DMM FPC Agent.

## Prerequisite Steps

1. Before you build, make sure to set up your OpenDaylight environment. For instructions on how to do this, go to [OpenDaylight instructions](https://wiki.opendaylight.org/view/GettingStarted:Development_Environment_Setup).
## Install FPC Agent
You can install and build FPC Agent manually by command line or automatically by running a script. Skip to the section that reflects how you want to install this agent.

#### Install FPC via Script
1. Download fpc_install.sh from [this project](https://github.com/opendaylight/fpc).
2. Switch to root user and run the installation script.
> sudo su
> ./fpc_install.sh

#### Install FPC via Command Line

1. Clone the FPC Agent code from [this project](https://github.com/opendaylight/fpc).
2. Install [ZeroMQ](http://zeromq.org/area:download).
3. Install the [Python binding for ZeroMQ](http://zeromq.org/bindings:python). 
> pip install pyzmq
4. Run the following command from the fpc root folder to build the FPC agent.
> ./build.sh

## Run FPC Agent

REQUIRED: Before starting this procedure, make sure you install [NGIC](https://gerrit.opencord.org/#/admin/projects/ngic).

1. Start the ZeroMQ service for DPN communication.
> ~/fpc/zmqforwarder/python forwarder_device.py

2. You can optionally start the listener for the southbound DPN ZeroMQ. This listener displays the messages sent on the southbound end of the controller.
> ~/fpc/zmqforwarder/python forwarder_subscriber.py
or
> ~/fpc/zmqforwarder/python jc.forwarder_subscriber.py --quiet

3. Start OpenDaylight. 
Change to the bin directory.
> cd ~/fpc/karaf/target/assembly/bin

Decide whether you want to run FPC Agent in the foreground or in the background by running one of the following commands.

* To run the agent in the foreground with an interactive shell:
> ./karaf

|Foreground with a Shell                  |Other Commands                       |
|-----------------------------------------|-------------------------------------|
|To exit the shell                        |`system:shutdown`                    |
|To view the logs                         |`log:tail`                           |

* To run the agent in the background as a service:
> ./start

|Background as a service                |Other Commands                           |
|---------------------------------------|-----------------------------------------|
|To stop the service                    |`./stop`                                 |
|Look in this directory for logs        |`cd ~/fpc/karaf/target/assembly/data/log`|
After starting, check the jc.forwarder_subscriber.py console for controller notification updates.
5. Run the get-topology script to make sure that the FPC topology state is clear. No DPNs should appear in the result.
> ~/fpc/scripts/get-topology.sh

* If the FPC topology state shows previously registered DPNs, then run the following script to delete them. 
> ~/fpc/scripts/delete_all_dpns.sh
* Run the get-topology script again to verify that no DPNs appear in the result.

#### Push Rules to DP over ZeroMQ
The rules_pub script listens for a DP to come online and then pushes rules via the following scripts: adc_rules, mtr_rules, pcc_rules, and sdf_rules. 

1. Navigate to the test_rules folder.
> cd ~/fpc/zmqforwarder/test_rules
2. Start script to push ADC, PCC, MTR and SDF rules to DP over ZMQ. To run the publisher with an interactive shell:
> ./rules_pub.py

## Start NGIC
At this point, you can run NGIC (Control Plane and Data Plane). For specific steps to start CP and DP, refer to the [NGIC repository](https://gerrit.opencord.org/#/admin/projects/ngic).

1. Start the DP.
2. Verify that DP started by checking the FPC Agent's ./karaf log to see if ngic-dp has been registered.
3. Start the CP.
4. Verify that CP started by checking the FPC Agent's ./karaf log to see if ngic-cp has been registered.
5. At any time, run the script get-topology.sh to show a list of available DPNs in the FPC Agent's topology.
> fpc/scripts/get-topology.sh

At this point, you can run traffic through the ngic. The FPC Agent will estabish and control the flows being requested of the end to end system.
