/* SPDX-License-Identifier: Apache 2.0 */
/* Copyright Contributors to the ODPi Egeria project. */
package org.odpi.openmetadata.accessservices.dataengine.server.service;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.odpi.openmetadata.accessservices.dataengine.ffdc.DataEngineErrorCode;
import org.odpi.openmetadata.accessservices.dataengine.model.*;
import org.odpi.openmetadata.accessservices.dataengine.model.Process;
import org.odpi.openmetadata.accessservices.dataengine.rest.*;
import org.odpi.openmetadata.accessservices.dataengine.server.admin.DataEngineInstanceHandler;
import org.odpi.openmetadata.accessservices.dataengine.server.handlers.DataEngineRegistrationHandler;
import org.odpi.openmetadata.accessservices.dataengine.server.handlers.DataEngineSchemaTypeHandler;
import org.odpi.openmetadata.accessservices.dataengine.server.handlers.PortHandler;
import org.odpi.openmetadata.accessservices.dataengine.server.handlers.ProcessHandler;
import org.odpi.openmetadata.accessservices.dataengine.server.mappers.PortPropertiesMapper;
import org.odpi.openmetadata.commonservices.ffdc.RESTExceptionHandler;
import org.odpi.openmetadata.commonservices.ffdc.rest.FFDCResponseBase;
import org.odpi.openmetadata.commonservices.ffdc.rest.GUIDResponse;
import org.odpi.openmetadata.commonservices.ffdc.rest.VoidResponse;
import org.odpi.openmetadata.commonservices.ocf.metadatamanagement.rest.ConnectionResponse;
import org.odpi.openmetadata.frameworks.connectors.ffdc.InvalidParameterException;
import org.odpi.openmetadata.frameworks.connectors.ffdc.PropertyServerException;
import org.odpi.openmetadata.frameworks.connectors.ffdc.UserNotAuthorizedException;
import org.odpi.openmetadata.repositoryservices.connectors.stores.metadatacollectionstore.properties.instances.EntityDetail;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.stream.Collectors.partitioningBy;

/**
 * The DataEngineRESTServices provides the server-side implementation of the Data Engine Open Metadata Assess Service
 * (OMAS). This service provide the functionality to create processes, ports with schema types and corresponding
 * relationships.
 */
public class DataEngineRESTServices {

    private static final Logger log = LoggerFactory.getLogger(DataEngineRESTServices.class);
    private static final String DEBUG_MESSAGE_METHOD_DETAILS = "Calling method {} for entity: {}";
    private static final String DEBUG_MESSAGE_METHOD_RETURN = "Returning from method: {} with response: {}";
    public static final String EXCEPTION_WHILE_ADDING_LINEAGE_MAPPING = "Exception while adding lineage mapping {} : {}";
    public static final String EXCEPTION_WHILE_CREATING_PROCESS = "Exception while creating process {} : {}";
    public static final String EXCEPTION_WHILE_CREATING_PROCESS_HIERARCHY = "Exception while creating process relationships for process {} : {}";

    private final RESTExceptionHandler restExceptionHandler = new RESTExceptionHandler();

    private final DataEngineInstanceHandler instanceHandler = new DataEngineInstanceHandler();

    /**
     * Create the external data engine as software server capability entity
     *
     * @param serverName  name of server instance to call
     * @param userId      the name of the calling user
     * @param requestBody properties of the server
     *
     * @return the unique identifier (guid) of the created external data engine
     */
    public GUIDResponse createExternalDataEngine(String serverName, String userId,
                                                 DataEngineRegistrationRequestBody requestBody) {
        final String methodName = "createExternalDataEngine";

        GUIDResponse response = new GUIDResponse();

        try {
            if (requestBody == null) {
                restExceptionHandler.handleNoRequestBody(userId, methodName, serverName);
                return response;
            }

            response.setGUID(createExternalDataEngine(userId, serverName, requestBody.getSoftwareServerCapability()));

        } catch (InvalidParameterException error) {
            restExceptionHandler.captureInvalidParameterException(response, error);
        } catch (PropertyServerException error) {
            restExceptionHandler.capturePropertyServerException(response, error);
        } catch (UserNotAuthorizedException error) {
            restExceptionHandler.captureUserNotAuthorizedException(response, error);
        }

        log.debug(DEBUG_MESSAGE_METHOD_RETURN, methodName, response);

        return response;
    }

    /**
     * Get the unique identifier from a external data engine qualified name
     *
     * @param serverName    name of the service to route the request to
     * @param userId        identifier of calling user
     * @param qualifiedName qualified name of the external data engine
     *
     * @return the unique identifier from a software server capability definition for an external data engine
     */
    public GUIDResponse getExternalDataEngineByQualifiedName(String serverName, String userId, String qualifiedName) {
        final String methodName = "getExternalDataEngineByQualifiedName";

        log.debug(DEBUG_MESSAGE_METHOD_DETAILS, methodName, qualifiedName);

        GUIDResponse response = new GUIDResponse();

        try {
            DataEngineRegistrationHandler handler = instanceHandler.getRegistrationHandler(userId, serverName, methodName);

            response.setGUID(handler.getExternalDataEngineByQualifiedName(userId, qualifiedName));

        } catch (InvalidParameterException error) {
            restExceptionHandler.captureInvalidParameterException(response, error);
        } catch (PropertyServerException error) {
            restExceptionHandler.capturePropertyServerException(response, error);
        } catch (UserNotAuthorizedException error) {
            restExceptionHandler.captureUserNotAuthorizedException(response, error);
        }

        log.debug(DEBUG_MESSAGE_METHOD_RETURN, methodName, response);

        return response;
    }

