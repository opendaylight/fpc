# FPC Agent



This project is an implementation of an IETF DMM FPC Agent.



### Prerequisites

1. Before you build, make sure to set up your OpenDaylight environment. For instructions on how to do this, go to [OpenDaylight's Instructions] (https://wiki.opendaylight.org/view/GettingStarted:Development_Environment_Setup).

2. Clone the FPC Agent code from this repo.

3. Install ZeroMQ from [http://zeromq.org/area:download].

4. Install the Python library for ZeroMQ using a command such as: pip install pyzmq.



### Build FPC Agent

Run this command to build the agent.

```

> ./build.sh

```



### Run FPC Agent

You must launch several processes before you can send a command.



1. Start the ZeroMQ service for DPN communication.



```

> zmqforwarder/python forwarder_device.py

```



2. Start the northbound HTTP notification receiver.



```

> scripts/httptest/python server.py 127.0.0.1 9997

```



3. Start the listener for the DPN ZeroMQ southbound. This displays the messages sent on the southbound end of the controller and replies with an acknowledgement.



```

> zmqforwarder/python forwarder_subscriber_with_ACK.py

```



4. Start up OpenDaylight.



```

> cd ~/fpc/karaf/target/assembly/bin

```

  Decide whether you want to run FPC Agent in the foreground or in the background by running one of the following commands. 

* To run the agent in the foreground with an interactive shell:

```

> ./karaf

```



|Foreground with a shell                  |Other commands                       |

|-----------------------------------------|-------------------------------------|

|To exit the shell                        |`system:shutdown`                    |

|To view the logs                         |`log:tail`                           |



* To run the agent in the background as a service:

```

> ./start

```



|Background as a service                |Other Commands                           |

|---------------------------------------|-----------------------------------------|

|To stop the service                    |`./stop`                                 |

|Look in this directory for logs        |`cd ~/fpc/karaf/target/assembly/data/log`|





5. Execute scripts to prime the server for default tenants and clients.



```

> scripts/bindclient.sh

```



6. At any time, run the script get-topology.sh to get a list of available DPNs.



```

> scripts/get-topology.sh

```



At this point, you can either execute shell scripts under the scripts directory or use jmeter tests under the jmeter directory.





