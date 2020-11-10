/* SPDX-License-Identifier: Apache-2.0 */
/* Copyright Contributors to the ODPi Egeria project. */
package org.odpi.openmetadata.accessservices.dataengine.model;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;
import java.util.Objects;

import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.NONE;
import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.PUBLIC_ONLY;

@JsonAutoDetect(getterVisibility = PUBLIC_ONLY, setterVisibility = PUBLIC_ONLY, fieldVisibility = NONE)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class ProcessHierarchy implements Serializable {
    private String parentProcess;
    private String childProcess;

    @JsonProperty("containmentType")
    private ProcessContainmentType processContainmentType;

    public String getParentProcess() {
        return parentProcess;
    }

    public void setParentProcess(String parentProcess) {
        this.parentProcess = parentProcess;
    }

    public String getChildProcess() {
        return childProcess;
    }

    public void setChildProcess(String childProcess) {
        this.childProcess = childProcess;
    }

    public ProcessContainmentType getProcessContainmentType() {
        return processContainmentType;
    }

    public void setProcessContainmentType(ProcessContainmentType processContainmentType) {
        this.processContainmentType = processContainmentType;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ProcessHierarchy that = (ProcessHierarchy) o;
        return Objects.equals(parentProcess, that.parentProcess) &&
                Objects.equals(childProcess, that.childProcess) &&
                processContainmentType == that.processContainmentType;
    }

    @Override
    public int hashCode() {
        return Objects.hash(parentProcess, childProcess, processContainmentType);
    }

    @Override
    public String toString() {
        return "ProcessHierarchy{" +
                "parentProcess='" + parentProcess + '\'' +
                ", childProcess='" + childProcess + '\'' +
                ", containmentType=" + processContainmentType +
                '}';
    }
}
