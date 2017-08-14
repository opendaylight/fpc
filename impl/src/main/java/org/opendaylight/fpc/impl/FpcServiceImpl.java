/*
 * Copyright © 2016 - 2017 Copyright (c) Sprint, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.fpc.impl;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.NotificationPublishService;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.fpc.activation.workers.ActivationThreadPool;
import org.opendaylight.fpc.activation.workers.MonitorThreadPool;
import org.opendaylight.fpc.tenant.TenantManager;
import org.opendaylight.fpc.utils.ErrorLog;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.ClientIdentifier;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.FpcAgentInfo;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Uri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.fpc.config.rev160927.FpcConfig;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.fpc.rev150105.ConnectionInfo;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.fpc.rev150105.ConnectionInfoBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.fpc.rev150105.DeregisterClientInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.fpc.rev150105.DeregisterClientOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.fpc.rev150105.FpcService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.fpc.rev150105.RegisterClientInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.fpc.rev150105.RegisterClientOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.fpc.rev150105.RegisterClientOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.fpc.rev150105.connection.info.Connections;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.fpc.rev150105.connection.info.ConnectionsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.fpc.rev150105.connection.info.ConnectionsKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.fpc.rev150105.connection.info.connections.AssignedInfoBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.fpc.rev150105.connection.info.connections.RequestedInfoBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcError;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.Futures;

/**
 * FPC Service used for Client registration.
 */
public class FpcServiceImpl implements FpcService {
    private static final Logger LOG = LoggerFactory.getLogger(FpcServiceImpl.class);
    private static AtomicInteger clientNumericId = new AtomicInteger(0);
    private static ConcurrentMap<String, Connections> connections = new ConcurrentHashMap<String, Connections>();
    private static int lastHashCode = -1;
    private final DataBroker dataBroker;
    private final NotificationPublishService notificationService;
    private final ActivationThreadPool activationService;
    private final MonitorThreadPool monitorService;

    /**
     * Returns a Notification Uri for a specific Client.
     * @param clientId - Client Identifier
     * @return Uri associated with the specified Client Identifier or null otherwise
     */
    public static Uri getNotificationUri(ClientIdentifier clientId) {
        Connections conn = connections.get(clientId.toString());
        return (conn != null) ? conn.getAssignedInfo().getEndpointUri() : null;
    }

    /**
     * Returns a Notification Uri for a specific Client.
     * @param clientId - String Value of the Client Identifier
     * @return Uri associated with the specified Client Identifier or null otherwise
     */
    public static Uri getNotificationUri(String clientId) {
        Connections conn = connections.get(clientId);
        return (conn != null) ? conn.getAssignedInfo().getEndpointUri() : null;
    }

    /**
     * Default Constructor.
     *
     * @param dataBroker - Data Broker
     * @param notificationService - Notification Service
     * @param monitorService - Monitor Service
     * @param activationService - Activation Service
     */
    public FpcServiceImpl(DataBroker dataBroker,
            NotificationPublishService notificationService,
            MonitorThreadPool monitorService,
            ActivationThreadPool activationService) {
        this.dataBroker = dataBroker;
        this.notificationService = notificationService;
        this.activationService = activationService;
        this.monitorService = monitorService;
        load();
    }

    // TODO - Ensure this hashcode check is valid for conncurrent hashmap updates
    /**
     * Checkpoints the Connection Information to the data store.
     */
    private void checkPoint() {
        WriteTransaction wtrans = dataBroker.newWriteOnlyTransaction();
        try {
            if (connections.hashCode() == lastHashCode) {
                return;
            }
            if (connections.isEmpty()) {
                wtrans.delete(LogicalDatastoreType.OPERATIONAL,
                        InstanceIdentifier.create(ConnectionInfo.class));
            } else {
                wtrans.put(LogicalDatastoreType.OPERATIONAL,
                        InstanceIdentifier.create(ConnectionInfo.class),
                        new ConnectionInfoBuilder()
                            .setConnections(new ArrayList<Connections>(connections.values()))
                            .build(), true);
            }
            wtrans.submit();
            lastHashCode = connections.hashCode() ;
        }catch (Exception exc) {
            ErrorLog.logError("FpcServiceImpl - Error storing Connection information from the datastore\n" + exc.getMessage(), exc.getStackTrace());
        }
    }

