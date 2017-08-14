/*
 * Copyright © 2016 - 2017 Copyright (c) Sprint, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.fpc.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.NotificationPublishService;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.RpcRegistration;
import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry;
import org.opendaylight.fpc.activation.ActivatorFactory;
import org.opendaylight.fpc.activation.cache.StorageWriter;
import org.opendaylight.fpc.activation.cache.transaction.Metrics;
import org.opendaylight.fpc.activation.cache.transaction.WriteToCache;
import org.opendaylight.fpc.activation.impl.dpdkdpn.DpdkImplFactory;
import org.opendaylight.fpc.activation.workers.ActivationThreadPool;
import org.opendaylight.fpc.activation.workers.MonitorThreadPool;
import org.opendaylight.fpc.impl.memcached.MemcachedThreadPool;
import org.opendaylight.fpc.impl.zeromq.ZMQNBIServerPool;
import org.opendaylight.fpc.impl.zeromq.ZMQSBListener;
import org.opendaylight.fpc.impl.zeromq.ZMQSBMessagePool;
import org.opendaylight.fpc.monitor.Events;
import org.opendaylight.fpc.monitor.ScheduledMonitors;
import org.opendaylight.fpc.notification.HTTPClientPool;
import org.opendaylight.fpc.tenant.TenantManager;
import org.opendaylight.fpc.utils.ErrorLog;
import org.opendaylight.fpc.utils.FpcCodecUtils;
import org.opendaylight.fpc.utils.StringConstants;
import org.opendaylight.fpc.utils.zeromq.ZMQClientPool;
import org.opendaylight.netconf.sal.restconf.api.JSONRestconfService;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.FpcAgentInfo;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.FpcAgentInfoBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.IetfDmmFpcagentService;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.Tenants;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.TenantsBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.tenants.Tenant;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.tenants.TenantBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.tenants.TenantKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcbase.rev160803.FpcDpnControlProtocol;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcbase.rev160803.FpcIdentity;
import org.opendaylight.yang.gen.v1.urn.opendaylight.fpc.config.rev160927.FpcConfig;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.fpc.rev150105.FpcService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.fpc.rev150105.ZmqDpnControlProtocol;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.OperationFailedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeromq.ZContext;

import com.google.common.base.Optional;

/**
 * Fpc Provider implementation.
 */
