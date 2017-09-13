/*
 * Copyright © 2016 - 2017 Copyright (c) Sprint, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.fpc.activation.impl.dpdkdpn;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import org.opendaylight.fpc.activation.Activator;
import org.opendaylight.fpc.activation.ResponseManager;
import org.opendaylight.fpc.activation.cache.Cache;
import org.opendaylight.fpc.dpn.DpnHolder;
import org.opendaylight.fpc.policy.BasePolicyManager;
import org.opendaylight.fpc.tenant.TenantManager;
import org.opendaylight.fpc.utils.ErrorLog;
import org.opendaylight.fpc.utils.IPToDecimal;
import org.opendaylight.fpc.utils.zeromq.ZMQClientPool;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.ClientIdentifier;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.OpHeader.OpType;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.OpIdentifier;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.instructions.Instructions;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.payload.Contexts;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.tenants.tenant.fpc.mobility.Ports;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.tenants.tenant.fpc.topology.Dpns;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcbase.rev160803.FpcAction;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcbase.rev160803.FpcContext;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcbase.rev160803.FpcDescriptor;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcbase.rev160803.FpcDpnId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcbase.rev160803.FpcPolicy;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcbase.rev160803.FpcPolicyGroup;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcbase.rev160803.FpcPolicyGroupId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcbase.rev160803.FpcPolicyId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcbase.rev160803.PmipSelectorDescriptor;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcbase.rev160803.fpc.action.action.value.Drop;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcbase.rev160803.fpc.action.action.value.Rate;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcbase.rev160803.fpc.descriptor.descriptor.value.DomainDescriptor;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcbase.rev160803.fpc.descriptor.descriptor.value.PmipSelector;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcbase.rev160803.fpc.descriptor.descriptor.value.PrefixDescriptor;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcbase.rev160803.fpc.policy.Rules;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcbase.rev160803.fpc.rule.Actions;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcbase.rev160803.fpc.rule.Descriptors;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcbase.rev160803.targets.value.Targets;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.threegpp.rev160803.ThreeGPPTunnel;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.threegpp.rev160803.ThreegppCommandset;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.threegpp.rev160803.ThreegppProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * DPDK DPN Activator implementation.
 */
public class DpdkImpl implements Activator {
	final static Long ZERO = new Long(0L);
	final static short ZERO_SHORT = 0;
	final static AtomicLong rxMessages = new AtomicLong(0L);
	final static AtomicLong txMessages = new AtomicLong(0L);
	protected static final Logger LOG = LoggerFactory.getLogger(DpdkImpl.class);

	DpnHolder dpnHolder;
	Short dpnTopic;
	DpnAPI2 api;

	/**
	 * Default Constructor.
	 */
	public DpdkImpl() {
		this.dpnHolder = null;
		this.api = null;
		this.dpnTopic = null;
	}

	/**
	 * Constructor
	 *
	 * @param dpnHolder
	 *            - DPN assigned to the Activator.
	 */
	public DpdkImpl(DpnHolder dpnHolder) {
		this.dpnHolder = dpnHolder;
		if (dpnHolder.dpn != null) {
			this.start();
			//DpnAPIListener.setUlDpnMapping((short) dpnHolder.dpn.getTopic().getBytes()[0], dpnHolder.dpn.getDpnId());
			DpnAPIListener.setUlDpnMapping(dpnHolder.dpn.getNodeId().toString()+"/"+dpnHolder.dpn.getNetworkId().toString(), dpnHolder.dpn.getDpnId());
		}
	}

	@Override
	public boolean canActivate() {
		if (dpnHolder.dpn == null)
			return false;
		return (DpnAPIListener.getTopicFromDpnId(dpnHolder.dpn.getDpnId()) != null);
	}
	