    /**
     * Loads the Connection Information from the data store
     */
    private void load() {
        ReadOnlyTransaction rtrans = dataBroker.newReadOnlyTransaction();
        try {
            Optional<ConnectionInfo> connInfo = rtrans.read(LogicalDatastoreType.OPERATIONAL,
                              InstanceIdentifier.create(ConnectionInfo.class)).get();
            if (connInfo.isPresent()) {
                for (Connections conn : (connInfo.get().getConnections() == null) ?
                        Collections.<Connections>emptyList() : connInfo.get().getConnections()) {
                    connections.put(conn.getAssignedInfo().getClientId().toString(), conn);
                }
            }
        } catch (Exception exc) {
            ErrorLog.logError("FpcServiceImpl - Error in loading Connection information from the datastore\n" + exc.getMessage(), exc.getStackTrace());
        }
    }

    @Override
    public Future<RpcResult<RegisterClientOutput>> registerClient(RegisterClientInput input) {
        ReadOnlyTransaction rtrans = dataBroker.newReadOnlyTransaction();
        try {
            // Validate URI, if present, to be a properly formed URL
            if (input.getEndpointUri() != null) {
                try {
                    URI uri = URI.create(input.getEndpointUri().getValue());
                    uri.toURL();
                } catch (Exception e) {
                    return Futures.immediateFuture(RpcResultBuilder.<RegisterClientOutput>failed()
                            .withRpcError(RpcResultBuilder.newError(RpcError.ErrorType.PROTOCOL,
                                    "client error",
                                    "Uri " + input.getEndpointUri().getValue() + " is invalid"))
                            .build());
                }
            }

            Connections newConn = null;
            Optional<FpcAgentInfo> oagent =
                      rtrans.read(LogicalDatastoreType.OPERATIONAL,
                              InstanceIdentifier.create(FpcAgentInfo.class)).get();
            if (!oagent.isPresent()) {
                return Futures.immediateFuture(RpcResultBuilder.<RegisterClientOutput>failed()
                        .withError(RpcError.ErrorType.APPLICATION,
                                "internal application error",
                                "Agent Information could not be found to complete Client binding.")
                        .build());
            } else {
                FpcAgentInfo info = oagent.get();
                FpcConfig conf = FpcProvider.getInstance().getConfig();
                Set<String> convergedFeatures = new HashSet<String>(info.getSupportedFeatures());
                convergedFeatures.addAll(input.getSupportedFeatures());

                if (!convergedFeatures.contains("urn:ietf:params:xml:ns:yang:fpcagent:instruction-bitset")) {
                    return Futures.immediateFuture(RpcResultBuilder.<RegisterClientOutput>failed()
                            .withError(RpcError.ErrorType.APPLICATION,
                                    "Requested Configuration NOT supported",
                                    "This Agent requires the following feature(s) to be supported but were not present" +
                                    "in the binding request." +
                                    "urn:ietf:params:xml:ns:yang:fpcagent:instruction-bitset")
                            .build());
                }

                if (convergedFeatures.contains("urn:ietf:params:xml:ns:yang:fpcagent:fpc-client-assignments") &&
                        convergedFeatures.contains("urn:ietf:params:xml:ns:yang:fpcagent:fpc-agent-assignments")) {
                    if (!conf.isPrefersFastClients()) {
                        convergedFeatures.remove("urn:ietf:params:xml:ns:yang:fpcagent:fpc-client-assignments");
                    } else {
                        convergedFeatures.remove("urn:ietf:params:xml:ns:yang:fpcagent:fpc-agent-assignments");
                    }
                }

                ClientIdentifier clientId = input.getClientId();
                if ((clientId == null) || connections.containsKey(clientId.toString())) {
                    while (connections.containsKey(new ClientIdentifier(clientNumericId.longValue()).toString())) {
                        if (clientNumericId.incrementAndGet() == Integer.MAX_VALUE) {
                            clientNumericId.set(0);
                        }
                    }
                    clientId = new ClientIdentifier(clientNumericId.longValue());
                    clientNumericId.incrementAndGet();
                }

                FpcagentServiceBase agentServiceStrategy = null;
                if (!convergedFeatures.contains("urn:ietf:params:xml:ns:yang:fpcagent:fpc-client-assignments")) {
                    agentServiceStrategy = new FpcAssignmentPhaseImpl(dataBroker,
                            activationService,
                            monitorService.getWorker(),
                            notificationService,
                            conf);
                } else {
                    agentServiceStrategy = new FpcAssignmentPhaseNoassignImpl(dataBroker,
                            activationService,
                            monitorService.getWorker(),
                            notificationService,
                            conf);
                }

                newConn = new ConnectionsBuilder()
                        .setClientId(clientId.toString())
                        .setKey(new ConnectionsKey(clientId.toString()))
                        .setRequestedInfo(new RequestedInfoBuilder(input).build())
                        .setAssignedInfo(new AssignedInfoBuilder(input)
                            .setSupportedFeatures(new ArrayList<String>(convergedFeatures))
                            .setClientId(clientId)
                            .build())
                        .build();
                connections.put(newConn.getClientId().toString(), newConn); // Add

                TenantManager tenantContext = TenantManager.getTenantManager(input.getTenantId());
                if (tenantContext == null) {
                    LOG.info("Populating Tenant {} per Connection Request from {}", input.getTenantId(),
                            input.getClientId());
                    tenantContext = TenantManager.populateTenant(input.getTenantId());
                }
                TenantManager.registerClient(clientId, input.getTenantId()); // Add
                FpcagentDispatcher.addStrategy(clientId, agentServiceStrategy); // Add
                if (agentServiceStrategy.requiresAssignmentManager() &&
                        (tenantContext.getAssignmentManager() == null)) {
                    tenantContext.genAssignmentManager();
                }

                checkPoint();
            }

            LOG.info("New Connection Created\n" + newConn.toString());
            return Futures.immediateFuture(RpcResultBuilder.<RegisterClientOutput>success(
                        new RegisterClientOutputBuilder(newConn.getAssignedInfo())
                        .build())
                    .build());
        } catch (Exception exc) {
            ErrorLog.logError("Error in new Connection Binding" + exc.getMessage(), exc.getStackTrace());
            return Futures.immediateFuture(RpcResultBuilder.<RegisterClientOutput>failed()
                    .withError(RpcError.ErrorType.APPLICATION,
                            "Error Occurred During Client Binding",
                            exc)
                    .build());
        }
    }

    @Override
    public Future<RpcResult<DeregisterClientOutput>> deregisterClient(DeregisterClientInput input) {
         try {
             if (connections.remove(input.getClientId().toString()) != null) {
                 TenantManager.deregisterClient(input.getClientId());
                 FpcagentDispatcher.removeStrategy(input.getClientId());
                 LOG.info("Connection Removed for Client {}", input.getClientId());
                 checkPoint();
             }
             return Futures.immediateFuture(RpcResultBuilder.<DeregisterClientOutput>success().build());
         } catch (Exception exc) {
            ErrorLog.logError("Error in new Connection Binding" + exc.getMessage(), exc.getStackTrace());
            return Futures.immediateFuture(RpcResultBuilder.<DeregisterClientOutput>failed()
                    .withError(RpcError.ErrorType.APPLICATION,
                            "Error Occurred During Client Binding",
                            exc)
                    .build());
        }
    }
}
