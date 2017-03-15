/*
 * Copyright © 2016 Copyright (c) Sprint, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.fpc.utils;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLDecoder;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.opendaylight.fpc.utils.yangtools.SchemaManager;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.IetfDmmFpcagentData;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcbase.rev160803.FpcIdentity;
import org.opendaylight.yangtools.sal.binding.generator.impl.ModuleInfoBackedContext;
import org.opendaylight.yangtools.yang.binding.YangModuleInfo;
import org.opendaylight.yangtools.yang.binding.util.BindingReflections;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.InstanceIdentifierBuilder;
import org.opendaylight.yangtools.yang.data.impl.codec.TypeDefinitionAwareCodec;
import org.opendaylight.yangtools.yang.model.api.AnyXmlSchemaNode;
import org.opendaylight.yangtools.yang.model.api.ChoiceCaseNode;
import org.opendaylight.yangtools.yang.model.api.ChoiceSchemaNode;
import org.opendaylight.yangtools.yang.model.api.ContainerSchemaNode;
import org.opendaylight.yangtools.yang.model.api.DataNodeContainer;
import org.opendaylight.yangtools.yang.model.api.DataSchemaNode;
import org.opendaylight.yangtools.yang.model.api.LeafListSchemaNode;
import org.opendaylight.yangtools.yang.model.api.LeafSchemaNode;
import org.opendaylight.yangtools.yang.model.api.ListSchemaNode;
import org.opendaylight.yangtools.yang.model.api.Module;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang.model.api.TypeDefinition;
import org.opendaylight.yangtools.yang.model.api.type.IdentityrefTypeDefinition;
import org.opendaylight.yangtools.yang.model.api.type.InstanceIdentifierTypeDefinition;
import org.opendaylight.yangtools.yang.model.api.type.LeafrefTypeDefinition;
import org.opendaylight.yangtools.yang.model.util.SchemaContextUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Function;
import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.base.Throwables;
import com.google.common.collect.Iterables;;

/**
 * FPC Name Resolver.
 *
 * It is a gut of multiple objects used by the sal-rest-connector to only support
 * de-serialization to a YangInstance.
 */
public class NameResolver {
    private final static Logger LOG = LoggerFactory.getLogger(NameResolver.class);
    private volatile SchemaContext globalSchema;

    /**
     * Known FPC Schema fixed types.
     */
    public enum FixedType {
        CONTEXT,
        PORT,
        POLICYGROUP,
        POLICY,
        ACTION,
        DESCRIPTOR
    }

    private static final Splitter SLASH_SPLITTER = Splitter.on('/');
    private final static String URI_ENCODING_CHAR_SET = "ISO-8859-1";
    private final static String NULL_VALUE = "null";
    private final static Map<FixedType, Map.Entry<Pattern,Integer>> entityPatterns
        = new HashMap<FixedType, Map.Entry<Pattern,Integer>>();

    static {
        entityPatterns.put( FixedType.CONTEXT, new AbstractMap.SimpleEntry<Pattern,Integer>(
                Pattern.compile("/ietf-dmm-fpcagent:tenants/tenant/[^ /]+/fpc-mobility/contexts/([^ /]+)"), 0));
        entityPatterns.put( FixedType.PORT, new AbstractMap.SimpleEntry<Pattern,Integer>(
                Pattern.compile("/ietf-dmm-fpcagent:tenants/tenant/[^ /]+/fpc-mobility/ports/([^ /]+)") , 0));
    }

    /**
     * Creates a NameResolver for a specific module.
     * @param module - Class instance of a Yang Module
     * @return NameResolver instance
     */
    static public NameResolver get(Class<?> module) {
         try {
             return new NameResolver(Collections.singleton(BindingReflections
                     .getModuleInfo(module)));
         } catch (Exception e) {
             LOG.error("Exception occured during FpcCodecUtilsInitialization");
             throw Throwables.propagate(e);
         }
    }

    /**
     * Creates a NameResolver for the FPC Agent module.
     * @return NameResolver instance
     */
    static public NameResolver get() {
        return get(IetfDmmFpcagentData.class);
    }

    /**
     * Default Constructor.
     */
    public NameResolver() {
        this(null);
    }