public class FpcProvider implements AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(FpcProvider.class);
    private static FpcProvider _instance = null;
    private final DataBroker dataBroker;
    private final RpcProviderRegistry rpcRegistryDependency;
    private final NotificationPublishService notificationService;
    private FpcConfig config;

    private RpcRegistration<IetfDmmFpcagentService> fpcService;
    private RpcRegistration<FpcService> fpcCoreServices;
    private RpcRegistration<IetfDmmFpcagentService> assignmentService;

    // Directly Managed Thread Pools
    private ActivationThreadPool activationService;
    private MonitorThreadPool monitorService;
    private HTTPClientPool httpNotifierPool;
    private ZMQNBIServerPool zmqNbi;
    private ZMQSBListener zmqSbListener;
    private WriteToCache wtc;
    private Thread writeToCache;
    /**
     * Returns the instance of the FpcProvider
     * @return FpcProvider instance
     */
    public static FpcProvider getInstance() {
        return _instance;
    }

    /**
     * Constructor.
     * @param dataBroker - MD-SAL DataBroker
     * @param rpcRegistryDependency - RPC Provider Registry
     * @param notificationService - Notification Publishing Service
     * @param config - FPC Configuration
     * @throws Exception - if the configuration is null or an error occurs when starting any of the underlying services.
     */
    public FpcProvider(final DataBroker dataBroker,
            RpcProviderRegistry rpcRegistryDependency,
            NotificationPublishService notificationService,
            FpcConfig config) throws Exception {
        LOG.info("FpcProvider Started");
        _instance = this;
        this.dataBroker = dataBroker;
        this.rpcRegistryDependency = rpcRegistryDependency;
        this.notificationService = notificationService;
        this.config = config;

        if (config == null) {
            throw new Exception("FpcProvider - configuration has not been set! Exiting...");
        }
        reportConfig();
        if(config.isUseMemcached()){
	        try {
		            MemcachedThreadPool.createInstance(config.getMemcachedThreads(),config.getMemcachedUri());
		            MemcachedThreadPool.getInstance().start();
		            MemcachedThreadPool.getInstance().run();
		    } catch(Exception e) {
		            ErrorLog.logError(e.getStackTrace());
		        close();
		        throw new Exception("FpcProvider - Error during start/run for Memcached Thread Pool. Exiting...");
		    }
        }

        try {
            ZMQClientPool.createInstance(new ZContext(),
                    config.getDpnClientUri(),
                    config.getDpnClientThreads());
            ZMQClientPool.getInstance().start();
            ZMQClientPool.getInstance().run();
        } catch (Exception e) {
        	ErrorLog.logError(e.getStackTrace());
            close();
            throw new Exception("FpcProvider - Error during start/run for ZMQ Client Pool. Exiting...");
        }

        try {
            ZMQSBMessagePool.createInstance(
                    config.getDpnMessageProcessorThreads(),config.getNodeId(),config.getNetworkId());
            ZMQSBMessagePool.getInstance().start();
            ZMQSBMessagePool.getInstance().run();
        } catch (Exception e) {
        	ErrorLog.logError(e.getStackTrace());
            close();
            throw new Exception("FpcProvider - Error during start/run for ZMQ SB Message Pool. Exiting...");
        }

        StorageWriter.init(dataBroker, config.getMobilityupdateMs());

        FpcIdentity defaultTenantId = new FpcIdentity(config.getDefaultTenantId());
        Map<Class<? extends FpcDpnControlProtocol>,ActivatorFactory> cpFactories =
                new HashMap<Class<? extends FpcDpnControlProtocol>,ActivatorFactory>();
        cpFactories.put(ZmqDpnControlProtocol.class, new DpdkImplFactory());
        TenantManager.populateTenant(defaultTenantId, cpFactories);

        try{
        	wtc = new WriteToCache();
        	wtc.start();
        	writeToCache = new Thread(wtc);
        	writeToCache.start();
        } catch (Exception e) {
        	ErrorLog.logError(e.getLocalizedMessage(),e.getStackTrace());
        }

        this.activationService = new ActivationThreadPool(dataBroker,config.getActivationThreads());
        this.monitorService = new MonitorThreadPool(dataBroker, config.getMonitorThreads());

        Metrics.init(dataBroker, config.getMetricsupdateMs());

        try {
            activationService.start();
            activationService.run();
        } catch (Exception e) {
        	ErrorLog.logError(e.getStackTrace());
            close();
            throw new Exception("FpcProvider - Error during start/run for activation services. Exiting...");
        }

        try {
            monitorService.start();
            monitorService.run();
        } catch (Exception e) {
        	ErrorLog.logError(e.getStackTrace());
            close();
            throw new Exception("FpcProvider - Error during start/run for monitor services. Exiting...");
        }

        this.httpNotifierPool= (config.getHttpNotifierClients() > 0) ?
                HTTPClientPool.init(config.getHttpNotifierClients()) : null;

        if (httpNotifierPool != null) {
            httpNotifierPool.start();
            httpNotifierPool.run();
        }

        //TODO - Migrate all pools to named threads
        //ZMQ SB Service
        this.zmqNbi = new ZMQNBIServerPool(
                config.getZmqNbiServerUri(),
                config.getZmqNbiInprocUri(),
                config.getZmqNbiServerPoolsize(),
                config.getZmqNbiHandlerPoolsize());

        //ZMQ SB Listener
        zmqSbListener = new ZMQSBListener(config.getDpnListenerUri(), config.getZmqBroadcastAll(), config.getZmqBroadcastControllers(), config.getZmqBroadcastDpns(), config.getNodeId(), config.getNetworkId());
        zmqSbListener.open();

        ScheduledMonitors.init(config.getScheduledMonitorsPoolsize());

        fpcService = null;
        assignmentService = null;


        LOG.info("FpcProvider - Constructor Complete");
    }

    /**
     * FPC Configuration retrieval.
     * @return FpcConfig
     */
    public FpcConfig getConfig() {
        return config;
    }

    /**
     * Reports the FPC Configuration to the LOG system.
     */
    public void reportConfig() {
        if (config == null) {
            LOG.info("FPC-Configuration: Value is null");
        } else {
            LOG.info("FPC-Configuration = {}\n", config);
        }
    }

	public void initJSON() {
    	Object[] instances =  FpcCodecUtils.getGlobalInstances(JSONRestconfService.class, this);
    	Object service = (instances != null) ? (JSONRestconfService) instances[0] : null;
    	if (service == null) {
    		ErrorLog.logError("JSONRestconfService was NOT available at the moment of the worker's constructor");
    	} else {
    		try {
    			ErrorLog.logError("!!!!---!!!! These Errors are expected !!!!---!!!! ");
				Optional<String> output = ((JSONRestconfService) service).invokeRpc("fpc:register_client", Optional.of(StringConstants.bindClient));
				if(output.isPresent()){
					LOG.info("Bind Client Output: "+output.get());
				}
				output = ((JSONRestconfService) service).invokeRpc("ietf-dmm-fpcagent:configure", Optional.of(StringConstants.context));
				if(output.isPresent()){
					LOG.info("Context Create Output: "+output.get());
				}
				output = ((JSONRestconfService) service).invokeRpc("ietf-dmm-fpcagent:configure", Optional.of(StringConstants.contextDelete));
    			if(output.isPresent()){
    				LOG.info("Context Delete Output: "+output.get());
    			}
    			output = ((JSONRestconfService) service).invokeRpc("fpc:deregister_client", Optional.of(StringConstants.unbindClient));
    			if(output.isPresent()){
    				LOG.info("Unbind Client Output: "+output.get());
    			}
			} catch (OperationFailedException e) {
				ErrorLog.logError(e.getLocalizedMessage(),e.getStackTrace());
			} catch (Exception e){
				ErrorLog.logError(e.getLocalizedMessage(),e.getStackTrace());
			}
    		ErrorLog.logError("!!!!---!!!! End of expected errors !!!!---!!!! ");
    	}

    }

    /**
     * Method called when the blueprint container is created.
     */
    public void init() {
        LOG.info("FpcProvider Session Initiated");
        prepStore(LogicalDatastoreType.CONFIGURATION);
        prepStore(LogicalDatastoreType.OPERATIONAL);

        // Prepare the default Activators.
        prepActivators();

       assignmentService =
               rpcRegistryDependency.addRpcImplementation(IetfDmmFpcagentService.class, new FpcagentDispatcher());

        fpcCoreServices =
                rpcRegistryDependency.addRpcImplementation(FpcService.class,
                        new FpcServiceImpl(this.dataBroker, this.notificationService, this.monitorService, this.activationService));
        initJSON();
    }

    /**
     * Prepares the Activator(s) supported by the FPC implementation.
     */
    protected void prepActivators() {
        LOG.info("Registering Activator(s)");
        TenantManager.addDefaultActivatorFactory(new DpdkImplFactory());
    }

    /**
     * Prepares a datastore with default stuctures.
     * @param dsType - Target DataStore Type (CONFIG or OPERATIONAL).
     */
    protected void prepStore(LogicalDatastoreType dsType) {
        ReadOnlyTransaction rtrans = dataBroker.newReadOnlyTransaction();
        try {
            LOG.info("Checking DataStore: " + dsType.toString());
            final String[] commonFeatures =  {
                    //"urn:ietf:params:xml:ns:yang:fpcagent:fpc-basename-registry"
                    //"urn:ietf:params:xml:ns:yang:fpcagent:fpc-cloning"
                    "urn:ietf:params:xml:ns:yang:fpcagent:fpc-bundles",
                    "urn:ietf:params:xml:ns:yang:fpcagent:fpc-client-binding",
                    "urn:ietf:params:xml:ns:yang:fpcagent:operation-ref-scope",
                    "urn:ietf:params:xml:ns:yang:fpcagent:fpc-agent-assignments"
                };

            // TODO - Add a Config parameter to set the Agent into single/multi-dpn mode
            String [] defaultFeatures = {
                "urn:ietf:params:xml:ns:yang:fpcagent:instruction-bitset",
                "urn:ietf:params:xml:ns:yang:fpcagent:fpc-multi-dpn",
                "urn:ietf:params:xml:ns:yang:fpcagent:fpc-client-assignments"
            };

            // Agent Info
            LOG.info("Agent Check Start");
            Optional<FpcAgentInfo> oagent =
                      rtrans.read(dsType, InstanceIdentifier.create(FpcAgentInfo.class)).get();
            if (!oagent.isPresent()) {
                  LOG.info("Populating Agent Structure.");
                  FpcAgentInfoBuilder bldr = new FpcAgentInfoBuilder();
                  ArrayList<String> araList = new ArrayList<>();
                  araList.addAll(Arrays.asList(commonFeatures));
                  araList.addAll(Arrays.asList(defaultFeatures));
                  bldr.setSupportedEvents(Events.getSupportedEvents())
                      .setSupportedFeatures(araList);
                  WriteTransaction wt = dataBroker.newWriteOnlyTransaction();
                  wt.put(dsType, InstanceIdentifier.create(FpcAgentInfo.class), bldr.build(), true);
                  wt.submit();
            }

            LOG.info("Agent Check Completed");
        } catch (Exception exc) {
            ErrorLog.logError("Error in Agent Stucture Checking\n" + exc.getMessage(), exc.getStackTrace());
        }

        try {
            // Topology Check - Only need to do work if we are in single-mode
            LOG.info("Topology Check Start");
            // TODO - Multi Tenant Support
            Optional<Tenants> otopo =
                      rtrans.read(dsType,InstanceIdentifier.create(Tenants.class))
                              .get();

            if (!otopo.isPresent()) {
                LOG.info("Populating Topology Structure.");
                FpcIdentity fpcid = new FpcIdentity(config.getDefaultTenantId());
                TenantBuilder tb = new TenantBuilder();
                tb.setTenantId(fpcid)
                  .setKey(new TenantKey(fpcid));
                List<Tenant> tenants = new ArrayList<Tenant>();
                tenants.add(tb.build());
                WriteTransaction wt = dataBroker.newWriteOnlyTransaction();
                wt.put(dsType, InstanceIdentifier.create(Tenants.class),
                          new TenantsBuilder()
                        .setTenant(tenants).build(), true);
                wt.submit();
            }
            LOG.info("Topology Check Completed");
        } catch (Exception exc) {
            ErrorLog.logError("Error in Topology Stucture Checking\n" + exc.getMessage(), exc.getStackTrace());
        }
    }

    /**
     * Method called when the blueprint container is destroyed.
     */
    @Override
    public void close() {
        if (fpcService != null) {
            fpcService.close();
        }
        if (assignmentService != null) {
            assignmentService.close();
        }
        if (activationService != null) {
            try {
                activationService.close();
            } catch (Exception e) {
            	ErrorLog.logError(e.getStackTrace());
            };
        }
        if (fpcCoreServices != null) {
            fpcCoreServices.close();
        }
        if(wtc != null){
        	wtc.stop();
        }

        if (httpNotifierPool != null) {
            try {
                httpNotifierPool.close();
            } catch (Exception e) {
            	ErrorLog.logError(e.getStackTrace());
            }
        }
        if (zmqNbi != null) {
            try {
                //zmqNbi.close();
            } catch (Exception e) {
            	ErrorLog.logError(e.getStackTrace());
            }
        }
        if (zmqSbListener != null) {
            zmqSbListener.stop();
        }
        if(ZMQSBMessagePool.getInstance() != null) {
            try {
            	ZMQSBMessagePool.getInstance().close();
            } catch (Exception e) {
            	ErrorLog.logError(e.getStackTrace());
            }
        }
        if(config.isUseMemcached()){
	        if(MemcachedThreadPool.getInstance() != null) {
	        	try {
	        		MemcachedThreadPool.getInstance().close();
	            } catch (Exception e) {
	            	ErrorLog.logError(e.getStackTrace());
	            }
	        }
        }
        if (ZMQClientPool.getInstance() != null) {
            try {
                ZMQClientPool.getInstance().close();
            } catch (Exception e) {
            	ErrorLog.logError(e.getStackTrace());
            }
        }

        LOG.info("FpcProvider - Closed");
    }

    /**
     * Retrieves the DataBroker
     * @return DataBorker
     */
    public DataBroker getDataBroker() {
        return this.dataBroker;
    }
}
