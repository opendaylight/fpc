/*
 * Copyright © 2016 Copyright (c) Sprint, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.fpc.notification;

import java.util.concurrent.atomic.AtomicLong;

import org.opendaylight.controller.md.sal.binding.api.NotificationPublishService;

/**
 * Convenience Class for Notifications.
 */
public class NotificationInfo {
    private static NotificationPublishService notificationService = null;
    private static AtomicLong notifcationId = new AtomicLong();

    /**
     * Sets the NotificationPublishService
     * @param notificationService - NotificationPublishService
     */
    static public void  setNotificationPublishService (NotificationPublishService notificationService) {
        NotificationInfo.notificationService = notificationService;
    }

    /**
     * Returns the NotificationPublishService
     * @return NotificationPublishService if set or null otherwise
     */
    static public NotificationPublishService getNotificationPublishService() {
        return notificationService;
    }

    /**
     * Gets the next notification Identifier.
     * @return long representing the next notification identifier
     */
    static public Long next() {
        return notifcationId.getAndIncrement();
    }
}
