An implementaiton of an IETF DMM FPC Agent.

# Building
./build.sh

# Running
Several processes must be launched prior to sending a command

1. Start the ZeroMQ Service for DPN Communication

```
> zmqforwarder/python forwarder_device.py
```

2. Start the Northbound HTTP Notification Receiver

```
> scripts/httptest/python server.py 127.0.0.1 9997
```

Optionally, there is a listener for the DPN ZeroMQ southbound.

```
> zmqforwarder/python forwarder_subscriber.py
```

3. Execute scripts to prime the server for default tenants and clients.

```
> scripts/bindclient.sh
> scripts/adddpn.sh put
```

At this point you can either execute shell scripts under the scrips directory or use jmeter tests under the jmeter directory.
