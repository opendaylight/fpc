# FPC Agent
This project is an implementation of an IETF DMM FPC Agent.

## Installation
### OPTION A- Manual installation
#### Download prerequisites
1. Before you build, make sure to set up your OpenDaylight environment. For instructions on how to do this, go to [OpenDaylight's Instructions] (https://wiki.opendaylight.org/view/GettingStarted:Development_Environment_Setup).

2. Clone the FPC Agent code from this repo.

3. Install ZeroMQ from [http://zeromq.org/area:download].

4. Install the Python library for ZeroMQ using a command such as: pip install pyzmq.

#### Build FPC Agent
Run this command to build the agent.
> ./build.sh

### OPTION B- Automated installation
1. Download fpc_install.sh from https://github.com/sprintlabs/fpc/tree/dev-stable

2. Switch to root user and run fpc_installer
> sudo su
> ./fpc_install.sh

## Run FPC Agent
### OPTION A- Standalone run
You must launch several processes before you can send a command.

1. Start the ZeroMQ service for DPN communication.
> ~/fpc/zmqforwarder/python forwarder_device.py

2. Start the northbound HTTP notification receiver.

> ~/fpc/scripts/httptest/python server.py 127.0.0.1 9997

3. Start the listener for the DPN ZeroMQ southbound. This displays the messages sent on the southbound end of the controller and replies with an acknowledgement.
> ~/fpc/zmqforwarder/python forwarder_subscriber_with_ACK.py

4. Start up OpenDaylight.
> cd ~/fpc/karaf/target/assembly/bin

  Decide whether you want to run FPC Agent in the foreground or in the background by running one of the following commands.

* To run the agent in the foreground with an interactive shell:
> ./karaf

|Foreground with a shell                  |Other commands                       |
|-----------------------------------------|-------------------------------------|
|To exit the shell                        |`system:shutdown`                    |
|To view the logs                         |`log:tail`                           |

* To run the agent in the background as a service:
> ./start

|Background as a service                |Other Commands                           |
|---------------------------------------|-----------------------------------------|
|To stop the service                    |`./stop`                                 |
|Look in this directory for logs        |`cd ~/fpc/karaf/target/assembly/data/log`|


5. Execute scripts to prime the server for default tenants and clients.
> ~/fpc/scripts/bindclient.sh

6. At any time, run the script get-topology.sh to get a list of available DPNs.
> ~/fpc/scripts/get-topology.sh

At this point, you can either execute shell scripts under the scripts directory or use jmeter tests under the jmeter directory.

### OPTION B- E2E Integrated system run w/ ngic
#### REQUIRED: ngic from https://gerrit.opencord.org/#/admin/projects/ngic
1. Start the ZeroMQ service for DPN communication.
> ~/fpc/zmqforwarder/python forwarder_device.py

2. Start the listener for the DPN ZeroMQ southbound. This displays the messages sent on the southbound end of the controller and replies with an acknowledgement.
> ~/fpc/zmqforwarder/python forwarder_subscriber.py
or
> ~/fpc/zmqforwarder/python jc.forwarder_subscriber.py --quiet

4. Start up OpenDaylight.
> cd ~/fpc/karaf/target/assembly/bin
Check console of jc.forwarder_subscriber.py for controller notification updates.

5. Ensure FPC topology state is clear
> ~/fpc/scripts/get-topology.sh
Check result to see no dpns are listed
If FPC topology state has previously registered dpns then
> ~/fpc/scripts/delete_all_dpns.sh
Check again result to see no dpns are listed
Note: ~/fpc/scripts/bindclient.sh is not required in teh E2E integrated system run with NGIC. The ngic-cp client bind to the fpc.

6. Start up OpenDaylight.
> cd ~/fpc/karaf/target/assembly/bin

  Decide whether you want to run FPC Agent in the foreground or in the background by running one of the following commands.

* To run the agent in the foreground with an interactive shell:
> ./karaf

|Foreground with a shell                  |Other commands                       |
|-----------------------------------------|-------------------------------------|
|To exit the shell                        |`system:shutdown`                    |
|To view the logs                         |`log:tail`                           |

* To run the agent in the background as a service:
> ./start

|Background as a service                |Other Commands                           |
|---------------------------------------|-----------------------------------------|
|To stop the service                    |`./stop`                                 |
|Look in this directory for logs        |`cd ~/fpc/karaf/target/assembly/data/log`|


|Following step is optional if Gx exists and FPC pushes initialization rules. It will push configured rules on DPs on behalf of FPC|
7. Start FPC test script as follows. This script will wait for DP to start and then will
push the test rules on DP. Refer test_rules/readme.md for more details.
> cd ~/fpc/zmqforwarder/test_rules/
> ./rules_pub.py

|At this point the ngic can be started. Refer ngic documentation

8. Start the ngic-dp
Check console of jc.forwarder_subscriber.py for dpn  notification updates.

9. Start the ngic-cp
Check ./karaf log to see ngic-cp has been registered.

10. At any time, run the script get-topology.sh to get a list of available DPNs.
> scripts/get-topology.sh

At this point, you can run traffic through the ngic. The FPC controller will estabish and control the flows being requested of the E2E system