    /**
     * Constructor.
     * @param moduleInfos - Modules to use as a basis for the Resolver
     */
    public NameResolver(Iterable<? extends YangModuleInfo> moduleInfos) {
        final ModuleInfoBackedContext moduleContext = SchemaManager.get();
        if (moduleInfos != null) {
            moduleContext.addModuleInfos(moduleInfos);
        }
        globalSchema =  moduleContext.tryToCreateSchemaContext().get();
    }

    /**
     * Sets the schema context for this resolver.
     * @param schemas - SchemaContext
     */
    public void setSchemas(final SchemaContext schemas) {
        onGlobalContextUpdated(schemas);
    }

    /**
     * Retrieves the Global Schema for the Context
     * @return SchemaContext
     */
    public SchemaContext getGlobalSchema() {
        return globalSchema;
    }

    /**
     * Listener call for Schema Context updates.
     * @param context - SchemaContext
     */
    public void onGlobalContextUpdated(final SchemaContext context) {
        if (context != null) {
            setGlobalSchema(context);
        }
    }

    /**
     * Sets the Global Schema Context
     * @param globalSchema - SchemaContext
     */
    public void setGlobalSchema(final SchemaContext globalSchema) {
        this.globalSchema = globalSchema;
        //dataNormalizer = new DataNormalizer(globalSchema);
    }

    /**
     * Transforms a RestConf based URI instance (JSON String) to a YangInstanceIdentifier
     * @param restconfInstance - URI
     * @return YangInstanceIdentifier
     */
    public YangInstanceIdentifier toInstanceIdentifier(final String restconfInstance) {
        if ((globalSchema == null) || (restconfInstance == null)) {
            return null;
        }

        List<String> pathArgs;
        try {
            pathArgs = urlPathArgsDecode(SLASH_SPLITTER.split(restconfInstance));
        } catch (Exception e) {
            return null;
        }
        omitFirstAndLastEmptyString(pathArgs);
        if (pathArgs.isEmpty()) {
            return null;
        }

        final String first = "ietf-dmm-fpcagent:tenants";
        final String startModule = toModuleName(first);
        if (startModule == null) {
            return null;
        }

        final InstanceIdentifierBuilder builder = YangInstanceIdentifier.builder();
        final Module latestModule = globalSchema.findModuleByName(startModule, null);

        if (latestModule == null) {
            return null;
        }

        return collectPathArguments(builder, pathArgs, latestModule);
    }

    /**
     * Decodes URI path arguments into a String list
     * @param strings - URI path list
     * @return a list of strings decoded from URI path arguments
     * @throws Exception if an invalid path as provided
     */
    private static List<String> urlPathArgsDecode(final Iterable<String> strings) throws Exception {
        try {
            final List<String> decodedPathArgs = new ArrayList<String>();
            for (final String pathArg : strings) {
                final String _decode = URLDecoder.decode(pathArg, URI_ENCODING_CHAR_SET);
                decodedPathArgs.add(_decode);
            }
            return decodedPathArgs;
        } catch (final UnsupportedEncodingException e) {
            throw new Exception("Invalid URL path '" + strings + "': " + e.getMessage());
        }
    }

    /**
     * Trims the string list
     * @param list - A list of strings
     * @return a trimmed string list
     */
    private static List<String> omitFirstAndLastEmptyString(final List<String> list) {
        if (list.isEmpty()) {
            return list;
        }
        if (list.get(0).isEmpty()) {
            list.remove(0);
        }
        if (list.isEmpty()) {
            return list;
        }
        if (list.get(list.size() - 1).isEmpty()) {
            list.remove(list.size() - 1);
        }
        return list;
    }

    /**
     * Returns a module name from the string <i>modulename:nodename</i>.
     * @param str - string value
     * @return - Module name (if present)
     */
    private static String toModuleName(final String str) {
        final int idx = str.indexOf(':');
        if ((idx == -1) ||
            (str.indexOf(':', idx + 1) != -1)) {
            return null;
        }
        return str.substring(0, idx);
    }

    /**
     * Returns a node name from the string <i>modulename:nodename</i>.
     * @param str - string value
     * @return - Node name (if present)
     */
    private static String toNodeName(final String str) {
        final int idx = str.indexOf(':');
        if ((idx == -1) ||
            (str.indexOf(':', idx + 1) != -1)) {
            return str;
        }
        return str.substring(idx + 1);
    }