    /**
     * Create the SchemaType with schema attributes and corresponding relationships
     *
     * @param serverName            name of server instance to call
     * @param userId                the name of the calling user
     * @param schemaTypeRequestBody properties of the schema type
     *
     * @return the unique identifier (guid) of the created schema type
     */
    public GUIDResponse createOrUpdateSchemaType(String userId, String serverName, SchemaTypeRequestBody schemaTypeRequestBody) {
        final String methodName = "createOrUpdateSchemaType";

        GUIDResponse response = new GUIDResponse();

        try {
            if (schemaTypeRequestBody == null) {
                restExceptionHandler.handleNoRequestBody(userId, methodName, serverName);
                return response;
            }

            String newSchemaTypeGUID = createOrUpdateSchemaType(userId, serverName, schemaTypeRequestBody.getSchemaType(),
                    schemaTypeRequestBody.getExternalSourceName());

            response.setGUID(newSchemaTypeGUID);

        } catch (InvalidParameterException error) {
            restExceptionHandler.captureInvalidParameterException(response, error);
        } catch (PropertyServerException error) {
            restExceptionHandler.capturePropertyServerException(response, error);
        } catch (UserNotAuthorizedException error) {
            restExceptionHandler.captureUserNotAuthorizedException(response, error);
        }

        return response;
    }

    /**
     * Create or update the Port Implementation with a PortSchema relationship
     *
     * @param serverName                    name of server instance to call
     * @param userId                        the name of the calling user
     * @param portImplementationRequestBody properties of the port
     *
     * @return the unique identifier (guid) of the created port
     */
    public GUIDResponse createOrUpdatePortImplementation(String userId, String serverName,
                                                         PortImplementationRequestBody portImplementationRequestBody) {
        final String methodName = "createOrUpdatePortImplementation";

        GUIDResponse response = new GUIDResponse();
        try {
            if (portImplementationRequestBody == null) {
                restExceptionHandler.handleNoRequestBody(userId, methodName, serverName);
                return response;
            }

            String portImplementationGUID = createOrUpdatePortImplementationWithSchemaType(userId, serverName,
                    portImplementationRequestBody.getPortImplementation(), portImplementationRequestBody.getExternalSourceName());

            response.setGUID(portImplementationGUID);

        } catch (InvalidParameterException error) {
            restExceptionHandler.captureInvalidParameterException(response, error);
        } catch (PropertyServerException error) {
            restExceptionHandler.capturePropertyServerException(response, error);
        } catch (UserNotAuthorizedException error) {
            restExceptionHandler.captureUserNotAuthorizedException(response, error);
        }

        return response;
    }

    /**
     * Create or update the Port Alias with a PortDelegation relationship
     *
     * @param serverName           name of server instance to call
     * @param userId               the name of the calling user
     * @param portAliasRequestBody properties of the port
     *
     * @return the unique identifier (guid) of the created port
     */
    public GUIDResponse createOrUpdatePortAlias(String userId, String serverName, PortAliasRequestBody portAliasRequestBody) {
        final String methodName = "createOrUpdatePortAliasWithDelegation";

        GUIDResponse response = new GUIDResponse();

        try {
            if (portAliasRequestBody == null) {
                restExceptionHandler.handleNoRequestBody(userId, methodName, serverName);
                return response;
            }

            response.setGUID(createOrUpdatePortAliasWithDelegation(userId, serverName, portAliasRequestBody.getPortAlias(),
                    portAliasRequestBody.getExternalSourceName()));

        } catch (InvalidParameterException error) {
            restExceptionHandler.captureInvalidParameterException(response, error);
        } catch (PropertyServerException error) {
            restExceptionHandler.capturePropertyServerException(response, error);
        } catch (UserNotAuthorizedException error) {
            restExceptionHandler.captureUserNotAuthorizedException(response, error);
        }

        return response;
    }

    /**
     * Add the provided ProcessHierarchy relationship
     *
     * @param serverName                  name of server instance to call
     * @param userId                      the name of the calling user
     * @param processHierarchyRequestBody properties of the process hierarchy
     *
     * @return the unique identifier (guid) of the child of the process hierarchy that was updated
     */
    public GUIDResponse addProcessHierarchy(String userId, String serverName, ProcessHierarchyRequestBody processHierarchyRequestBody) {
        final String methodName = "addProcessHierarchy";

        GUIDResponse response = new GUIDResponse();

        try {
            if (processHierarchyRequestBody == null) {
                restExceptionHandler.handleNoRequestBody(userId, methodName, serverName);
                return response;
            }

            response.setGUID(addProcessHierarchyToProcess(userId, serverName, processHierarchyRequestBody.getProcessHierarchy(),
                    processHierarchyRequestBody.getExternalSourceName()));

        } catch (InvalidParameterException error) {
            restExceptionHandler.captureInvalidParameterException(response, error);
        } catch (PropertyServerException error) {
            restExceptionHandler.capturePropertyServerException(response, error);
        } catch (UserNotAuthorizedException error) {
            restExceptionHandler.captureUserNotAuthorizedException(response, error);
        }

        return response;
    }

