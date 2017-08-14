/*
 * Copyright © 2016 Copyright (c) Sprint, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.fpc.policy;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.HashMap;
import java.util.HashSet;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.fpc.tenant.TenantManager;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.tenants.tenant.fpc.mobility.Ports;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.tenants.tenant.fpc.policy.Actions;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.tenants.tenant.fpc.policy.Descriptors;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.tenants.tenant.fpc.policy.Policies;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.tenants.tenant.fpc.policy.PoliciesKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.tenants.tenant.fpc.policy.PolicyGroups;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcbase.rev160803.FpcAction;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcbase.rev160803.FpcActionId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcbase.rev160803.FpcActionIdType;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcbase.rev160803.FpcDescriptor;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcbase.rev160803.FpcDescriptorId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcbase.rev160803.FpcIdentity;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcbase.rev160803.FpcPolicy;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcbase.rev160803.FpcPolicyGroup;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcbase.rev160803.FpcPolicyGroupId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcbase.rev160803.FpcPolicyId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcbase.rev160803.fpc.policy.Rules;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BasePolicyManager extends PolicyManager implements AutoCloseable {
	private static final Logger LOG = LoggerFactory.getLogger(BasePolicyManager.class);

	private final TenantManager tenantMgr;
    private boolean initialized;
    private final Map<FpcIdentity, Set<FpcIdentity>> revpointer;
    private final Set<FpcIdentity> portNames;
    public static Map<FpcPolicyGroupId, FpcPolicyGroup> fpcPolicyGroupMap = new ConcurrentHashMap<>();
    public static Map<FpcPolicyId, FpcPolicy> fpcPolicyMap = new ConcurrentHashMap<>();
    public static Map<FpcIdentity,FpcDescriptor> fpcDescriptorMap = new ConcurrentHashMap<>();
    public static Map<FpcActionIdType,FpcAction> fpcActionMap = new ConcurrentHashMap<>();

    /**
     * Primary Constructor.
     * @param tenantMgr - Tenant Manager Assigned to the Activation Manager.
     * @param dataBroker - Data Broker
     */
    public BasePolicyManager(TenantManager tenantMgr,
            DataBroker dataBroker) {
        super();
        initialized = false;
        this.tenantMgr = tenantMgr;
        revpointer = new HashMap<FpcIdentity,Set<FpcIdentity>>();
        portNames = new HashSet<FpcIdentity>();
        init(dataBroker, tenantMgr.getTenant());
        registerListeners();
    }

    /**
     * Retrieves the mappings of FpcIdentities to {link @org.opendaylight.fpc.policy.BasePolicyManager.getRevPointer()}
     *
     * @return A Map of FpcIdentity instances mapped to the Set of Pointers to FpcIdentity.
     */
    public Map<FpcIdentity, Set<FpcIdentity>> getRevPointer() {
        return revpointer;
    }



	@Override
	public void addDescriptor(Descriptors desc) throws Exception {
       LOG.info("Descriptor Manager - Adding Descriptor " + desc.getDescriptorId());
       fpcDescriptorMap.put(desc.getDescriptorId(), desc);
       if (!revpointer.containsKey(desc.getDescriptorId()))
    	   revpointer.put(desc.getDescriptorId(), null);
	}

	@Override
	public void removeDescriptor(Descriptors desc) {
		if (desc != null) {
			LOG.info("Descriptor Manager - Removing Descriptor " + desc.getDescriptorId());
			revpointer.remove(desc.getDescriptorId());
			fpcDescriptorMap.remove(desc.getDescriptorId());
		}
	}

	@Override
	public void addAction(Actions act) throws Exception {
		LOG.info("Action Manager - Adding Action " + act.getActionId());
		fpcActionMap.put(act.getActionId(), act);
		if (!revpointer.containsKey(act.getActionId()))
	    	   revpointer.put(act.getActionId(), null);
	}

	@Override
	public void removeAction(Actions act) {
		if (act != null) {
			LOG.info("Action Manager - Removing Action " + act.getActionId());
			revpointer.remove(act.getActionId());
			fpcActionMap.remove(act.getActionId());
		}
	}

	@Override
	public void addPolicy(Policies policy) throws Exception {
		LOG.info("Policy Manager - Adding Policy " + policy.getPolicyId());
		fpcPolicyMap.put(policy.getPolicyId(), policy);
		for (Rules rule : policy.getRules()){
			for (org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcbase.rev160803.fpc.rule.Descriptors desc : rule.getDescriptors()) {
				Set<FpcIdentity> s = revpointer.containsKey(desc.getDescriptorId()) ? revpointer.get(desc.getDescriptorId()) :
					new HashSet<FpcIdentity>();
				s.add(policy.getPolicyId());
				revpointer.put(desc.getDescriptorId(), s);
			}
			for (org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcbase.rev160803.fpc.rule.Actions act : rule.getActions()) {
				Set<FpcIdentity> s = revpointer.containsKey(act.getActionId()) ? revpointer.get(act.getActionId()) :
					new HashSet<FpcIdentity>();
				s.add(policy.getPolicyId());
				revpointer.put(act.getActionId(), s);
			}
		}
	}

	@Override
	public void removePolicy(Policies policy) {
		if (policy != null) {
			LOG.info("Policy Manager - Removing Policy " + policy.getPolicyId());
			revpointer.remove(policy.getPolicyId());
			fpcPolicyMap.remove(policy.getPolicyId());
		}
	}

	@Override
	public void addPolicyGroups(PolicyGroups polgro) throws Exception {
		LOG.info("Policy Group Manager - Adding Policy Group " + polgro.getPolicyGroupId());
		fpcPolicyGroupMap.put(polgro.getPolicyGroupId(), polgro);
		for (FpcPolicyId policy : polgro.getPolicies()){
			for (Rules rule : ((FpcPolicy) policy).getRules()){
				for (org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcbase.rev160803.fpc.rule.Descriptors desc : rule.getDescriptors()) {
					Set<FpcIdentity> s = revpointer.containsKey(desc.getDescriptorId()) ? revpointer.get(desc.getDescriptorId()) :
						new HashSet<FpcIdentity>();
					s.add(((FpcPolicy) policy).getPolicyId());
					revpointer.put(desc.getDescriptorId(), s);
				}
				for (org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcbase.rev160803.fpc.rule.Actions act : rule.getActions()) {
					Set<FpcIdentity> s = revpointer.containsKey(act.getActionId()) ? revpointer.get(act.getActionId()) :
						new HashSet<FpcIdentity>();
					s.add(((FpcPolicy) policy).getPolicyId());
					revpointer.put(act.getActionId(), s);
				}
			}
		}
	}

	@Override
	public void removePolicyGroups(PolicyGroups polgro) {
		if (polgro != null) {
			LOG.info("Policy Manager - Removing Policy " + polgro.getPolicyGroupId());
			revpointer.remove(polgro.getPolicyGroupId());
			fpcPolicyGroupMap.remove(polgro.getPolicyGroupId());
		}
	}

	/**Recursive method called reevaluate
	 * goes through each key
	 * grab the mappings
	 * Print out to log
	 * chase the mappings
	 * Goal: Break the system
	 * Updated upstream
	 * @param key - FpcIdentity to search for
	 */

	public void reevaluate(FpcIdentity key){
		//Check if Key exists
		if (revpointer.containsKey(key)){
			if (revpointer.get(key) == null) {

			} else {
				for (FpcIdentity id : revpointer.get(key)){
					reevaluate(id);
				}
			}
		//If key does not exist
		} else {
			if (portNames.contains(key))
				evaluate(key);
		}

	}
	public void evaluate(FpcIdentity key){
		//Tie into Dpn
	}

	@Override
	public void addPorts(Ports port) throws Exception {
		for (FpcPolicyGroupId polgroup : port.getPolicyGroups()) {
			Set<FpcIdentity> s = revpointer.containsKey(polgroup) ? revpointer.get(polgroup) :
				new HashSet<FpcIdentity>();
			s.add(port.getPortId());
			revpointer.put(polgroup, s);
		}
		portNames.add(port.getPortId());
	}

	@Override
	public void removePorts(Ports port) {
		// TODO Auto-generated method stub

	}
}