    /**
     * Collects the path arguments to provide a YangInstanceIdentifier.
     * @param builder - InstanceIdentifierBuilder
     * @param strings - URI parameters
     * @param parentNode - Parent Node of the structure
     * @return YangInstanceIdentifier based upon the input
     */
    private YangInstanceIdentifier collectPathArguments(final InstanceIdentifierBuilder builder,
            final List<String> strings, final DataNodeContainer parentNode) {
        Preconditions.<List<String>> checkNotNull(strings);

        if (parentNode == null) {
            return null;
        }

        if (strings.isEmpty()) {
            return builder.build();
        }

        final String head = strings.iterator().next();
        final String nodeName = toNodeName(head);
        final String moduleName = toModuleName(head);

        DataSchemaNode targetNode = null;
        if (!Strings.isNullOrEmpty(moduleName)) {
            Module module = null;
            if (globalSchema == null) {
                return null;
            }
            module = globalSchema.findModuleByName(moduleName, null);
            if (module == null) {
                return null;
            }

            targetNode = findInstanceDataChildByNameAndNamespace(parentNode, nodeName, module.getNamespace());

            if (targetNode == null) {
                return null;
            }
        } else {
            final List<DataSchemaNode> potentialSchemaNodes = findInstanceDataChildrenByName(parentNode, nodeName);
            if ((potentialSchemaNodes.size() > 1) || (potentialSchemaNodes.isEmpty())) {
                return null;
            }

            targetNode = potentialSchemaNodes.iterator().next();
        }

        if (!isListOrContainer(targetNode)) {
            return null;
        }

        int consumed = 1;
        if ((targetNode instanceof ListSchemaNode)) {
            final ListSchemaNode listNode = ((ListSchemaNode) targetNode);
            final int keysSize = listNode.getKeyDefinition().size();
            if ((strings.size() - consumed) < keysSize) {
                return null;
            }

            final List<String> uriKeyValues = strings.subList(consumed, consumed + keysSize);
            final HashMap<QName, Object> keyValues = new HashMap<QName, Object>();
            int i = 0;
            for (final QName key : listNode.getKeyDefinition()) {
                {
                    final String uriKeyValue = uriKeyValues.get(i);
                    if (uriKeyValue.equals(NULL_VALUE)) {
                        return null;
                    }

                    try {
                        addKeyValue(keyValues, listNode.getDataChildByName(key), uriKeyValue);
                    } catch (Exception e) {
                        return null;
                    }
                    i++;
                }
            }

            consumed = consumed + i;
            // The following line is added for the inmemory DOM store...
            builder.node(targetNode.getQName());
            builder.nodeWithKey(targetNode.getQName(), keyValues);
        } else {
            builder.node(targetNode.getQName());
        }

        if ((targetNode instanceof DataNodeContainer)) {
            final List<String> remaining = strings.subList(consumed, strings.size());
            return collectPathArguments(builder, remaining, ((DataNodeContainer) targetNode));
        }

        return builder.build();
    }

    /**
     * Looks for a DataSchemaNode by Name and Namespace.
     * @param container - Container
     * @param name - Child Name
     * @param namespace - Namespace
     * @return DataSchemaNode or null if one was not found
     */
    private static DataSchemaNode findInstanceDataChildByNameAndNamespace(final DataNodeContainer container, final String name,
            final URI namespace) {
        Preconditions.<URI> checkNotNull(namespace);

        final List<DataSchemaNode> potentialSchemaNodes = findInstanceDataChildrenByName(container, name);

        final Predicate<DataSchemaNode> filter = new Predicate<DataSchemaNode>() {
            @Override
            public boolean apply(final DataSchemaNode node) {
                return Objects.equal(node.getQName().getNamespace(), namespace);
            }
        };

        final Iterable<DataSchemaNode> result = Iterables.filter(potentialSchemaNodes, filter);
        return Iterables.getFirst(result, null);
    }

    /**
     * Looks for a DataSchemaNode by Name.
     * @param container - Container
     * @param name - Child Name
     * @return DataSchemaNode or null if one was not found
     */
    public static List<DataSchemaNode> findInstanceDataChildrenByName(final DataNodeContainer container, final String name) {
        Preconditions.<DataNodeContainer> checkNotNull(container);
        Preconditions.<String> checkNotNull(name);

        final List<DataSchemaNode> instantiatedDataNodeContainers = new ArrayList<DataSchemaNode>();
        collectInstanceDataNodeContainers(instantiatedDataNodeContainers, container, name);
        return instantiatedDataNodeContainers;
    }