    /**
     * Create or update the processes with ports, schema types and lineage mappings
     *
     * @param userId               the name of the calling user
     * @param serverName           name of server instance to call
     * @param processesRequestBody properties of the processes
     *
     * @return a list unique identifiers (GUIDs) of the created/updated processes
     */
    public ProcessListResponse createOrUpdateProcesses(String userId, String serverName, ProcessesRequestBody processesRequestBody) {
        final String methodName = "createOrUpdateProcesses";

        ProcessListResponse response = new ProcessListResponse();

        try {
            if (processesRequestBody == null || CollectionUtils.isEmpty(processesRequestBody.getProcesses())) {
                restExceptionHandler.handleNoRequestBody(userId, methodName, serverName);
                return response;
            }

            return createOrUpdateProcesses(userId, serverName, processesRequestBody.getProcesses(), processesRequestBody.getExternalSourceName());
        } catch (InvalidParameterException error) {
            restExceptionHandler.captureInvalidParameterException(response, error);
        }

        return response;
    }

    /**
     * Create or update a Port Alias with a PortDelegation relationship
     *
     * @param userId             the name of the calling user
     * @param serverName         name of server instance to call
     * @param portAlias          the port alias values
     * @param externalSourceName the unique name of the external source
     *
     * @return the unique identifier (guid) of the created port alias
     *
     * @throws InvalidParameterException  the bean properties are invalid
     * @throws UserNotAuthorizedException user not authorized to issue this request
     * @throws PropertyServerException    problem accessing the property server
     */
    public String createOrUpdatePortAliasWithDelegation(String userId, String serverName, PortAlias portAlias, String externalSourceName) throws
                                                                                                                                          InvalidParameterException,
                                                                                                                                          PropertyServerException,
                                                                                                                                          UserNotAuthorizedException {
        final String methodName = "createOrUpdatePortAliasWithDelegation";

        log.debug(DEBUG_MESSAGE_METHOD_DETAILS, methodName, portAlias);

        PortHandler portHandler = instanceHandler.getPortHandler(userId, serverName, methodName);

        Optional<EntityDetail> portEntity = portHandler.findPortAliasEntity(userId, portAlias.getQualifiedName());

        String portAliasGUID;
        if (!portEntity.isPresent()) {
            portAliasGUID = portHandler.createPortAlias(userId, portAlias, externalSourceName);
        } else {
            portAliasGUID = portEntity.get().getGUID();
            portHandler.updatePortAlias(userId, portEntity.get(), portAlias);
        }

        if (!StringUtils.isEmpty(portAlias.getDelegatesTo())) {
            portHandler.addPortDelegationRelationship(userId, portAliasGUID, portAlias.getPortType(), portAlias.getDelegatesTo(), externalSourceName);
        }

        log.debug(DEBUG_MESSAGE_METHOD_RETURN, methodName, portAliasGUID);

        return portAliasGUID;
    }

    /**
     * Add a a ProcessHierarchy relationship to the process
     *
     * @param userId             the name of the calling user
     * @param serverName         name of server instance to call
     * @param processHierarchy   the process hierarchy values
     * @param externalSourceName the unique name of the external source
     *
     * @return the unique identifier (guid) of the child of the process hierarchy that was updated
     *
     * @throws InvalidParameterException  the bean properties are invalid
     * @throws UserNotAuthorizedException user not authorized to issue this request
     * @throws PropertyServerException    problem accessing the property server
     */
    public String addProcessHierarchyToProcess(String userId, String serverName, ProcessHierarchy processHierarchy, String externalSourceName) throws
                                                                                                                                               InvalidParameterException,
                                                                                                                                               PropertyServerException,
                                                                                                                                               UserNotAuthorizedException {
        final String methodName = "addProcessHierarchyToProcess";

        log.debug(DEBUG_MESSAGE_METHOD_DETAILS, methodName, processHierarchy);

        ProcessHandler processHandler = instanceHandler.getProcessHandler(userId, serverName, methodName);

        Optional<EntityDetail> childProcessEntity = processHandler.findProcessEntity(userId, processHierarchy.getChildProcess());

        String childProcessGUID;
        if (childProcessEntity.isPresent()) {
            childProcessGUID = childProcessEntity.get().getGUID();
            ParentProcess parentProcess = new ParentProcess();
            parentProcess.setQualifiedName(processHierarchy.getParentProcess());
            parentProcess.setProcessContainmentType(processHierarchy.getProcessContainmentType());
            processHandler.createOrUpdateProcessHierarchyRelationship(userId, parentProcess, childProcessGUID, externalSourceName);
        } else {
            throw new InvalidParameterException(DataEngineErrorCode.PROCESS_NOT_FOUND.getMessageDefinition(processHierarchy.getChildProcess()), this.getClass().getName(), methodName, "childProcess");
        }

        log.debug(DEBUG_MESSAGE_METHOD_RETURN, methodName, childProcessGUID);

        return childProcessGUID;
    }

