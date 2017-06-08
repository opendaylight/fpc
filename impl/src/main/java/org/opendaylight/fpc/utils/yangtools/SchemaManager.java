/*
 * Copyright © 2016 - 2017 Copyright (c) Sprint, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.fpc.utils.yangtools;

import org.opendaylight.yangtools.sal.binding.generator.impl.ModuleInfoBackedContext;
import org.opendaylight.yangtools.yang.binding.util.BindingReflections;

/**
 * Simple functions to help in the construction of ModuleInfoBackedContext.  Use get() if a clone is required
 * or getStaticInstance() otherwise.
 *
 */
public class SchemaManager {
    private static final ModuleInfoBackedContext moduleContext = ModuleInfoBackedContext.create();
    static {
        loadDefaultModules(moduleContext);
    }

    /**
     * Loads default modules in the provided context.
     * @param context - context to load default contexts into.
     */
    private static void loadDefaultModules(ModuleInfoBackedContext context) {
        // TODO - Fix Spifly so this is unnecessary and we can add more Yang bindings.
    	context.registerModuleInfo(org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.dmm.fpc.pmip.rev160119.$YangModuleInfoImpl.getInstance());
    	context.registerModuleInfo(org.opendaylight.yang.gen.v1.urn.opendaylight.fpc.config.rev160927.$YangModuleInfoImpl.getInstance());
    	context.registerModuleInfo(org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.fpc.rev150105.$YangModuleInfoImpl.getInstance());
    	context.registerModuleInfo(org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.$YangModuleInfoImpl.getInstance());
    	context.registerModuleInfo(org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.traffic.selector.types.rev160114.$YangModuleInfoImpl.getInstance());
    	context.registerModuleInfo(org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcbase.rev160803.$YangModuleInfoImpl.getInstance());
    	context.registerModuleInfo(org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.pmip.qos.rev160210.$YangModuleInfoImpl.getInstance());
    	context.registerModuleInfo(org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.threegpp.rev160803.$YangModuleInfoImpl.getInstance());
    	context.registerModuleInfo(org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.fpc.rev150105.$YangModuleInfoImpl.getInstance());
        context.registerModuleInfo(org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcpolicyext.rev160803.$YangModuleInfoImpl.getInstance());
    }

    /**
     * Returns Singleton
     * @return ModuleInfoBackedContext singleton for this class or null if not initialized
     */
    public static ModuleInfoBackedContext getStaticInstance() {
        return moduleContext;
    }

    /**
     * Returns a new ModuleInfoBackedContext loaded with default modules
     * @return ModuleInfoBackedContext
     */
    public static ModuleInfoBackedContext get() {
        ModuleInfoBackedContext returnValue = ModuleInfoBackedContext.create();
        loadDefaultModules(returnValue);
        moduleContext.addModuleInfos(BindingReflections.loadModuleInfos());
        return returnValue;
    }
}