    /**
     * Internal Function for returning Node cases.
     */
    private static final Function<ChoiceSchemaNode, Set<ChoiceCaseNode>> CHOICE_FUNCTION = new Function<ChoiceSchemaNode, Set<ChoiceCaseNode>>() {
        @Override
        public Set<ChoiceCaseNode> apply(final ChoiceSchemaNode node) {
            return node.getCases();
        }
    };

    /**
     * Collects Instance DataNode Containers.
     * @param potentialSchemaNodes - A list of possible DataSchemaNode values
     * @param container - DataNodeContainer
     * @param name - the Name of the data node
     */
    private static void collectInstanceDataNodeContainers(final List<DataSchemaNode> potentialSchemaNodes,
            final DataNodeContainer container, final String name) {

        final Predicate<DataSchemaNode> filter = new Predicate<DataSchemaNode>() {
            @Override
            public boolean apply(final DataSchemaNode node) {
                return Objects.equal(node.getQName().getLocalName(), name);
            }
        };

        final Iterable<DataSchemaNode> nodes = Iterables.filter(container.getChildNodes(), filter);

        // Can't combine this loop with the filter above because the filter is
        // lazily-applied by Iterables.filter.
        for (final DataSchemaNode potentialNode : nodes) {
            if (isInstantiatedDataSchema(potentialNode)) {
                potentialSchemaNodes.add(potentialNode);
            }
        }

        final Iterable<ChoiceSchemaNode> choiceNodes = Iterables.filter(container.getChildNodes(), ChoiceSchemaNode.class);
        final Iterable<Set<ChoiceCaseNode>> map = Iterables.transform(choiceNodes, CHOICE_FUNCTION);

        final Iterable<ChoiceCaseNode> allCases = Iterables.<ChoiceCaseNode> concat(map);
        for (final ChoiceCaseNode caze : allCases) {
            collectInstanceDataNodeContainers(potentialSchemaNodes, caze, name);
        }
    }

    /**
     * Determines if the DataSchemaNode is one that is instantiated.
     * @param node - DataSchemaNode
     * @return true if the DataSchemaNode is one that is instantiated, false otherwise
     */
    private static boolean isInstantiatedDataSchema(final DataSchemaNode node) {
        return node instanceof LeafSchemaNode || node instanceof LeafListSchemaNode
                || node instanceof ContainerSchemaNode || node instanceof ListSchemaNode
                || node instanceof AnyXmlSchemaNode;
    }

    /**
     * Determines if the DataSchemaNode is a List or Container.
     * @param node - DataSchemaNode
     * @return true if the DataSchemaNode is a List or Container, false otherwise
     */
    private static boolean isListOrContainer(final DataSchemaNode node) {
        return node instanceof ListSchemaNode || node instanceof ContainerSchemaNode;
    }

    /**
     * Adds a Key Value to the map for a specific uri value.
     * @param map - current QName to Object mapping
     * @param node - DataSchemaNode 
     * @param uriValue - uri value to add
     * @throws Exception if error occurs during Uri decoding.
     */
    private void addKeyValue(final HashMap<QName, Object> map, final DataSchemaNode node, final String uriValue)
            throws Exception {
        Preconditions.checkNotNull(uriValue);
        Preconditions.checkArgument((node instanceof LeafSchemaNode));

        final String urlDecoded = urlPathArgDecode(uriValue);
        TypeDefinition<?> typedef = ((LeafSchemaNode) node).getType();
        final TypeDefinition<?> baseType = resolveBaseTypeFrom(typedef);
        if (baseType instanceof LeafrefTypeDefinition) {
            typedef = SchemaContextUtil.getBaseTypeForLeafRef((LeafrefTypeDefinition) baseType, globalSchema, node);
        }

        Object decoded = deserialize(typedef, urlDecoded);
        String additionalInfo = "";
        if (decoded == null) {
            if ((baseType instanceof IdentityrefTypeDefinition)) {
                decoded = toQName(urlDecoded);
                additionalInfo = "For key which is of type identityref it should be in format module_name:identity_name.";
            }
        }

        if (decoded == null) {
            throw new Exception(uriValue + " from URI can't be resolved. " + additionalInfo);
        }

        map.put(node.getQName(), decoded);
    }