	/**
	 * Retrieves rules from the port and sends them to the DPN
	 *
	 * @param topic
	 * 			- topic of DPN assigned to Context
	 * @param port
	 * 			- Port assigned to Context
	 */
	public void send_ADC_rules(Short topic, Ports port){
		LOG.info("Obtaining rules from port...");
		for(FpcPolicyGroupId polgroId : port.getPolicyGroups()){
			FpcPolicyGroup polgro = BasePolicyManager.fpcPolicyGroupMap.get(polgroId);
			for(FpcPolicyId policyId : polgro.getPolicies()){
				if(!BasePolicyManager.fpcPolicyMap.containsKey(policyId))
					continue;
				FpcPolicy policy = BasePolicyManager.fpcPolicyMap.get(policyId);
				for(Rules rule : policy.getRules()){
					for(Descriptors descrip: rule.getDescriptors()){
						FpcDescriptor desc = BasePolicyManager.fpcDescriptorMap.get(descrip.getDescriptorId());
						for(Actions act: rule.getActions()){
							FpcAction action = BasePolicyManager.fpcActionMap.get(act.getActionId());
							Short drop = 0;
							String ip = null, domainName = null;
							Rate theAction = ((Rate) action.getActionValue());
						
							if(desc.getDescriptorValue() instanceof PrefixDescriptor)
								ip = new String(((PrefixDescriptor) desc.getDescriptorValue()).getDestinationIp().getValue());
							else if(desc.getDescriptorValue() instanceof DomainDescriptor)
								domainName = ((DomainDescriptor) desc.getDescriptorValue()).getDestinationDomains().get(0).getValue();

							this.api.send_ADC_rules(topic, domainName, ip,  drop, theAction.getRatingGroup(), theAction.getServiceIdentifier(), theAction.getSponsorIdentity().getValue());
							
							// Drop Logic
//							if(action.getActionValue() instanceof Rate){
//								Rate theAction = ((Rate) action.getActionValue());
//								this.api.send_ADC_rules(topic, domainName, ip,  drop, theAction.getRatingGroup(), theAction.getServiceIdentifier(), theAction.getSponsorIdentity().getValue());
//							}else if(action.getActionValue() instanceof Drop){
//								if(((Drop) action.getActionValue()).isDrop()){
//									drop = 1;
//								}
//								//drop action doesn't have rategrp,serviceid,sponsorid
//								this.api.send_ADC_rules(topic, domainName, ip,  drop, null, null, null);
//							}
						}
					}
				}
			}
		}
	}

	@Override
	public boolean applyConfiguration(DpnHolder dpnHolder) throws Exception {
		this.shutdown();
		this.dpnHolder = dpnHolder;
		this.start();
		return true;
	}

	@Override
	public boolean testConnection() {
		return true;
	}

	@Override
	public boolean start() {
		if (this.dpnHolder.dpn != null) {
			api = new DpnAPI2(ZMQClientPool.getInstance().getWorker());
			//this.dpnTopic = dpnHolder.dpn.getTopic().substring(0, 1);
			this.dpnTopic = DpnAPIListener.getTopicFromNode(this.dpnHolder.dpn.getNodeId().toString()+"/"+this.dpnHolder.dpn.getNetworkId().toString());
			return true;
		}
		return false;
	}

	@Override
	public boolean shutdown() {
		if (dpnHolder.dpn != null) {
			DpnAPIListener.removeUlDpnMapping(dpnHolder.dpn.getNodeId().toString()+"/"+dpnHolder.dpn.getNetworkId().toString());
		}
		if (api != null) {
			api = null;
		}
		return true;
	}

	@Override
	public void activate(ClientIdentifier clientIdentifier, OpIdentifier opIdentifier, OpType opType, Instructions instructions, Contexts context, Cache cache) throws Exception {
		// Look for 3GPP Command Instructions or Error out
		if (instructions != null) {
			if (instructions.getInstrType() instanceof ThreegppCommandset) {
				activate(clientIdentifier, opIdentifier, opType, (ThreegppCommandset) instructions.getInstrType(), context, cache);
				return;
			}
		}

		throw new Exception("3GPP Commnnd Instructions MUST be provided for this Activator!");
	}

