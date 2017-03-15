/*
 * Copyright © 2016 Copyright (c) Sprint, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.fpc.activation.cache.transaction;

/**
 * Empty Input Body Exception
 */
public class EmptyBodyException extends Exception {
    private static final long serialVersionUID = 1L;

    /**
     * Default Constructor.
     */
    public EmptyBodyException() {
        super();
    }

    /**
     * Constructor.
     * @param message - String message
     */
    EmptyBodyException(String message) {
        super(message);
    }
}