    /**
     * Create or update a Port Implementation with an associated SchemaType
     *
     * @param userId             the name of the calling user
     * @param serverName         name of server instance to call
     * @param portImplementation the port implementation values
     * @param externalSourceName the unique name of the external source
     *
     * @return the unique identifier (guid) of the created port alias
     *
     * @throws InvalidParameterException  the bean properties are invalid
     * @throws UserNotAuthorizedException user not authorized to issue this request
     * @throws PropertyServerException    problem accessing the property server
     */
    public String createOrUpdatePortImplementationWithSchemaType(String userId, String serverName, PortImplementation portImplementation,
                                                                 String externalSourceName) throws InvalidParameterException,
                                                                                                   PropertyServerException,
                                                                                                   UserNotAuthorizedException {
        final String methodName = "createOrUpdatePortImplementationWithSchemaType";

        log.debug(DEBUG_MESSAGE_METHOD_DETAILS, methodName, portImplementation);

        PortHandler portHandler = instanceHandler.getPortHandler(userId, serverName, methodName);

        String schemaTypeGUID = createOrUpdateSchemaType(userId, serverName, portImplementation.getSchemaType(), externalSourceName);

        Optional<EntityDetail> portEntity = portHandler.findPortImplementationEntity(userId, portImplementation.getQualifiedName());

        String portImplementationGUID;
        if (!portEntity.isPresent()) {
            portImplementationGUID = portHandler.createPortImplementation(userId, portImplementation, externalSourceName);
        } else {
            portImplementationGUID = portEntity.get().getGUID();
            portHandler.updatePortImplementation(userId, portEntity.get(), portImplementation);

            if (portImplementation.getUpdateSemantic() == UpdateSemantic.REPLACE) {
                deleteObsoleteSchemaType(userId, serverName, schemaTypeGUID, portHandler.findSchemaTypeForPort(userId, portImplementationGUID));
            }
        }

        portHandler.addPortSchemaRelationship(userId, portImplementationGUID, schemaTypeGUID, externalSourceName);

        log.debug(DEBUG_MESSAGE_METHOD_RETURN, methodName, portImplementationGUID);

        return portImplementationGUID;
    }

    /**
     * Create the external data engine as software server capability entity
     *
     * @param userId                   the name of the calling user
     * @param serverName               name of server instance to call
     * @param softwareServerCapability the software server values
     *
     * @return he unique identifier (guid) of the created external data engine
     *
     * @throws InvalidParameterException  the bean properties are invalid
     * @throws UserNotAuthorizedException user not authorized to issue this request
     * @throws PropertyServerException    problem accessing the property server
     */
    public String createExternalDataEngine(String userId, String serverName, SoftwareServerCapability softwareServerCapability) throws
                                                                                                                                InvalidParameterException,
                                                                                                                                PropertyServerException,
                                                                                                                                UserNotAuthorizedException {
        final String methodName = "createExternalDataEngine";

        log.debug(DEBUG_MESSAGE_METHOD_DETAILS, methodName, softwareServerCapability);

        if (softwareServerCapability == null) {
            return null;
        }

        DataEngineRegistrationHandler handler = instanceHandler.getRegistrationHandler(userId, serverName, methodName);

        return handler.createOrUpdateExternalDataEngine(userId, softwareServerCapability);
    }

    /**
     * Create ProcessPort relationships for an existing Process
     *
     * @param userId             the name of the calling user
     * @param serverName         name of server instance to call
     * @param processGUID        the process entity unique identifier(guid)
     * @param portQualifiedNames the list of qualified names for the port entities
     * @param externalSourceName the unique name of the external source
     *
     * @throws InvalidParameterException  the bean properties are invalid
     * @throws UserNotAuthorizedException user not authorized to issue this request
     * @throws PropertyServerException    problem accessing the property server
     */
    public void addPortsToProcess(String userId, String serverName, String processGUID, List<String> portQualifiedNames, String externalSourceName) throws
                                                                                                                                                    InvalidParameterException,
                                                                                                                                                    PropertyServerException,
                                                                                                                                                    UserNotAuthorizedException {
        final String methodName = "addPortsToProcess";

        if (CollectionUtils.isEmpty(portQualifiedNames)) {
            return;
        }

        ProcessHandler processHandler = instanceHandler.getProcessHandler(userId, serverName, methodName);
        PortHandler portHandler = instanceHandler.getPortHandler(userId, serverName, methodName);

        for (String portQualifiedName : portQualifiedNames) {
            Optional<EntityDetail> portEntity = portHandler.findPortEntity(userId, portQualifiedName);
            if (portEntity.isPresent()) {
                processHandler.addProcessPortRelationship(userId, processGUID, portEntity.get().getGUID(), externalSourceName);
            }
        }
    }