	/**
	 * Context Activation.
	 *
	 * @param opType
	 *            - Operation Type
	 * @param commands
	 *            - 3GPP Instructions
	 * @param context
	 *            - Context that must be activated
	 * @param cache
	 *            - Cache configured to the specifics of the RPC operation (@see
	 *            FPC Op-Reference).
	 * @throws Exception
	 *             - If an error occurs during the Activation
	 */
	private void activate(ClientIdentifier clientIdentifier, OpIdentifier opIdentifier, OpType opType, ThreegppCommandset commands, Contexts context, Cache cache) throws Exception {
		this.dpnTopic = DpnAPIListener.getTopicFromNode(this.dpnHolder.dpn.getNodeId().toString()+"/"+this.dpnHolder.dpn.getNetworkId().toString());
		rxMessages.incrementAndGet();
		IpPrefix assignedPrefix = (context.getDelegatingIpPrefixes() == null) ? null
				: (context.getDelegatingIpPrefixes().isEmpty()) ? null : context.getDelegatingIpPrefixes().get(0);

		ThreegppProperties threeProps = (ThreegppProperties) context;

		// CREATE
		if (opType == OpType.Create) {
			Long s1u_sgw_gtpu_teid;

			if (commands.getInstr3gppMob().isSession()) {
				if (assignedPrefix == null)
					throw new Exception("Session Create Requested but no IP assignment provided");
				if (context.getUl().getMobilityTunnelParameters().getMobprofileParameters() instanceof ThreeGPPTunnel) {
					s1u_sgw_gtpu_teid = ((ThreeGPPTunnel) context.getUl().getMobilityTunnelParameters()
							.getMobprofileParameters()).getTunnelIdentifier();
				} else
					throw new Exception("Session Create Requested but no UL Tunnel Info provided");
				try{
					api.create_session(dpnTopic, threeProps.getImsi().getValue(),
						IPToDecimal.cidrBase(assignedPrefix.getIpv4Prefix().getValue()), threeProps.getEbi().getValue(),
						context.getUl().getTunnelLocalAddress().getIpv4Address(), s1u_sgw_gtpu_teid,clientIdentifier.getInt64(), opIdentifier.getValue(), context.getContextId().getInt64());
				} catch (Exception e) {
					ErrorLog.logError("Illegal Arguments - Check Configure Input values",e.getStackTrace());
				}
				txMessages.incrementAndGet();
				// TODO - Mod create_session to include TFT for X2 with SGW
				// re-selection

				if (commands.getInstr3gppMob().isDownlink()) {
					// SGW Re-selection
					Long s1u_enb_gtpu_teid;
					if (context.getUl().getMobilityTunnelParameters()
							.getMobprofileParameters() instanceof ThreeGPPTunnel) {
						s1u_enb_gtpu_teid = ((ThreeGPPTunnel) context.getUl().getMobilityTunnelParameters()
								.getMobprofileParameters()).getTunnelIdentifier();
					} else
						throw new Exception(
								"Session Create with implied DL information but no DL Tunnel Info provided");
					api.modify_bearer_dl(dpnTopic, s1u_sgw_gtpu_teid,
							context.getDl().getTunnelRemoteAddress().getIpv4Address(), s1u_enb_gtpu_teid,clientIdentifier.getInt64(), opIdentifier.getValue());
					txMessages.incrementAndGet();
				}
			} else if (commands.getInstr3gppMob().isIndirectForward()) {
				// This is a Create Indirect Forward
				Long dl_gtpu_teid;
				if (context.getDl().getMobilityTunnelParameters().getMobprofileParameters() instanceof ThreeGPPTunnel) {
					dl_gtpu_teid = ((ThreeGPPTunnel) context.getDl().getMobilityTunnelParameters()
							.getMobprofileParameters()).getTunnelIdentifier();
				} else
					throw new Exception("Session Create Requested but no UL Tunnel Info provided");

				// TODO - Modify API for Indirect Forwarding to/from another SGW
				// FFS
			} else if (commands.getInstr3gppMob().isUplink()) {
				// This is a Create Bearer
				if (context.getUl().getMobilityTunnelParameters().getMobprofileParameters() instanceof ThreeGPPTunnel) {
					s1u_sgw_gtpu_teid = ((ThreeGPPTunnel) context.getUl().getMobilityTunnelParameters()
							.getMobprofileParameters()).getTunnelIdentifier();
				} else
					throw new Exception("Uplink Create Requested but no UL Tunnel Info provided");
				// LOG.info("Sending Message");
				api.create_bearer_ul(dpnTopic, threeProps.getImsi().getValue(),
						((threeProps.getLbi() == null) ? 0 : threeProps.getLbi().getValue()),
						threeProps.getEbi().getValue(), context.getUl().getTunnelLocalAddress().getIpv4Address(),
						s1u_sgw_gtpu_teid, null);
				txMessages.incrementAndGet();
			}
		} else if (opType == OpType.Update) {
			// Update
			Long s1u_enb_gtpu_teid = ZERO, s1u_sgw_gtpu_teid = ZERO, s1u_enb_gtpu_lifeTime = ZERO;

			if (commands.getInstr3gppMob().isDownlink()) {
				if (context.getDl().getMobilityTunnelParameters().getMobprofileParameters() instanceof ThreeGPPTunnel) {
					s1u_enb_gtpu_teid = ((ThreeGPPTunnel) context.getDl().getMobilityTunnelParameters()
							.getMobprofileParameters()).getTunnelIdentifier();
				} else
					throw new Exception("Modify Bearer but no 3GPP DL Tunnel Info provided");
				boolean isDLdelete = (context.getDl().getLifetime() == null) ? false : true;

				if (!isDLdelete) {
					s1u_sgw_gtpu_teid = ((ThreeGPPTunnel) context.getUl().getMobilityTunnelParameters()
							.getMobprofileParameters()).getTunnelIdentifier();
					try {
						api.modify_bearer_dl(dpnTopic, context.getDl().getTunnelRemoteAddress().getIpv4Address(),
							s1u_enb_gtpu_teid, context.getDl().getTunnelLocalAddress().getIpv4Address(), null, clientIdentifier.getInt64(), opIdentifier.getValue(), context.getContextId().getInt64());
					} catch (Exception e) {
						ErrorLog.logError(e.getMessage(),e.getStackTrace());
					}
				} else {
					s1u_enb_gtpu_lifeTime = context.getDl().getLifetime();
					if (s1u_enb_gtpu_lifeTime == 0L)
						api.delete_bearer(dpnTopic, s1u_enb_gtpu_teid);
					else
						api.delete_bearer(api, dpnTopic, s1u_enb_gtpu_teid, s1u_enb_gtpu_lifeTime);
				}
				txMessages.incrementAndGet();
			}
			if (commands.getInstr3gppMob().isUplink()) {
				if (context.getUl().getMobilityTunnelParameters().getMobprofileParameters() instanceof ThreeGPPTunnel) {
					s1u_sgw_gtpu_teid = ((ThreeGPPTunnel) context.getUl().getMobilityTunnelParameters()
							.getMobprofileParameters()).getTunnelIdentifier();
				} else
					throw new Exception("Modify Bearer but no 3GPP UL Tunnel Info provided");
				boolean isULdelete = (context.getUl().getLifetime() == null) ? false : true;
				if (!isULdelete) {
					s1u_enb_gtpu_teid = ((ThreeGPPTunnel) context.getDl().getMobilityTunnelParameters()
							.getMobprofileParameters()).getTunnelIdentifier();
					api.modify_bearer_ul(dpnTopic, context.getUl().getTunnelLocalAddress().getIpv4Address(),
							s1u_enb_gtpu_teid, s1u_sgw_gtpu_teid, null);
				} else {
					s1u_enb_gtpu_lifeTime = context.getUl().getLifetime();
					if (s1u_enb_gtpu_lifeTime == 0L)
						api.delete_bearer(dpnTopic, s1u_sgw_gtpu_teid);
					else
						api.delete_bearer(api, dpnTopic, s1u_sgw_gtpu_teid, s1u_enb_gtpu_lifeTime);
				}
				txMessages.incrementAndGet();
			}
		} else if (opType == OpType.Delete) {
			// Delete
			Long s1u_sgw_gtpu_teid;

			if (context.getUl().getMobilityTunnelParameters().getMobprofileParameters() instanceof ThreeGPPTunnel) {
				s1u_sgw_gtpu_teid = ((ThreeGPPTunnel) context.getUl().getMobilityTunnelParameters()
						.getMobprofileParameters()).getTunnelIdentifier();
			} else
				throw new Exception("Uplink Delete Requested but no UL Tunnel Info provided");

			// TODO - Improve this as it is weak
			if (!commands.getInstr3gppMob().isSession()) {
				// LOG.info("Sending Message");
				api.delete_bearer(dpnTopic, s1u_sgw_gtpu_teid);
				txMessages.incrementAndGet();
			} else {
				// Bearer Delete seems odd - how does DL get deleted
				// LOG.info("Sending Message");
				try {
					api.delete_session(dpnTopic, threeProps.getLbi().getValue(), s1u_sgw_gtpu_teid, clientIdentifier.getInt64(), opIdentifier.getValue(), context.getContextId().getInt64());
				} catch (Exception e) {
					ErrorLog.logError("Illegal Arguments - Check Configure Input values",e.getStackTrace());
				}
				txMessages.incrementAndGet();
			}
		}
	}

