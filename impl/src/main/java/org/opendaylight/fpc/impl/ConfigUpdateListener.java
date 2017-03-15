/*
 * Copyright © 2016 Copyright (c) Sprint, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.fpc.impl;

import org.opendaylight.yang.gen.v1.urn.opendaylight.fpc.config.rev160927.FpcConfig;

/**
 * Generic Listener for Configuration Updates.
 */
public interface ConfigUpdateListener {
    /**
     * Configuration Update.
     * @param conf - new FPC Configuration
     */
    public void updateConf(FpcConfig conf);
}
