/*
 * Copyright © 2016 - 2017 Copyright (c) Sprint, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.fpc.utils;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.opendaylight.fpc.utils.yangtools.SchemaManager;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.IetfDmmFpcagentData;
import org.opendaylight.yangtools.binding.data.codec.gen.impl.StreamWriterGenerator;
import org.opendaylight.yangtools.binding.data.codec.impl.BindingNormalizedNodeCodecRegistry;
import org.opendaylight.yangtools.sal.binding.generator.impl.ModuleInfoBackedContext;
import org.opendaylight.yangtools.sal.binding.generator.util.BindingRuntimeContext;
import org.opendaylight.yangtools.sal.binding.generator.util.JavassistUtils;
import org.opendaylight.yangtools.yang.binding.BindingStreamEventWriter;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.binding.Notification;
import org.opendaylight.yangtools.yang.binding.YangModuleInfo;
import org.opendaylight.yangtools.yang.binding.util.BindingReflections;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.stream.NormalizedNodeStreamWriter;
import org.opendaylight.yangtools.yang.data.api.schema.stream.NormalizedNodeWriter;
import org.opendaylight.yangtools.yang.data.codec.gson.JSONCodecFactory;
import org.opendaylight.yangtools.yang.data.codec.gson.JSONNormalizedNodeStreamWriter;
import org.opendaylight.yangtools.yang.data.codec.gson.JsonParserStream;
import org.opendaylight.yangtools.yang.data.codec.gson.JsonWriterFactory;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNormalizedNodeStreamWriter;
import org.opendaylight.yangtools.yang.data.impl.schema.NormalizedNodeResult;
import org.opendaylight.yangtools.yang.model.api.DataNodeContainer;
import org.opendaylight.yangtools.yang.model.api.DataSchemaNode;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang.model.api.SchemaPath;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Function;
import com.google.common.base.Throwables;
import com.google.common.collect.FluentIterable;
import com.google.gson.stream.JsonReader;
//import org.apache.aries.spifly.client.jar.Consumer;

import javassist.ClassPool;

/**
 * This class is an adaptation of TTPUtils (org.opendaylight.ttp.utils)
 * for this specific set of modules.
 */
public class FpcCodecUtils {
	/**
	 * Retrieves Global instances of Specific Service in the Bundle.  This is
	 * adapted from org.opendaylight.controller.sal.utils.ServiceHelper
	 *
	 * @param clazz - Class associated with the Class loader.
	 * @param bundle - Bundle
	 * @return an array of objects that implement the services specified by the clazz
	 */
    public static Object[] getGlobalInstances(Class<?> clazz, Object bundle) {
        Object instances[] = null;
        try {
            BundleContext bCtx = FrameworkUtil.getBundle(bundle.getClass())
                    .getBundleContext();

            ServiceReference[] services = bCtx.getServiceReferences(clazz
                    .getName(), null);

            if (services != null) {
                instances = new Object[services.length];
                for (int i = 0; i < services.length; i++) {
                    instances[i] = bCtx.getService(services[i]);
                }
            }
        } catch (Exception e) {
            LOG.error("Instance reference is NULL");
        }
        return instances;
    }

    private static final Logger LOG = LoggerFactory.getLogger(FpcCodecUtils.class);

    private static final QName TOP_ODL_FPC_QNAME =
            QName.create("urn:ietf:params:xml:ns:yang:fpcagent", "2016-08-03", "fpcagent");
    private static final YangInstanceIdentifier TOP_ODL_FPC_PATH =
            YangInstanceIdentifier.of(TOP_ODL_FPC_QNAME);
    private SchemaContext context;
    private BindingRuntimeContext bindingContext;
    private BindingNormalizedNodeCodecRegistry codecRegistry;
    private final YangInstanceIdentifier TOP_PATH;