    /**
     * Decodes a URL path argument
     * @param pathArg - URL path
     * @return a String representing the URL Decoded path
     * @throws Exception if the URL path decoding fails
     */
    private String urlPathArgDecode(final String pathArg) throws Exception {
        if (pathArg != null) {
            try {
                return URLDecoder.decode(pathArg, URI_ENCODING_CHAR_SET);
            } catch (final UnsupportedEncodingException e) {
                throw new Exception("Invalid URL path arg '" + pathArg + "': " + e.getMessage());
            }
        }

        return null;
    }

    /**
     * Resolves the Base Type of a TypeDefinition
     * @param type - TypeDefinition
     * @return Base type for the TypeDefinition
     */
    private TypeDefinition<?> resolveBaseTypeFrom(final TypeDefinition<?> type) {
        TypeDefinition<?> superType = type;
        while (superType.getBaseType() != null) {
            superType = superType.getBaseType();
        }
        return superType;
    }

    /**
     * Returns the QName of a string
     * @param name - string containing the module and node name
     * @return QName of the string or null if no module could be found for the input
     */
    private QName toQName(final String name) {
        if (globalSchema == null) {
            return null;
        }
        final String module = toModuleName(name);
        final String node = toNodeName(name);
        final Module m = globalSchema.findModuleByName(module, null);
        return m == null ? null : QName.create(m.getQNameModule(), node);
    }

    /**
     * De-serializes a string.
     * @param typeDefinition - Type Definition
     * @param input - Input
     * @return De-serialized JSON string
     */
    private Object deserialize(final TypeDefinition<?> typeDefinition, String input) {
        TypeDefinition<?> type = resolveBaseTypeFrom(typeDefinition);

        try {
            if ((type instanceof IdentityrefTypeDefinition) || (type instanceof InstanceIdentifierTypeDefinition)) {
                return null;
            } else {
                final TypeDefinitionAwareCodec<Object, ? extends TypeDefinition<?>> typeAwarecodec = TypeDefinitionAwareCodec
                        .from(type);
                if (typeAwarecodec != null) {
                    return typeAwarecodec.deserialize(input);
                } else {
                    return null;
                }
            }
        } catch (final ClassCastException e) {
            LOG.error(
                    "ClassCastException was thrown when codec is invoked with parameter " + String.valueOf(input),
                    e);
            return null;
        }
    }

    /**
     * Extracts a key from a RestConf path
     * @param restconfPath - RestConf URI path
     * @return Key from the RestConf URI path
     */
    public String extractKey(String restconfPath) {
        for (Map.Entry<FixedType, Map.Entry<Pattern,Integer>> p : entityPatterns.entrySet()) {
            Matcher m = p.getValue().getKey().matcher(restconfPath);
            if (m.matches()) {
                return m.group(p.getValue().getValue());
            }
        }
        return null;
    }

    /**
     * Extracts a type and key from a RestConf path
     * @param restconfPath - RestConf URI path
     * @return a pair (Map.Entry) of the FixedType and Key from the RestConf URI path
     */
    public Map.Entry<FixedType, String> extractTypeAndKey(String restconfPath) {
        for (Map.Entry<FixedType, Map.Entry<Pattern,Integer>> p : entityPatterns.entrySet()) {
            Matcher m = p.getValue().getKey().matcher(restconfPath);
            if (m.matches()) {
                return new AbstractMap.SimpleEntry<FixedType, String>(p.getKey(), m.group(p.getValue().getValue()));
            }
        }
        return null;
    }

    /**
     * Extracts a String value from an FpcIdentity
     * @param value - FPC Identity
     * @return A string representation of the Fpc Identity.
     */
    static  public String extractString(FpcIdentity value) {
        if (value != null) {
            if (value.getString() != null) {
                return value.getString();
            } else
           if (value.getUint32() != null) {
               return value.getUint32().toString();
           } else

           if (value.getInstanceIdentifier() != null) {
               return value.getInstanceIdentifier().toString();
           }
       }
       return null;
    }
}
