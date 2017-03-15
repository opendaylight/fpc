/*
 * Copyright © 2016 Copyright (c) Sprint, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.fpc.utils;

/**
 * Thread worker.  
 */
// TODO - Set this up so that construction accepts a CountdownLatch
public interface Worker extends Runnable, AutoCloseable {
	/**
	 * Stops the worker.
	 */
    public void stop();
    
    /**
     * Opens resources associated with the worker.
     */
    public void open();
    
    /**
     * Indicates if the Worker's resources are open.
     * @return true if resources are open otherwise false
     */
    public boolean isOpen();
}