	@Override
	public void activate(OpType opType, Instructions instructions, Ports context, Cache cache) throws Exception {
		rxMessages.incrementAndGet();
	}

	@Override
	public void activatePolicyElements(OpType opType, Cache cache) throws Exception {
		rxMessages.incrementAndGet();
	}

	@Override
	public void delete(ClientIdentifier clientIdentifier, OpIdentifier opIdentifier, Instructions instructions, Targets target, FpcContext context) throws Exception {
		this.dpnTopic = DpnAPIListener.getTopicFromNode(this.dpnHolder.dpn.getNodeId().toString()+"/"+this.dpnHolder.dpn.getNetworkId().toString());
		rxMessages.incrementAndGet();
		Long teid = (context.getUl().getMobilityTunnelParameters().getMobprofileParameters() instanceof ThreeGPPTunnel)
				? ((ThreeGPPTunnel) context.getUl().getMobilityTunnelParameters().getMobprofileParameters())
						.getTunnelIdentifier()
				: null;

		if (teid == null) {
			throw new Exception("Downlink Create Requested but no UL Tunnel Info provided");
		}

		if (target!=null && (target.getTarget().toString().endsWith("ul") || target.getTarget().toString().endsWith("dl"))) {
			api.delete_bearer(dpnTopic, teid);
			txMessages.incrementAndGet();
		} else {
			if (context.getLbi() != null) {
				try{
					api.delete_session(dpnTopic, context.getLbi().getValue(), teid, clientIdentifier.getInt64(), opIdentifier.getValue(), context.getContextId().getInt64());
				} catch (Exception e) {
					ErrorLog.logError("Illegal Arguments - Check Configure Input values",e.getStackTrace());
				}
				txMessages.incrementAndGet();
			}
		}
	}

	@Override
	public Object read(Targets target) throws Exception {
		rxMessages.incrementAndGet();
		throw new UnsupportedOperationException(
				"Use a generic Config Store Reader for this.  This protocol does not support reads.");
	}

	@Override
	public ResponseManager getResponseManager() {
		return new DpdkReponseManager();
	}

	@Override
	public void announceHello(String identity) {
		api.hello((short)DpnAPI2.BROADCAST_TOPIC, identity);
		txMessages.incrementAndGet();
	}

	@Override
	public void announceGoodbye(String identity) {
		api.bye((short)DpnAPI2.BROADCAST_TOPIC, identity);
		txMessages.incrementAndGet();
	}

	@Override
	public Long rxMessages() {
		return rxMessages.get();
	}

    @Override
    public Long txMessages() {
        return txMessages.get();
    }
}
