/*
 * Copyright © 2016 Copyright (c) Sprint, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.fpc.activation;

import org.opendaylight.fpc.activation.cache.Cache;
import org.opendaylight.fpc.dpn.DpnHolder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.OpHeader.OpType;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.instructions.Instructions;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.payload.Contexts;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.payload.Ports;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.tenants.tenant.fpc.topology.Dpns;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcbase.rev160803.FpcContext;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcbase.rev160803.targets.value.Targets;

/**
 * Interface used by the South-bound DPN Adaptors in support of the DPN specific use cases.
 *
 * Each Activator instance is given a single DPN to manage.
 */
public interface Activator {
    /**
     * Activates a Context.
     *
     * @param opType - Operation Type
     * @param instructions - Instructions (if they were present).
     * @param context - Context that must be activated
     * @param cache - Cache configured per the RPC operation (@see FPC Op-Reference).
     * @throws Exception - If an error occurs during the Activation
     */
    public void activate(OpType opType, Instructions instructions, Contexts context, Cache cache) throws Exception;

    /**
     * Activates a Context.
     *
     * @param opType - Operation Type
     * @param instructions - Instructions (if they were present).
     * @param port - Port that must be activated
     * @param cache - Cache configured to the specifics of the RPC operation (@see FPC Op-Reference).
     * @throws Exception - If an error occurs during the Activation
     */
    public void activate(OpType opType, Instructions instructions, Ports port, Cache cache) throws Exception;

    /**
     * This method provisions policy elements in storage.
     *
     * They are placed into the cache and then the appropriate operation is applied. This method is typically used
     * to syncrhonize the policy information in the Agent with the pre-provisioned policies in the DPN.  Not all
     * DPNs or DPN types support this.
     *
     * @param opType - Operation Type
     * @param cache - The cache containing policy elements to be activated.
     * @throws Exception - If an error occurs during the Activation
     */
    public void activatePolicyElements(OpType opType, Cache cache) throws Exception;

    /**
     * Removes Targets from the DPN.
     *
     * @param instructions - Instructions (if they were present).
     * @param target - Target to be removed from the DPN
     * @param context - Context that is associated with the Target
     * @throws Exception - If an error occurs during the deletion
     */
    public void delete(Instructions instructions, Targets target, FpcContext context) throws Exception;

    /**
     * Reads A Target from the DPN.
     * @param target - Target to be read
     * @return The Target object reteived from the DPN.  If not present a null is returned.
     * @throws Exception - If an error occurs during the read operation
     */
    public Object read(Targets target) throws Exception;

    /**
     * A metric that tracks the number of Activation Messages received by the Activator.
     * @return - The number of messages the Activator has received.
     */
    public Long rxMessages();

    /**
     * A metric that tracks the number of Activation Messages transmitted by the Activator.
     * @return - The number of messages the Activator has transmitted.
     */
    public Long txMessages();

    /**
     * Applies a DPN configuration to the Activator.
     * @param dpnHolder - DPN to assign to the Activator
     * @return - boolean indicating success or failure of the configuration application
     * @throws Exception - thrown if an error occurs during DPN assignment
     */
    public boolean applyConfiguration(DpnHolder dpnHolder) throws Exception;

    /**
     * Determines if the Activator can work on the current DPN it is assigned to {@link #applyConfiguration(DpnHolder) applyConfiguration(Dpns)}
     * @return - boolean indicating if the Activator can support the configured DPN.
     */
    public boolean canActivate();

    /**
     * Tests the Activator's connection to the current DPN it is assigned to @see {@link #applyConfiguration(DpnHolder) applyConfiguration(Dpns)}
     * @return - boolean indicating if the connection is working
     */
    public boolean testConnection();

    /**
     * Starts the connection between the Activator and DPN.
     * @return  - boolean indicating if the connection was successfully started
     */
    public boolean start();

    /**
     * Shuts down the connection between the Activator and DPN.
     * @return  - boolean indicating if the connection was successfully shut down
     */
    public boolean shutdown();

    /**
     * Indicates that a Hello (handshake) should be initiated to the DPN. This is typically executed after a successful
     * {@link #start() start()}.
     *
     * @param identity - Identity to be used in the Message
     */
    public void announceHello(String identity);

    /**
     * Indicates that a Goodbye (tear down) should be initiated to the DPN. This is typically executed before a
     * {@link #shutdown() shutdown()}.
     *
     * @param identity - Identity to be used in the Message
     */
    public void announceGoodbye(String identity);

    /**
     * Retrieves the Response Manager for the associated DPN.
     *
     * @return - Response Manager of the assigned DPN.
     */
    public ResponseManager getResponseManager();

	/**
	 * Sends the ADC rules to a DPN
	 *
	 * @param dpn - DPN to send the ADC rules to
	 */
	public void send_ADC_rules(Dpns dpn);
}
