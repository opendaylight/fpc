/*
 * Copyright © 2016 Copyright (c) Sprint, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.fpc.activation.workers;

import java.util.Collections;
import java.util.concurrent.BlockingQueue;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.fpc.monitor.ChangeMonitor;
import org.opendaylight.fpc.monitor.EventMonitorMgr;
import org.opendaylight.fpc.monitor.ScheduledMonitors;
import org.opendaylight.fpc.utils.ErrorLog;
import org.opendaylight.fpc.utils.Worker;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.EventDeregisterInput;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.EventRegisterInput;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.ProbeInput;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.event_deregister.input.Monitors;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcbase.rev160803.FpcIdentity;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcbase.rev160803.report.config.EventConfigValue;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcbase.rev160803.report.config.event.config.value.EventsConfig;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcbase.rev160803.report.config.event.config.value.EventsConfigIdent;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcbase.rev160803.report.config.event.config.value.PeriodicConfig;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcbase.rev160803.report.config.event.config.value.ScheduledConfig;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcbase.rev160803.report.config.event.config.value.ThresholdConfig;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcbase.rev160803.targets.value.Targets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Primary worker for MONITOR requests (REG, DEREG and PROBE).
 */
public class MonitorWorker
    implements Worker {
    private static final Logger LOG = LoggerFactory.getLogger(MonitorWorker.class);

    private boolean run;
    private final BlockingQueue<Object> blockingConfigureQueue;
    private final DataBroker db;

    /**
     * Constructor.
     * @param db - DataBroker
     * @param blockingConfigureQueue - Work queue
     */
    public MonitorWorker(DataBroker db, BlockingQueue<Object> blockingConfigureQueue) {
        this.db = db;;
        this.blockingConfigureQueue = blockingConfigureQueue;
        LOG.info("ConfigureWorker has been initialized");
    }

    /**
     * Retrieves the work queue.
     * @return - the work queue for this instance
     */
    public BlockingQueue<Object> getQueue() {
        return blockingConfigureQueue;
    }

    @Override
    public void stop() {
        this.run = false;
    }

    @Override
    public void close() throws Exception {
    }

    @Override
    public void open() {
        // Does nothing
    }

    @Override
    public void run() {
        this.run = true;
        LOG.info("ActivationWorker RUN started");
        try {
            while(run) {
                Object obj = blockingConfigureQueue.take();
                if (obj instanceof EventRegisterInput) {
                    EventRegisterInput regInput = (EventRegisterInput) obj;
                    EventConfigValue reg = regInput.getEventConfigValue();
                    if (reg instanceof ScheduledConfig) {
                        ScheduledMonitors.register(regInput.getMonitorId(), regInput.getClientId(),
                                regInput.getTarget(), (ScheduledConfig) reg);
                    } else if (reg instanceof PeriodicConfig) {
                        ScheduledMonitors.register(regInput.getMonitorId(), regInput.getClientId(),
                                regInput.getTarget(), (PeriodicConfig) reg);
                    } else if (reg instanceof EventsConfigIdent) {
                        EventMonitorMgr.register(regInput.getMonitorId(), regInput.getClientId(),
                                (EventsConfigIdent) reg);
                    } else if (reg instanceof EventsConfig) {
                        EventMonitorMgr.register(regInput.getMonitorId(), regInput.getClientId(), (EventsConfig) reg);
                    } else if (reg instanceof ThresholdConfig) {
                        ChangeMonitor.register(regInput.getMonitorId(), regInput.getClientId(),
                                regInput.getTarget(), (ThresholdConfig) reg);
                    } else {
                        LOG.info("Unknown Monitor Type sent to Worker - discarding montort type = {}",
                                reg.getImplementedInterface().getName());
                    }
                } else if (obj instanceof EventDeregisterInput) {
                    EventDeregisterInput deregInput = (EventDeregisterInput) obj;
                    for (Monitors monitor : (deregInput.getMonitors() != null) ? deregInput.getMonitors() :
                            Collections.<Monitors>emptyList()) {
                        FpcIdentity id = monitor.getMonitorId();
                        if (ScheduledMonitors.hasMonitorId(id)) {
                            ScheduledMonitors.deregister(id);
                        } else if (EventMonitorMgr.hasMonitorId(id)) {
                            EventMonitorMgr.deregister(id);
                        } else if (ChangeMonitor.hasMonitorId(id)) {
                            ChangeMonitor.deregister(id);
                        }
                    }
                } else if (obj instanceof ProbeInput) {
                    ProbeInput prInput = (ProbeInput) obj;
                    for (Targets monitor : (prInput.getTargets() != null) ? prInput.getTargets() :
                        Collections.<Targets>emptyList()) {
                        FpcIdentity id = monitor.getTarget();
                        if (ScheduledMonitors.hasMonitorId(id)) {
                            ScheduledMonitors.probe(id);
                        } else if (EventMonitorMgr.hasMonitorId(id)) {
                            LOG.info("A probe request was made for an event.  This is not possible");
                        } else if (ChangeMonitor.hasMonitorId(id)) {
                            ChangeMonitor.probe(id);
                        }
                    }
                }
            }
        } catch (InterruptedException e) {
        	ErrorLog.logError(e.getStackTrace());
        }
    }

	@Override
	public boolean isOpen() {
		return true;
	}
}