    /**
     * Primary Constructor
     * @param moduleInfos - Yang Module Information
     * @param topPath - Top Path for the Utility to use
     */
    public FpcCodecUtils(Iterable<? extends YangModuleInfo> moduleInfos,
            YangInstanceIdentifier topPath) {
        LOG.info("Building context");
        TOP_PATH = topPath;
        final ModuleInfoBackedContext moduleContext = SchemaManager.get();

        moduleContext.addModuleInfos(moduleInfos);
        context =  moduleContext.tryToCreateSchemaContext().get();
        if (context == null) {
            LOG.info("Context could not be built");
        } else {
            LOG.info("Context built");
        }

        LOG.info("Building Binding Context");
        bindingContext = BindingRuntimeContext.create(moduleContext, context);

        LOG.info("Building Binding Codec Factory");
        final BindingNormalizedNodeCodecRegistry bindingStreamCodecs = new BindingNormalizedNodeCodecRegistry(StreamWriterGenerator.create(JavassistUtils.forClassPool(ClassPool.getDefault())));
        bindingStreamCodecs.onBindingRuntimeContextUpdated(bindingContext);
        codecRegistry = bindingStreamCodecs;
        LOG.info("Mapping service built");
    }

    /**
     * Returns the Normalized Codec Registry
     * @return BindingNormalizedNodeCodecRegistry
     */
    public final BindingNormalizedNodeCodecRegistry getCodecRegistry() {
        return codecRegistry;
    }

    /**
     * Returns the Schema Context
     * @return SchemaContext
     */
    public final SchemaContext getSchemaContext() {
        return context;
    }

    /**
     * Converts a {@link DataObject} to a JSON representation in a string using the relevant YANG
     * schema if it is present. This defaults to using a {@link org.opendaylight.yangtools.yang.model.api.SchemaContextListener} if running an
     * OSGi environment or {@link BindingReflections#loadModuleInfos()} if run while not in an OSGi
     * environment or if the schema isn't available via {@link org.opendaylight.yangtools.yang.model.api.SchemaContextListener}.
     *
     * @param path - InstanceIdentifier
     * @param object - DataObject
     * @return JSON String representing the input
     */
    public final String jsonStringFromDataObject(InstanceIdentifier<?> path, DataObject object) {
        return jsonStringFromDataObject(path, object, false);
    }

   /**
     * Converts a {@link DataObject} to a JSON representation in a string using the relevant YANG
     * schema if it is present. This defaults to using a {@link org.opendaylight.yangtools.yang.model.api.SchemaContextListener} if running an
     * OSGi environment or {@link BindingReflections#loadModuleInfos()} if run while not in an OSGi
     * environment or if the schema isn't available via {@link org.opendaylight.yangtools.yang.model.api.SchemaContextListener}.
     *
     * @param path - InstanceIdentifier
     * @param object - DataObject
     * @param pretty - indicates if pretty printing (whitespace) should be used
     * @return JSON String representing the input
     */
    public final String jsonStringFromDataObject(InstanceIdentifier<?> path, DataObject object, boolean pretty) {
            final SchemaPath scPath = SchemaPath.create(FluentIterable.from(path.getPathArguments()).transform(new Function<PathArgument, QName>() {

                @Override
                public QName apply(final PathArgument input) {
                    return BindingReflections.findQName(input.getType());
                }

            }), true);

            final Writer writer = new StringWriter();
            final NormalizedNodeStreamWriter domWriter;
            if(pretty)
                domWriter = JSONNormalizedNodeStreamWriter.createExclusiveWriter(JSONCodecFactory.create(context), scPath.getParent(), scPath.getLastComponent().getNamespace(), JsonWriterFactory.createJsonWriter(writer,2));
            else
                domWriter = JSONNormalizedNodeStreamWriter.createExclusiveWriter(JSONCodecFactory.create(context), scPath.getParent(), scPath.getLastComponent().getNamespace(), JsonWriterFactory.createJsonWriter(writer));
            final BindingStreamEventWriter bindingWriter = codecRegistry.newWriter(path, domWriter);

            try {
              codecRegistry.getSerializer(path.getTargetType()).serialize(object, bindingWriter);
            } catch (final IOException e) {
                throw new IllegalStateException(e);
            }
            String writerString = writer.toString();
            try {
				writer.close();
			} catch (IOException e) {
				ErrorLog.logError(e.getStackTrace());
			}
            return writerString;
    }