    /**
     * Create LineageMappings relationships between schema attributes
     *
     * @param userId             the name of the calling user
     * @param serverName         name of server instance to call
     * @param lineageMappings    the list of lineage mappings to be created
     * @param response           the response object that will capture the exceptions that might occur during
     *                           parallel processing
     * @param externalSourceName the unique name of the external source
     *
     * @throws InvalidParameterException  the bean properties are invalid
     * @throws UserNotAuthorizedException user not authorized to issue this request
     * @throws PropertyServerException    problem accessing the property server
     */
    public void addLineageMappings(String userId, String serverName, List<LineageMapping> lineageMappings, FFDCResponseBase response,
                                   String externalSourceName) throws InvalidParameterException,
                                                                     PropertyServerException,
                                                                     UserNotAuthorizedException {
        final String methodName = "addLineageMappings";

        log.debug(DEBUG_MESSAGE_METHOD_DETAILS, methodName, lineageMappings);

        if (CollectionUtils.isEmpty(lineageMappings)) {
            return;
        }

        DataEngineSchemaTypeHandler dataEngineSchemaTypeHandler = instanceHandler.getDataEngineSchemaTypeHandler(userId, serverName, methodName);

        lineageMappings.parallelStream().forEach(lineageMapping -> {
            try {
                dataEngineSchemaTypeHandler.addLineageMappingRelationship(userId, lineageMapping.getSourceAttribute(),
                        lineageMapping.getTargetAttribute(), externalSourceName);
            } catch (InvalidParameterException error) {
                log.error(EXCEPTION_WHILE_ADDING_LINEAGE_MAPPING, lineageMapping.toString(), error.toString());
                restExceptionHandler.captureInvalidParameterException(response, error);
            } catch (PropertyServerException error) {
                log.error(EXCEPTION_WHILE_ADDING_LINEAGE_MAPPING, lineageMapping.toString(), error.toString());
                restExceptionHandler.capturePropertyServerException(response, error);
            } catch (UserNotAuthorizedException error) {
                log.error(EXCEPTION_WHILE_ADDING_LINEAGE_MAPPING, lineageMapping.toString(), error.toString());
                restExceptionHandler.captureUserNotAuthorizedException(response, error);
            }
        });
    }

    /**
     * @param userId             the name of the calling user
     * @param serverName         name of server instance to call
     * @param processes          list of processes to be created
     * @param externalSourceName the unique name of the external source
     *
     * @return a list unique identifiers (GUIDs) of the created/updated processes
     */
    public ProcessListResponse createOrUpdateProcesses(String userId, String serverName, List<Process> processes, String externalSourceName) {
        final String methodName = "createOrUpdateProcesses";

        log.debug(DEBUG_MESSAGE_METHOD_DETAILS, methodName, processes);

        Predicate<? super Process> hasPortImplementationsPredicate = process -> CollectionUtils.isNotEmpty(process.getPortImplementations());
        Map<Boolean, List<Process>> partitionedProcesses = processes.parallelStream().collect(partitioningBy(hasPortImplementationsPredicate));

        List<GUIDResponse> createdProcesses = new ArrayList<>();
        List<GUIDResponse> failedProcesses = new ArrayList<>();
        Consumer<Process> processConsumer = process ->
        {
            GUIDResponse guidResponse = createOrUpdateProcess(userId, serverName, process, externalSourceName);
            if (guidResponse.getRelatedHTTPCode() == HttpStatus.OK.value()) {
                String processGUID = guidResponse.getGUID();
                process.setGUID(processGUID);
                createdProcesses.add(guidResponse);
            } else {
                failedProcesses.add(guidResponse);
            }
        };

        partitionedProcesses.get(Boolean.TRUE).parallelStream().forEach(processConsumer);
        // processes that have port aliases can not be processed in parallel, as multiple processes can define the same port alias
        partitionedProcesses.get(Boolean.FALSE).forEach(processConsumer);

        ProcessListResponse response = new ProcessListResponse();
        response.setGUIDs(createdProcesses.parallelStream().map(GUIDResponse::getGUID).collect(Collectors.toList()));
        handleFailedProcesses(response, failedProcesses);

        addProcessHierarchyRelationships(userId, serverName, processes, response, externalSourceName);

        log.debug(DEBUG_MESSAGE_METHOD_RETURN, methodName, response);

        return response;
    }

    /**
     * Create ProcessPort relationships for an existing Process
     *
     * @param serverName          name of server instance to call
     * @param userId              the name of the calling user
     * @param processGuid         the guid of the process
     * @param portListRequestBody list of port qualified names
     *
     * @return the unique identifier (guid) of the updated process entity
     */
    public GUIDResponse addPortsToProcess(String userId, String serverName, String processGuid, PortListRequestBody portListRequestBody) {
        final String methodName = "addPortsToProcess";

        GUIDResponse response = new GUIDResponse();

        try {
            if (portListRequestBody == null) {
                restExceptionHandler.handleNoRequestBody(userId, methodName, serverName);
                return response;
            }

            addPortsToProcess(userId, serverName, processGuid, portListRequestBody.getPorts(), portListRequestBody.getExternalSourceName());

            response.setGUID(processGuid);

        } catch (InvalidParameterException error) {
            restExceptionHandler.captureInvalidParameterException(response, error);
        } catch (PropertyServerException error) {
            restExceptionHandler.capturePropertyServerException(response, error);
        } catch (UserNotAuthorizedException error) {
            restExceptionHandler.captureUserNotAuthorizedException(response, error);
        }

        log.debug(DEBUG_MESSAGE_METHOD_RETURN, methodName, response);

        return response;
    }

