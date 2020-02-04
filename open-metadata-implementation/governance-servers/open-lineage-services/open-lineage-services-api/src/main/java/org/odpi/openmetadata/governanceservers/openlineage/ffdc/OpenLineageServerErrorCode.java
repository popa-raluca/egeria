/* SPDX-License-Identifier: Apache-2.0 */
/* Copyright Contributors to the ODPi Egeria project. */
package org.odpi.openmetadata.governanceservers.openlineage.ffdc;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import org.odpi.openmetadata.repositoryservices.auditlog.OMRSAuditLogRecordSeverity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.MessageFormat;
import java.util.Arrays;

import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.NONE;
import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.PUBLIC_ONLY;

/**
 * The ODF error code is used to define first failure data capture (FFDC) for errors that occur when working with
 * Open Lineage Services.  It is used in conjunction with all ODF Exceptions, both Checked and Runtime (unchecked).
 * <p>
 * The 5 fields in the enum are:
 * <ul>
 * <li>HTTP Error Code for translating between REST and JAVA - Typically the numbers used are:</li>
 * <li><ul>
 * <li>500 - internal error</li>
 * <li>400 - invalid parameters</li>
 * <li>404 - not found</li>
 * <li>409 - data conflict errors - eg item already defined</li>
 * </ul></li>
 * <li>Error Message Id - to uniquely identify the message</li>
 * <li>Error Message Text - includes placeholder to allow additional values to be captured</li>
 * <li>SystemAction - describes the result of the error</li>
 * <li>UserAction - describes how a user should correct the error</li>
 * </ul>
 */
