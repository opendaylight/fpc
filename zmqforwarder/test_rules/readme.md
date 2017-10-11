# Prerequisite
	- python is installed
	- Configparser is installed 
	- ZeroMQ is installed
	- Python Binding of ZeroMQ is installed

# Run FPC Agent
Following is the sequence of operations to do for launching test_rule.

1. Start the ZeroMQ service for DPN communication.

```
> ~/fpc/zmqforwarder/python forwarder_device.py
```

2. Start the service counts ZMQ messages on the SB, Displays message types.

```
> ~/fpc/zmqforwarder/python jc.forwarder_subscriber.py --quiet
```

3. Start up OpenDaylight.

```
> cd ~/fpc/karaf/target/assembly/bin
```
* To run the agent in the foreground with an interactive shell:

```
> ./karaf
```

Above steps will start FPC. Once FPC is running then use following to start
test_rule program.

#Run Rule Push Script
Start script to push ADC, PCC, MTR and SDF rules to DP over ZMQ

* Go to following folder

```
> cd ~/fpc/zmqforwarder/test_rules
```
* Start script to push ADC, PCC, MTR and SDF rules to DP over ZMQ.
* To run the publisher with an interactive shell:

```
> ./rules_pub.py
```
This script listens for DP to come online and then push rules.

#Run DP
Start up DP

```
Note: This script waits for single DP to come up, push rules on the DP and exit.
To push rules on another DP need to restart the script.

```
