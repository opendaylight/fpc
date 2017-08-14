/*
 * Copyright © 2016 - 2017 Copyright (c) Sprint, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.fpc.utils;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;

import com.google.common.base.Supplier;
import com.google.common.collect.Iterables;

/**
 * Generic Thread pool class which provides instances of Workers and uses a
 * cycle iterator.  A CountdownLatch is used to signal all Workers simultaneously
 * to begin processing.
 *
 * @param <T> - A subclass of Worker
 *
 * @see com.google.common.collect.Iterables
 * @see java.util.concurrent.CountDownLatch
 * @see org.opendaylight.fpc.utils.Worker
 */
abstract public class AbstractThreadPool<T extends Worker> implements AutoCloseable {
    protected Supplier<? extends T> poolFactory;
    protected int poolSize;
    protected List<T> pool;
    protected Iterator<T> it;
    protected List<Thread> threadPool;
    protected CountDownLatch startSignal;
    protected DataBroker db;
    protected AtomicInteger atomicInt;

    /**
     * Main Constructor.
     * @param db - Data Broker
     * @param poolSize - Thread pool size
     */
    public AbstractThreadPool(DataBroker db, int poolSize) {
        this.poolSize = poolSize;
        this.db = db;
        this.startSignal = new CountDownLatch(1);
        this.pool = new ArrayList<T>();
        this.threadPool = new ArrayList<Thread>();
        this.it = Iterables.cycle(pool).iterator();
        atomicInt = new AtomicInteger(0);
    }

    /**
     * Creates a Threadpool Factory
     * @param db - Data Broker
     * @return A Factory (Supplier of Worker subclass)
     */
    protected abstract Supplier<? extends T> getPoolFactory(DataBroker db);

    /**
     * Starts all threads in this pool
     * @throws Exception - thrown when an error occurs during start up.
     */
    public void start() throws Exception {
        startUp();
    }

    /**
     * Starts the individual threads of the pool.  They SHOULD not process
     * until the count down signal is called via run.
     *
     * @see #run()
     * @throws Exception - thrown when an error occurs during start up.
     */
    protected void startUp() throws Exception {
        this.poolFactory = getPoolFactory(db);
        for (int x = 0; x<poolSize; ++x) {
            T worker = poolFactory.get();
            pool.add(worker);
            worker.open();
            Thread thread = new Thread(worker);
            threadPool.add(thread);
            thread.start();
        }
        this.it = Iterables.cycle(pool).iterator();
    }

    /**
     * Signals all threads to proceed with their work.
     *
     * @throws Exception - thrown when an error occurs during the signal
     */
    public void run() throws Exception {
        startSignal.countDown();
    }

    /**
     * Shuts down the threads in this pool.  This is done by calling stop,
     * pausing 1 second and then calling close on the Worker.
     *
     * @throws Exception - thrown when any error occurs during the stop and close calls
     */
    protected void shutDown() throws Exception {
        for (T worker : pool) {
            worker.stop();
        }

        Thread.sleep(1000);

        for (T worker : pool) {
            worker.close();
        }
    }

    /**
     * Retrieves the next worker.  If the iterator is at the last element in the
     * pool it cycles to the first element.
     *
     * @return T - A subclass of Worker
     */
    public T getWorker() {
    	int cur;
    	while(true){
    		cur = atomicInt.get();
    		if(atomicInt.compareAndSet(cur, cur==this.poolSize-1 ? 0 : cur+1))
    				break;
    	}
    	return pool.get(cur);
        //return it.next();
    }

    /**
     * Retrieves the specified worker.
     * @param index - the index of the worker within the pool
     * @return T - A subclass of Worker. Null, if index out of bound.
     */
    public T getWorker(long index) {
    	return (index >=0 && index < poolSize) ? pool.get((int) index) : null;
    }

    @Override
    public void close() throws Exception {
        shutDown();
    }
}