    /**
     * Converts a YANG Notification to a JSON String
     * @param clz - Notification subclass
     * @param notification - Notification instance
     * @param pretty - indicates if pretty JSON (whitespace) should be produced
     * @return a String representing the Notification or an empty string if some error occurred.
     */
    public String notificationToJsonString(Class<? extends Notification> clz,
            DataObject notification,
            boolean pretty) {
        final Writer writer = new StringWriter();
        NormalizedNode<?, ?> node = null;
        try {
        	node = codecRegistry.toNormalizedNodeNotification((Notification)notification);
        } catch (RuntimeException e){
        	try {
				Thread.sleep(200);
			} catch (InterruptedException e1) {
				ErrorLog.logError(e1.getStackTrace());
			}
        	node = codecRegistry.toNormalizedNodeNotification((Notification)notification);
        }
        try {
            final SchemaPath scPath = SchemaPath.create(true, BindingReflections.findQName(notification.getImplementedInterface()));

            String str =  normalizedNodeToJsonStreamTransformation(writer, scPath, node);
            writer.close();
            return str;
        } catch (IOException e) {
            LOG.info("Error encounterd during notification => JSON serialization");
            ErrorLog.logError(e.getStackTrace());
        }
        return "";
    }

    /**
     * Returns a Normalized Node as a JSON String
     * @param writer - Writer to use
     * @param scPath - Schema Context Path for the Normalized Node
     * @param inputStructure - A Normalized Node
     * @return a String representing the Normalized Node
     * @throws IOException - if an error was encountered when writing the string
     */
    public String normalizedNodeToJsonStreamTransformation(final Writer writer,
            SchemaPath scPath,
            final NormalizedNode<?, ?> inputStructure) throws IOException {

        final NormalizedNodeStreamWriter jsonStream = JSONNormalizedNodeStreamWriter.
                createExclusiveWriter(JSONCodecFactory.create(context), scPath, scPath.getLastComponent().getNamespace(),
                    JsonWriterFactory.createJsonWriter(writer, 2));
        final NormalizedNodeWriter nodeWriter = NormalizedNodeWriter.forStreamWriter(jsonStream);
        nodeWriter.write(inputStructure);

        nodeWriter.close();
        return writer.toString();
    }

    /**
     * Returns a Normalized Node from a JSON String
     * @param inputJson - JSON input
     * @return A Normalized Node generated based upon the input string
     */
    public NormalizedNode<?,?> normalizedNodeFromJsonString(final String inputJson) {
        final NormalizedNodeResult result = new NormalizedNodeResult();
        final NormalizedNodeStreamWriter streamWriter = ImmutableNormalizedNodeStreamWriter.from(result);
        // note: context used to be generated by using loadModules from TestUtils in
        //       org.opendaylight.yangtools.yang.data.codec.gson
        final JsonParserStream jsonParser = JsonParserStream.create(streamWriter, context);
        jsonParser.parse(new JsonReader(new StringReader(inputJson)));
        final NormalizedNode<?, ?> transformedInput = result.getResult();
        return transformedInput;
    }

    /**
     * Creates a DataObject from a Normalized Node
     * @param iid - Yang Instance Identifier
     * @param nn - Normalized Node
     * @return DataObject based upon the Normalized Node and Yang Instance Identifier
     */
    public DataObject dataObjectFromNormalizedNode(YangInstanceIdentifier iid, NormalizedNode<?, ?> nn) {
        return codecRegistry.fromNormalizedNode(iid, nn).getValue();
    }

    /**
     * Creates a DataObject from a Normalized Node
     * @param nn - Normalized Node
     * @return DataObject based upon the Normalized Node
     */
    public DataObject dataObjectFromNormalizedNode(NormalizedNode<?, ?> nn) {
        return codecRegistry.fromNormalizedNode(TOP_PATH, nn).getValue();
    }

    /**
     * Creates a FpcCodecUtils instance.
     * @param module - Yang Module the instance should use for basis
     * @param topPath - Top path for the instance
     * @return FpcCodecUtils
     */
    static public FpcCodecUtils get(Class<?> module,
            YangInstanceIdentifier topPath) {
         try {
             return new FpcCodecUtils(Collections.singleton(BindingReflections
                     .getModuleInfo(module)), topPath);
         } catch (Exception e) {
             LOG.error("Exception occured during FpcCodecUtilsInitialization");
             throw Throwables.propagate(e);
         }
    }

    /**
     * Creates a FpcCodecUtils instance based upon the FPC Agent module and top path.
     * @return FpcCodecUtils based upon FPC Agent Module
     */
    static public FpcCodecUtils get() {
        return get(IetfDmmFpcagentData.class, TOP_ODL_FPC_PATH);
    }
}