    /**
     * Create LineageMappings relationships between schema attributes
     *
     * @param userId                     the name of the calling user
     * @param serverName                 ame of server instance to call
     * @param lineageMappingsRequestBody list of lineage mappings
     *
     * @return void response
     */
    public VoidResponse addLineageMappings(String userId, String serverName, LineageMappingsRequestBody lineageMappingsRequestBody) {
        final String methodName = "addLineageMappings";

        VoidResponse response = new VoidResponse();
        try {
            if (lineageMappingsRequestBody == null) {
                restExceptionHandler.handleNoRequestBody(userId, methodName, serverName);
                return response;
            }

            addLineageMappings(userId, serverName, lineageMappingsRequestBody.getLineageMappings(), response,
                    lineageMappingsRequestBody.getExternalSourceName());
        } catch (InvalidParameterException error) {
            restExceptionHandler.captureInvalidParameterException(response, error);
        } catch (PropertyServerException error) {
            restExceptionHandler.capturePropertyServerException(response, error);
        } catch (UserNotAuthorizedException error) {
            restExceptionHandler.captureUserNotAuthorizedException(response, error);
        }

        log.debug(DEBUG_MESSAGE_METHOD_RETURN, methodName, response);

        return response;
    }

    /**
     * Retrieve in topic connection details from the service instance hosting Data Engine access service
     *
     * @param serverName the name of server instance to call
     * @param userId     the name/identifier of the calling user
     *
     * @return OCF API ConnectionResponse object describing the details for the input topic connection used
     * or
     * InvalidParameterException one of the parameters is null or invalid or
     * UserNotAuthorizedException user not authorized to issue this request or
     * PropertyServerException problem retrieving the discovery engine definition
     */
    public ConnectionResponse getInTopicConnection(String serverName, String userId) {

        final String methodName = "getInTopicConnection";
        ConnectionResponse response = new ConnectionResponse();

        try {
            response.setConnection(instanceHandler.getInTopicConnection(userId, serverName, methodName));
        } catch (InvalidParameterException e) {
            restExceptionHandler.captureInvalidParameterException(response, e);
        } catch (UserNotAuthorizedException e) {
            restExceptionHandler.captureUserNotAuthorizedException(response, e);
        } catch (PropertyServerException e) {
            restExceptionHandler.capturePropertyServerException(response, e);
        }

        return response;
    }


    public String createOrUpdateSchemaType(String userId, String serverName, SchemaType schemaType, String externalSourceName) throws
                                                                                                                               InvalidParameterException,
                                                                                                                               UserNotAuthorizedException,
                                                                                                                               PropertyServerException {
        final String methodName = "createOrUpdateSchemaType";

        log.debug(DEBUG_MESSAGE_METHOD_DETAILS, methodName, schemaType);

        DataEngineSchemaTypeHandler dataEngineSchemaTypeHandler = instanceHandler.getDataEngineSchemaTypeHandler(userId, serverName, methodName);

        String schemaTypeGUID = dataEngineSchemaTypeHandler.createOrUpdateSchemaType(userId, schemaType, externalSourceName);

        log.debug(DEBUG_MESSAGE_METHOD_RETURN, methodName, schemaTypeGUID);

        return schemaTypeGUID;
    }

    private void deleteObsoleteSchemaType(String userId, String serverName, String schemaTypeGUID, String oldSchemaTypeGUID) throws
                                                                                                                             InvalidParameterException,
                                                                                                                             UserNotAuthorizedException,
                                                                                                                             PropertyServerException {
        final String methodName = "deleteObsoleteSchemaType";

        if (!oldSchemaTypeGUID.equalsIgnoreCase(schemaTypeGUID)) {
            DataEngineSchemaTypeHandler dataEngineSchemaTypeHandler = instanceHandler.getDataEngineSchemaTypeHandler(userId, serverName, methodName);

            dataEngineSchemaTypeHandler.removeSchemaType(userId, oldSchemaTypeGUID);
        }
    }

    private void handleFailedProcesses(ProcessListResponse response, List<GUIDResponse> failedProcesses) {
        response.setFailedGUIDs((failedProcesses.parallelStream().map(GUIDResponse::getGUID).collect(Collectors.toList())));
        failedProcesses.parallelStream().forEach(guidResponse -> captureException(guidResponse, response));
    }

    private void captureException(FFDCResponseBase initialResponse, FFDCResponseBase response) {
        response.setExceptionErrorMessage(initialResponse.getExceptionErrorMessage());
        response.setExceptionClassName(initialResponse.getExceptionClassName());
        response.setExceptionSystemAction(initialResponse.getExceptionSystemAction());
        response.setExceptionUserAction(initialResponse.getExceptionUserAction());
        response.setRelatedHTTPCode(initialResponse.getRelatedHTTPCode());
        response.setExceptionProperties(initialResponse.getExceptionProperties());
    }

