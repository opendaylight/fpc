/*
 * Copyright © 2016 - 2017 Copyright (c) Sprint, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.fpc.activation.cache.transaction;

import org.opendaylight.fpc.activation.cache.PayloadCache;
import org.opendaylight.fpc.tenant.TenantManager;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.OpInput;

public class ContextInfoHolder {
	public TenantManager tenantManager;
	public PayloadCache payloadCache;
	public OpInput opInput;

	public ContextInfoHolder(TenantManager tenantManager,PayloadCache payloadCache, OpInput opInput){
		this.tenantManager = tenantManager;
		this.payloadCache = payloadCache;
		this.opInput = opInput;
	}
}
