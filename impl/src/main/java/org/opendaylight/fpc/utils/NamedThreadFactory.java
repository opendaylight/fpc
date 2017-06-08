/*
 * Copyright © 2016 - 2017 Copyright (c) Sprint, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.fpc.utils;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Named thread factory class.
 */
// TODO - Convert Thread pool to using the NamedThreadFactory for improved logging.
public class NamedThreadFactory implements ThreadFactory {
    private String name = "";
    private final AtomicInteger id;

    /**
     * Constructor.
     * @param name - Base name for generated threads.
     */
    public NamedThreadFactory(String name) {
        this.name = name;
        this.id = new AtomicInteger(1);
    }

    @Override
    public Thread newThread(Runnable r) {
        final Thread thread = new Thread(r);
        thread.setName(name + "-" + id.incrementAndGet());
        return thread;
    }
}
