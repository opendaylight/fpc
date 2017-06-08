/*
 * Copyright © 2016 - 2017 Copyright (c) Sprint, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.fpc.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Helper to log error messages and stack traces.
 */
public class ErrorLog {
	private static final Logger LOG = LoggerFactory.getLogger(ErrorLog.class);

	/**
	 * Converts StackTraceEmlement[] to String.
	 * @param ste - array of stack trace elements
	 */
	private static String stackTraceString(StackTraceElement[] ste){
		StringBuilder sb = new StringBuilder();
		for (StackTraceElement elem : ste) {
		        sb.append(elem.toString());
		        sb.append("\n");
		}
		return sb.toString();
	}

	/**
	 * Logs the error stack trace.
	 * @param ste - array of stack trace elements
	 */
	public static void logError(StackTraceElement[] ste){
		LOG.error("My Error: "+stackTraceString(ste));
	}

	/**
	 * Logs the error message and stack trace.
	 * @param msg - string message to be printed to log
	 * @param ste - array of stack trace elements
	 */
	public static void logError(String msg, StackTraceElement[] ste){
		LOG.error(msg+"\n"+stackTraceString(ste));
	}
}