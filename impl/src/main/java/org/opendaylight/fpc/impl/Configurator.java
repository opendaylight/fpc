/*
 * Copyright © 2016 Copyright (c) Sprint, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.fpc.impl;

import java.io.File;
import java.io.InputStream;
import java.util.Collection;
import java.util.Dictionary;

import org.opendaylight.yang.gen.v1.urn.opendaylight.fpc.config.rev160927.FpcConfig;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.BundleListener;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkListener;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;

public class Configurator implements ConfigUpdateListener, BundleContext{

	@Override
	public String getProperty(String key) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Bundle getBundle() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Bundle installBundle(String location, InputStream input) throws BundleException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Bundle installBundle(String location) throws BundleException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Bundle getBundle(long id) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Bundle[] getBundles() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void addServiceListener(ServiceListener listener, String filter) throws InvalidSyntaxException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void addServiceListener(ServiceListener listener) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void removeServiceListener(ServiceListener listener) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void addBundleListener(BundleListener listener) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void removeBundleListener(BundleListener listener) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void addFrameworkListener(FrameworkListener listener) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void removeFrameworkListener(FrameworkListener listener) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public ServiceRegistration<?> registerService(String[] clazzes, Object service, Dictionary<String, ?> properties) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ServiceRegistration<?> registerService(String clazz, Object service, Dictionary<String, ?> properties) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public <S> ServiceRegistration<S> registerService(Class<S> clazz, S service, Dictionary<String, ?> properties) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ServiceReference<?>[] getServiceReferences(String clazz, String filter) throws InvalidSyntaxException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ServiceReference<?>[] getAllServiceReferences(String clazz, String filter) throws InvalidSyntaxException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ServiceReference<?> getServiceReference(String clazz) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public <S> ServiceReference<S> getServiceReference(Class<S> clazz) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public <S> Collection<ServiceReference<S>> getServiceReferences(Class<S> clazz, String filter)
			throws InvalidSyntaxException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public <S> S getService(ServiceReference<S> reference) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean ungetService(ServiceReference<?> reference) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public File getDataFile(String filename) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Filter createFilter(String filter) throws InvalidSyntaxException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Bundle getBundle(String location) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void updateConf(FpcConfig conf) {
		// TODO Auto-generated method stub
		
	}

}
