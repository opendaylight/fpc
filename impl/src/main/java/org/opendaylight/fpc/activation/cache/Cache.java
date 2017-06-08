/*
 * Copyright © 2016 - 2017 Copyright (c) Sprint, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.fpc.activation.cache;

import java.util.Map;

import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcbase.rev160803.FpcContext;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcbase.rev160803.FpcIdentity;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcbase.rev160803.FpcPort;

/**
 * Cache interface.
 */
public interface Cache {
    /**
     * Mapping of Identity to Context.
     * @return HashMap of Context Identities to their corresponding Objects.
     */
    public Map<FpcIdentity, FpcContext> getContexts();

    /**
     * Mapping of Identity to Ports.
     * @return HashMap of Port Identities to their corresponding Objects.
     */
    public Map<FpcIdentity, FpcPort> getPorts();

    /**
     * Mapping of Identity to Objects.
     * @return HashMap of Identities to their corresponding Objects.
     */
    public Map<FpcIdentity, Object> getUnifiedHashmap();

    /**
     * Retrieves a Context
     * @param key - Context Identity
     * @return FpcContext with the provided key or null if not present
     */
    public FpcContext getContext(FpcIdentity key);

    /**
     * Retrieves a Port
     * @param key - Port Identity
     * @return FpcPort with the provided key or null if not present
     */
    public FpcPort getPort(FpcIdentity key);
}
