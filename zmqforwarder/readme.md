# FPC Agent ZeroMQ Scripts
These scripts are used to view the messages sent on the southbound end of the FPC Agent.
1. Make sure forwarder_device.py is running, as described in the main README.md file. The forwarder_device sets up the queues and the connectivity between the publishers and subscribers.
2. Run one or both of the following scripts, depending on what you want to do.
    * Use forwarder_subscriber_with_ACK.py, which is a dataplane message emmulator. It allows you to view the southbound messages sent from the FPC Agent over ZeroMQ and reply with an ACK message. Do not use this script when running NGIC. 
    * Use jc.forwarder_subscriber.py to view the southbound messages sent from the FPC Agent over ZeroMQ without sending an ACK message. To view statistics (number of messages for each message type) that are refreshed every second, run this script with the --quiet option.