    /**
     * Create the process with ports, schema types and lineage mappings
     *
     * @param serverName name of server instance to call
     * @param userId     the name of the calling user
     * @param process    properties of the process
     *
     * @return the unique identifier (guid) of the created process
     */
    private GUIDResponse createOrUpdateProcess(String userId, String serverName, Process process, String externalSourceName) {
        final String methodName = "createOrUpdateProcess";

        log.debug(DEBUG_MESSAGE_METHOD_DETAILS, methodName, process);

        String qualifiedName = process.getQualifiedName();
        List<PortImplementation> portImplementations = process.getPortImplementations();
        List<PortAlias> portAliases = process.getPortAliases();
        List<LineageMapping> lineageMappings = process.getLineageMappings();
        UpdateSemantic updateSemantic = process.getUpdateSemantic();

        GUIDResponse response = new GUIDResponse();

        try {
            Set<String> portImplementationGUIDs = createOrUpdatePortImplementations(userId, serverName, portImplementations, response,
                    externalSourceName);

            Set<String> portAliasGUIDs = createOrUpdatePortAliases(userId, serverName, portAliases, response, externalSourceName);

            //check intermediary status of the response after creating the ports
            if (response.getRelatedHTTPCode() != HttpStatus.OK.value()) {
                return response;
            }

            ProcessHandler processHandler = instanceHandler.getProcessHandler(userId, serverName, methodName);

            Optional<EntityDetail> processEntity = processHandler.findProcessEntity(userId, qualifiedName);
            String processGUID;
            if (!processEntity.isPresent()) {
                processGUID = processHandler.createProcess(userId, process, externalSourceName);

                List<Attribute> schemaAttributes = getAttributes(portImplementations);

                addAnchorGUID(userId, serverName, processGUID, schemaAttributes);
            } else {
                processGUID = processEntity.get().getGUID();
                processHandler.updateProcess(userId, processEntity.get(), process);

                if (updateSemantic == UpdateSemantic.REPLACE) {
                    deleteObsoletePorts(userId, serverName, portImplementationGUIDs, processGUID,
                            PortPropertiesMapper.PORT_IMPLEMENTATION_TYPE_NAME, response);
                    deleteObsoletePorts(userId, serverName, portAliasGUIDs, processGUID, PortPropertiesMapper.PORT_ALIAS_TYPE_NAME, response);
                }
            }

            addProcessPortRelationships(userId, serverName, processGUID,
                    Stream.concat(portImplementationGUIDs.stream(), portAliasGUIDs.stream()).collect(Collectors.toSet()), response,
                    externalSourceName);

            addLineageMappings(userId, serverName, lineageMappings, response, externalSourceName);

            response.setGUID(processGUID);
        } catch (InvalidParameterException error) {
            log.error(EXCEPTION_WHILE_CREATING_PROCESS, qualifiedName, error.toString());
            restExceptionHandler.captureInvalidParameterException(response, error);
        } catch (PropertyServerException error) {
            log.error(EXCEPTION_WHILE_CREATING_PROCESS, qualifiedName, error.toString());
            restExceptionHandler.capturePropertyServerException(response, error);
        } catch (UserNotAuthorizedException error) {
            log.error(EXCEPTION_WHILE_CREATING_PROCESS, qualifiedName, error.toString());
            restExceptionHandler.captureUserNotAuthorizedException(response, error);
        }

        log.debug(DEBUG_MESSAGE_METHOD_RETURN, methodName, response);

        return response;
    }

    private List<Attribute> getAttributes(List<PortImplementation> portImplementations) {
        if (CollectionUtils.isEmpty(portImplementations)) {
            return new ArrayList<>();
        }
        return portImplementations.stream().map(portImplementation -> portImplementation.getSchemaType().getAttributeList())
                .flatMap(Collection::stream).collect(Collectors.toList());
    }

    private void addAnchorGUID(String userId, String serverName, String processGUID, List<Attribute> schemaAttributes) throws
                                                                                                                       InvalidParameterException,
                                                                                                                       PropertyServerException,
                                                                                                                       UserNotAuthorizedException {
        final String methodName = "addAnchorGUID";

        DataEngineSchemaTypeHandler dataEngineSchemaTypeHandler = instanceHandler.getDataEngineSchemaTypeHandler(userId, serverName, methodName);
        for (Attribute attribute : schemaAttributes) {
            dataEngineSchemaTypeHandler.addAnchorGUID(userId, attribute, processGUID);
        }
    }

