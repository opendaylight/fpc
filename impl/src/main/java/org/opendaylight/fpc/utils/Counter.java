/*
 * Copyright © 2016 - 2017 Copyright (c) Sprint, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.fpc.utils;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.TreeMap;

/**
 * A Counter used to track allocated integers in a range of values. 
 *
 * Example:
 *
 * NextValue:
 *
 * Counter c = new Counter();
 * c.nextValue();
 *
 * Recycle:
 * c.recyle(i);
 *
 */
// TODO - Determine if we can eliminate the background thread in the PageManager during a recycle as it appears unnecessary.
// TODO - Re-issue partially used pages.  Right now we throw them away.
public class Counter {
    private final static int MAX_SIZE_EXPONENT = 32;
    private final static int PAGE_SIZE_EXPONENT= 2;
    private final static int PAGE_SIZE = (int) Math.pow(2, PAGE_SIZE_EXPONENT);

    private PageManager w;
    private Page currentPage, nextPage;
    private int offset = 0;

    /**
     * Constructor for a given range.
     * @param lo - minimum value (inclusive)
     * @param hi - maximum value (inclusive)
     */
    public Counter(int lo, int hi) {
        offset = lo;
        w = new PageManager(lo,hi);
        currentPage = w.nextPage();
        nextPage = w.nextPage();
    }

    /**
     * Default Constructor for the full range of integer values.
     */
    public Counter() {
        w = new PageManager();
        currentPage = w.nextPage();
        nextPage = w.nextPage();
    }

    /**
     * Returns the next unused value.
     * @return integer
     * @throws OutOfRangeException when no unused value exists
     */
    public int nextValue() throws OutOfRangeException {
        int nv;

        try {
            nv = currentPage.nextValue();
        } catch (OutOfRangeException e) {
            // Running of current page
            currentPage = nextPage;
            nextPage = w.nextPage();

            nv = currentPage.nextValue();
        }

        return nv + offset;
    }

    /**
     * Returns a value to the pool
     * @param i - integer value to be recycled.
     */
    public void recycle(int i) {
        w.recycle(i);
    }

    /**
     * Manages the pages (sub-ranges) of the pool
     */
    private class PageManager {
        private static final int TIMER_INTERVAL = 60000;
        private static final int TIMER_START_TIME = 1000;
        private int page_index_range = (int) Math.pow(2.0, MAX_SIZE_EXPONENT - PAGE_SIZE_EXPONENT);
        private BitSet unusedPageIndex = new BitSet(page_index_range);
        private TreeMap<Integer, BitSet> recylePages = new TreeMap<>();

        /**
         * Range based Constructor
         * @param lo - minimum value (inclusive)
         * @param hi - maximum value (inclusive)
         */
        public PageManager(int lo, int hi) {
            page_index_range = (int) ((hi - lo) / Math.pow(2.0, PAGE_SIZE_EXPONENT));
            unusedPageIndex = new BitSet(page_index_range);
            start();
        }

        /**
         * Default Constructor.
         */
        public PageManager() {
            page_index_range = (int) Math.pow(2.0, MAX_SIZE_EXPONENT - PAGE_SIZE_EXPONENT);
            unusedPageIndex = new BitSet(page_index_range);
            start();
        }

        /**
         * Starts the internal thread used to clean up pages.
         */
        private void start() {
            Timer t = new Timer();
            t.scheduleAtFixedRate( new TimerTask() {

                @Override
                public void run() {
                    cleanup();
                }
            },
                    TIMER_START_TIME, TIMER_INTERVAL);
        }

        /**
         * Returns the next available page.
         * @return next page with some unallocated values
         * @throws OutOfRangeException when no pages remain
         */
        public Page nextPage() throws OutOfRangeException {
            int index = unusedPageIndex.nextClearBit(0);
            if (index > page_index_range) 
            	throw new OutOfRangeException();
            unusedPageIndex.set(index);
            return new Page(index);
        }

        /**
         * Recycles an integer
         * @param num - integer to be recycled in the pool
         */
        public void recycle(int num) {
          Integer pageNumber = (num -1) / PAGE_SIZE;

          BitSet usedPage = recylePages.get(pageNumber);

          if (usedPage == null) {
              usedPage = new BitSet(PAGE_SIZE);
              recylePages.put(pageNumber, usedPage);

          }
          usedPage.set(num-1);
        }

        /**
         * Cleanup process that looks for completely empty pages.
         */
        public synchronized void cleanup() {
            ArrayList<Integer> removeList = new ArrayList<>();
                for (Map.Entry<Integer, BitSet> entry: recylePages.entrySet()) {
                    int i = entry.getKey();
                    BitSet value = entry.getValue();
                    if (value.cardinality() == PAGE_SIZE) {
                        unusedPageIndex.clear(i);
                        removeList.add(i);
                    }

            }
            for (Integer i : removeList) {
                recylePages.remove(i);
            }
        }
    }

    /**
     * Tracking class for used values.
     */
    private class Page {
        int currentValue;
        int maxValue;

        /**
         * Primary Constructor.
         * @param index - Page index (used by the PageManager for tracking)
         */
        public Page(int index) {
            if (index == 0) {
                this.maxValue = PAGE_SIZE;
                this.currentValue = 0;
            } else {
                this.maxValue = (int)Math.pow(2,PAGE_SIZE_EXPONENT+index);
                this.currentValue = (int) Math.pow(2,PAGE_SIZE_EXPONENT+index-1);
            }
        }

        /**
         * Returns the next unused value.
         * @return integer
         * @throws OutOfRangeException when no unused value exists
         */ 
        public int nextValue() throws OutOfRangeException {
            this.currentValue++;

            if (this.currentValue > this.maxValue) {
                throw new OutOfRangeException();
            }
            return this.currentValue;
        }
    }

    /**
     * Explicit error noting the exhaust of a page or pool
     */
    public class OutOfRangeException extends RuntimeException {
        /**
         * Generated Serial Id.
         */
        private static final long serialVersionUID = -1537850018712931278L;
    }
}