@JsonAutoDetect(getterVisibility = PUBLIC_ONLY, setterVisibility = PUBLIC_ONLY, fieldVisibility = NONE)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public enum OpenLineageServerErrorCode {

    NO_CONFIG_DOC(400, "OPEN-LINEAGE-SERVER-400-001 ",
            "Open Lineage server {0} does not have a configuration document.",
            "The server is not able to retrieve its configuration.  It fails to start.",
            "Add the Open Lineage configuration to the Open Lineage server's configuration document."),

    SERVICE_INSTANCE_FAILURE(400, "OPEN-LINEAGE-SERVER-400-005 ",
            "The open lineage  services are unable to initialize a new instance of open lineage server {0};" +
                    " error message is {1}",
            "The open lineage  services detected an error during the start up of a specific open lineage server " +
                    "instance.  Its open lineage services are not available for the server.",
            "Review the error message and any other reported failures to determine the cause of the problem.  " +
                    "Once this is resolved, restart the server."),

    CANNOT_OPEN_GRAPH_DB(400, "OPEN-LINEAGE-SERVER-400-006 ",
            "It is not possible to open the graph database at path {0} in the {1} method of {2} class.",
            "Graph could not be opened due to invalid configuration.",
            "Please check that the graph database exists and is not in use by another process."),

    ERROR_INITIALIZING_BUFFER_GRAPH_CONNECTOR_DB(400, "OPEN-LINEAGE-SERVER-400-007 ",
            "The Open Lineage server {0} is not able to initialize the Buffergraph database connector.",
            "The Buffergraph database connector could not be initialized.",
            "Please check that the Buffergraph database exists and is not in use by another process, and verify the Open Lineage Services configuration."),

    ERROR_INITIALIZING_MAIN_GRAPH_CONNECTOR_DB(400, "OPEN-LINEAGE-SERVER-400-008 ",
            "The Open Lineage server {0} is not able to initialize the Maingraph database connector.",
            "The Maingraph database connector could not be initialized.",
            "Please check that the Maingraph database exists and is not in use by another process, and verify the Open Lineage Services configuration."),

    ERROR_STARTING_BUFFER_GRAPH_CONNECTOR(400, "OPEN-LINEAGE-SERVER-400-009 ",
            "The Open Lineage server {0} is not able to register the Buffergraph database connector as \"active\".",
            "The Buffergraph database connector could not be started.",
            "Please check that the Buffergraph database exists and is not in use by another process, and verify the Open Lineage Services configuration."),

    ERROR_STARTING_MAIN_GRAPH_CONNECTOR(400, "OPEN-LINEAGE-SERVER-400-010 ",
            "The Open Lineage server {0} is not able to register the Maingraph database connector as \"active\" .",
            "The Maingraph database connector could not be started.",
            "Please check that the Maingraph database exists and is not in use by another process, and verify the Open Lineage Services configuration."),


    ERROR_OBTAINING_IN_TOPIC_CONNECTOR(400, "OPEN-LINEAGE-SERVER-400-011 ",
            "The Open Lineage Services server {0} is unable to obtain an in topic connector.",
            "The in topic connector could not be obtained.",
            "Review the topic name set by the Open Lineage Services configuration."),

    ERROR_OBTAINING_BUFFER_GRAPH_CONNECTOR(400, "OPEN-LINEAGE-SERVER-400-012 ",
            "The Open Lineage Services server {0} is not able to obtain a Buffergraph database connector.",
            "The Buffergraph database connector could not be obtained.",
            "Please verify the Buffergraph connection object within the Open Lineage Services configuration."),

    ERROR_OBTAINING_MAIN_GRAPH_CONNECTOR(400, "OPEN-LINEAGE-SERVER-400-012 ",
            "The Open Lineage Services server {0} is not able to obtain a Maingraph database connector.",
            "The Maingraph database connector could not be obtained.",
            "Please verify the Maingraph connection object within the Open Lineage Services configuration."),

    ERROR_STARTING_IN_TOPIC_CONNECTOR(400, "OPEN-LINEAGE-SERVER-400-013 ",
            "The Open Lineage Services server {0} is unable to start an in topic listener.",
            "The topic connector could not be started.",
            "Review the status of the eventbus server and review the topic name set by the Open Lineage Services configuration."),

    NODE_NOT_FOUND(404, "OPEN-LINEAGE-SERVICES-404-001 ",
            "Error retrieving queried node.",
            "The queried node was not found in the lineage graph.",
            "Please verify the queried GUID."),

    GRAPH_INITIALIZATION_ERROR(500, "OPEN-LINEAGE-SERVICES-500-001 ",
            "The graph database could not be initialized for open metadata repository {0}.",
            "The system was unable to initialize.",
            "Please raise a github issue."),

    OPEN_LINEAGE_HANDLER_NOT_INSTANTIATED(500, "OPEN-LINEAGE-SERVER-500-002 ",
            "The OpenLineageHandler {0} has not been instantiated in the open lineage server {1}",
            "The OpenLineageHandler has not been instantiated in the open lineage server.",
            "the open lineage server. Once the cause is resolved, retry the open lineage request."),


    LINEAGE_CYCLE(503, "OPEN-LINEAGE-SERVICES-503-001 ",
            "A possible cycle in the lineage graph has been detected.",
            "No nodes were returned by the lineage query. This could mean that the ultimate sources/destinations of the " +
                    "queried node are included in a cyclic data flow. This is not supported by the Open lineage Services.",
            "Query the full end to end lineage of the queried node to identify the problematic data flow cycle."),

    ERROR_INITIALIZING_OLS(503, "OPEN-LINEAGE-SERVICES-503-002 ",
            "The Open Lineage Services server {0} encountered an unexpected error and could not start.",
            "An unexpected error occurred while initializing the Open Lineage Services.",
            "Please contact an Egeria maintainer about your issue."),
    ;



    private int httpErrorCode;
    private String errorMessageId;
    private String errorMessage;
    private String systemAction;
    private String userAction;

    private static final Logger log = LoggerFactory.getLogger(OpenLineageServerErrorCode.class);


    /**
     * The constructor for ODFErrorCode expects to be passed one of the enumeration rows defined in
     * ODFErrorCode above.   For example:
     * <p>
     * ODFErrorCode   errorCode = ODFErrorCode.UNKNOWN_ENDPOINT;
     * <p>
     * This will expand out to the 5 parameters shown below.
     *
     * @param httpErrorCode  error code to use over REST calls
     * @param errorMessageId unique Id for the message
     * @param errorMessage   text for the message
     * @param systemAction   description of the action taken by the system when the error condition happened
     * @param userAction     instructions for resolving the error
     */
    OpenLineageServerErrorCode(int httpErrorCode, String errorMessageId, String errorMessage, String systemAction, String userAction) {
        this.httpErrorCode = httpErrorCode;
        this.errorMessageId = errorMessageId;
        this.errorMessage = errorMessage;
        this.systemAction = systemAction;
        this.userAction = userAction;
    }


    public int getHTTPErrorCode() {
        return httpErrorCode;
    }


    /**
     * Returns the unique identifier for the error message.
     *
     * @return errorMessageId
     */
    public String getErrorMessageId() {
        return errorMessageId;
    }


    /**
     * Returns the error message with the placeholders filled out with the supplied parameters.
     *
     * @param params strings that plug into the placeholders in the errorMessage
     * @return errorMessage (formatted with supplied parameters)
     */
    public String getFormattedErrorMessage(String... params) {
        MessageFormat mf = new MessageFormat(errorMessage);
        String result = mf.format(params);

        log.debug(String.format("ODFErrorCode.getMessage(%s): %s", Arrays.toString(params), result));

        return result;
    }


    /**
     * Returns a description of the action taken by the system when the condition that caused this exception was
     * detected.
     *
     * @return systemAction
     */
    public String getSystemAction() {
        return systemAction;
    }


    /**
     * Returns instructions of how to resolve the issue reported in this exception.
     *
     * @return userAction
     */
    public String getUserAction() {
        return userAction;
    }
}