    private void addProcessHierarchyRelationships(String userId, String serverName, List<Process> processes, ProcessListResponse response,
                                                  String externalSourceName) {
        final String methodName = "addProcessHierarchyRelationships";

        // add the ProcessHierarchy relationships only for successfully created processes
        processes.parallelStream().filter(process -> response.getGUIDs().contains(process.getGUID())).forEach(process -> {
            List<ParentProcess> parentProcesses = process.getParentProcesses();
            String processGUID = process.getGUID();
            if (CollectionUtils.isNotEmpty(parentProcesses)) {
                try {
                    ProcessHandler processHandler = instanceHandler.getProcessHandler(userId, serverName, methodName);
                    for (ParentProcess parentProcess : parentProcesses) {
                        processHandler.createOrUpdateProcessHierarchyRelationship(userId, parentProcess, processGUID, externalSourceName);
                    }
                } catch (InvalidParameterException error) {
                    log.error(EXCEPTION_WHILE_CREATING_PROCESS_HIERARCHY, process.getQualifiedName(), error.toString());
                    restExceptionHandler.captureInvalidParameterException(response, error);
                } catch (PropertyServerException error) {
                    log.error(EXCEPTION_WHILE_CREATING_PROCESS_HIERARCHY, process.getQualifiedName(), error.toString());
                    restExceptionHandler.capturePropertyServerException(response, error);
                } catch (UserNotAuthorizedException error) {
                    log.error(EXCEPTION_WHILE_CREATING_PROCESS_HIERARCHY, process.getQualifiedName(), error.toString());
                    restExceptionHandler.captureUserNotAuthorizedException(response, error);
                }
            }
        });

    }

    private void addProcessPortRelationships(String userId, String serverName, String processGUID, Set<String> portGUIDs, GUIDResponse response,
                                             String externalSourceName) throws InvalidParameterException, PropertyServerException,
                                                                               UserNotAuthorizedException {

        final String methodName = "addProcessPortRelationships";

        ProcessHandler processHandler = instanceHandler.getProcessHandler(userId, serverName, methodName);

        portGUIDs.parallelStream().forEach(portGUID -> {
            try {
                processHandler.addProcessPortRelationship(userId, processGUID, portGUID, externalSourceName);
            } catch (InvalidParameterException error) {
                restExceptionHandler.captureInvalidParameterException(response, error);
            } catch (PropertyServerException error) {
                restExceptionHandler.capturePropertyServerException(response, error);
            } catch (UserNotAuthorizedException error) {
                restExceptionHandler.captureUserNotAuthorizedException(response, error);
            }
        });
    }

    private void deleteObsoletePorts(String userId, String serverName, Set<String> newPortGUIDs, String processGUID, String portTypeName,
                                     GUIDResponse response) throws InvalidParameterException,
                                                                   PropertyServerException,
                                                                   UserNotAuthorizedException {
        final String methodName = "deleteObsoletePorts";

        if (CollectionUtils.isEmpty(newPortGUIDs)) {
            return;
        }

        ProcessHandler processHandler = instanceHandler.getProcessHandler(userId, serverName, methodName);
        PortHandler portHandler = instanceHandler.getPortHandler(userId, serverName, methodName);

        Set<String> oldPortGUIDs = processHandler.getPortsForProcess(userId, processGUID, portTypeName);

        // delete ports that are not in the process payload anymore
        List<String> obsoletePorts = oldPortGUIDs.parallelStream().collect(partitioningBy(newPortGUIDs::contains)).get(Boolean.FALSE);
        obsoletePorts.parallelStream().forEach(portGUID -> {
            try {
                portHandler.removePort(userId, portGUID, portTypeName);
            } catch (InvalidParameterException error) {
                restExceptionHandler.captureInvalidParameterException(response, error);
            } catch (PropertyServerException error) {
                restExceptionHandler.capturePropertyServerException(response, error);
            } catch (UserNotAuthorizedException error) {
                restExceptionHandler.captureUserNotAuthorizedException(response, error);
            }
        });

    }


    private Set<String> createOrUpdatePortImplementations(String userId, String serverName, List<PortImplementation> portImplementations,
                                                          GUIDResponse response, String externalSourceName) {
        Set<String> portImplementationGUIDs = new HashSet<>();

        if (CollectionUtils.isNotEmpty(portImplementations)) {
            portImplementations.parallelStream().forEach(portImplementation ->
            {
                try {
                    portImplementationGUIDs.add(createOrUpdatePortImplementationWithSchemaType(userId, serverName, portImplementation,
                            externalSourceName));
                } catch (InvalidParameterException error) {
                    restExceptionHandler.captureInvalidParameterException(response, error);
                } catch (PropertyServerException error) {
                    restExceptionHandler.capturePropertyServerException(response, error);
                } catch (UserNotAuthorizedException error) {
                    restExceptionHandler.captureUserNotAuthorizedException(response, error);
                }
            });
        }

        return portImplementationGUIDs;
    }

    private Set<String> createOrUpdatePortAliases(String userId, String serverName, List<PortAlias> portAliases, GUIDResponse response,
                                                  String externalSourceName) {
        Set<String> portAliasGUIDs = new HashSet<>();

        if (CollectionUtils.isNotEmpty(portAliases)) {
            portAliases.parallelStream().forEach(portAlias -> {
                try {
                    portAliasGUIDs.add(createOrUpdatePortAliasWithDelegation(userId, serverName, portAlias, externalSourceName));
                } catch (InvalidParameterException error) {
                    restExceptionHandler.captureInvalidParameterException(response, error);
                } catch (PropertyServerException error) {
                    restExceptionHandler.capturePropertyServerException(response, error);
                } catch (UserNotAuthorizedException error) {
                    restExceptionHandler.captureUserNotAuthorizedException(response, error);
                }
            });
        }

        return portAliasGUIDs;
    }
}