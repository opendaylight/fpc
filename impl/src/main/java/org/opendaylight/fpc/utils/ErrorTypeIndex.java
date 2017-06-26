/*
 * Copyright © 2016 - 2017 Copyright (c) Sprint, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.fpc.utils;

/**
 * General Error Types used in the FPC Agent.
 */
public class ErrorTypeIndex {
    static final public Long CREATE_UPDATE_WO_PAYLOAD = 0L;
    static final public Long POLICY_CHAIN_INCOMPLETE = 1L;
    static final public Long POLICY_CHAIN_INCOMPLETE_REFSCOPE = 2L;
    static final public Long CONTEXT_ACTIVATION_FAIL = 3L;
    static final public Long PORT_ACTIVATION_FAIL = 4L;
    static final public Long QUERY_WO_PAYLOAD = 5L;
    static final public Long DELETE_WO_PAYLOAD = 6L;
    static final public Long QUERY_FAILURE= 7L;
    static final public Long DELETE_FAILURE = 8L;
    static final public Long UNKNOWN_OP_TYPE = 9L;
    static final public Long MESSAGE_WITH_NO_BODY = 10L;
    static final public Long CLIENT_ID_NOT_REGISTERED = 11L;
    static final public Long SESSION_ALREADY_EXISTS = 12L;
}
