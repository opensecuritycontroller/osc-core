/*******************************************************************************
 * Copyright (c) Intel Corporation
 * Copyright (c) 2017
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package org.osc.core.broker.service.tasks.conformance.openstack.securitygroup;

import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import org.apache.commons.lang.RandomStringUtils;
import org.osc.core.broker.job.TaskGraph;
import org.osc.core.broker.model.entities.virtualization.SecurityGroup;
import org.osc.core.broker.model.entities.virtualization.SecurityGroupMember;
import org.osc.core.broker.model.entities.virtualization.SecurityGroupMemberType;
import org.osc.core.broker.model.entities.virtualization.ServiceFunctionChain;
import org.osc.core.broker.model.entities.virtualization.VirtualizationConnector;
import org.osc.core.broker.model.entities.virtualization.openstack.VM;
import org.osc.core.broker.model.entities.virtualization.openstack.VMPort;
import org.osc.core.broker.model.plugin.ApiFactoryService;
import org.osc.core.broker.service.tasks.conformance.openstack.sfc.SfcFlowClassifierCreateTask;
import org.osc.core.broker.service.tasks.conformance.openstack.sfc.SfcFlowClassifierDeleteTask;
import org.osc.core.broker.service.tasks.conformance.openstack.sfc.SfcFlowClassifierUpdateTask;
import org.osc.sdk.controller.api.SdnRedirectionApi;
import org.osc.sdk.controller.element.InspectionHookElement;
import org.osc.sdk.controller.element.InspectionPortElement;

class SecurityGroupMemberHookCheckTaskTestData {
    private static final String OPENSTACK_SFC_ID = "openstack_sfc_id";

    public static final long SGM_ID = 1L;

    public static final String OPENSTACK_PORT_CLASSIFIER = "openstack_port_classifier";

    public static final String NEUTRON_SFC_CONTROLLER = "Neutron-SFC";

    private ApiFactoryService apiFactoryServiceMock = mock(ApiFactoryService.class);
    private SdnRedirectionApi redirApiMock = mock(SdnRedirectionApi.class);

    public static SecurityGroupMember SFC_SGM_WITH_NO_PORTS = createBindedSfcSecurityGroupMember(new HashSet<>());
    public static SecurityGroupMember SFC_SGM_WITH_PORTS = createBindedSfcSecurityGroupMember(getSfcPorts(false));
    public static SecurityGroupMember SFC_SGM_WITH_DELETED_PORTS = createBindedSfcSecurityGroupMember(
            getSfcPorts(true));
    public static SecurityGroupMember SFC_SGM_WITH_MIXED_PORTS = createBindedSfcSecurityGroupMember(
            getMixedMarkedForDeletedSfcPorts());
    public static SecurityGroupMember SFC_SGM_WITH_PORTS_SFC_MISMATCH = createBindedSfcSecurityGroupMember(
            getSfcPorts(false));

    public static SecurityGroupMember SFC_SGM_UNBINDED_WITH_PORTS = createUnBindedSfcSecurityGroupMember(
            getSfcPorts(false));

    public static SecurityGroupMember SFC_SGM_UNBINDED_WITH_PORTS_UNBINDED = createUnBindedSfcSecurityGroupMember(
            getSfcPorts(false, false));

    public TaskGraph getSfcWithPortsTaskGraph(SecurityGroupMember sgm) {
        TaskGraph expectedGraph = new TaskGraph();

        try {
            initializeApiFactory(sgm);
            when(this.redirApiMock.getInspectionHook(any())).thenReturn(null);
        } catch (Exception e) {
            throw new RuntimeException();
        }
        for (VMPort port : sgm.getVmPorts()) {
            expectedGraph.appendTask(new SfcFlowClassifierCreateTask().create(sgm.getSecurityGroup(), port));
        }
        return expectedGraph;
    }

    public TaskGraph getSfcWithDeletedPortsTaskGraph(SecurityGroupMember sgm) {
        TaskGraph expectedGraph = new TaskGraph();

        initializeApiFactory(sgm);

        for (VMPort port : sgm.getVmPorts()) {
            expectedGraph.appendTask(new SfcFlowClassifierDeleteTask().create(sgm.getSecurityGroup(), port));
            expectedGraph.appendTask(new VmPortDeleteFromDbTask().create(sgm, port));
        }
        return expectedGraph;
    }

    public TaskGraph getSfcWithMixedPortsTaskGraph(SecurityGroupMember sgm) {
        TaskGraph expectedGraph = new TaskGraph();

        initializeApiFactory(sgm);

        for (VMPort port : sgm.getVmPorts()) {
            if (port.getMarkedForDeletion()) {
                expectedGraph.appendTask(new SfcFlowClassifierDeleteTask().create(sgm.getSecurityGroup(), port));
                expectedGraph.appendTask(new VmPortDeleteFromDbTask().create(sgm, port));
            } else {
                expectedGraph.appendTask(new SfcFlowClassifierCreateTask().create(sgm.getSecurityGroup(), port));
            }
        }
        return expectedGraph;
    }

    public TaskGraph getSfcMismatchTaskGraph(SecurityGroupMember sgm) {
        TaskGraph expectedGraph = new TaskGraph();

        try {
            initializeApiFactory(sgm);

            InspectionHookElement mockInspectionHook = mock(InspectionHookElement.class);
            when(mockInspectionHook.getInspectionPort()).thenReturn(mock(InspectionPortElement.class));
            when(mockInspectionHook.getInspectionPort().getElementId()).thenReturn("foo");
            when(this.redirApiMock.getInspectionHook(any())).thenReturn(mockInspectionHook);
        } catch (Exception e) {
            throw new RuntimeException();
        }
        for (VMPort port : sgm.getVmPorts()) {
            expectedGraph.appendTask(new SfcFlowClassifierUpdateTask().create(sgm.getSecurityGroup(), port));
        }
        return expectedGraph;
    }

    public TaskGraph getSfcUnbindedTaskGraph(SecurityGroupMember sgm) {
        TaskGraph expectedGraph = new TaskGraph();

        initializeApiFactory(sgm);

        for (VMPort port : sgm.getVmPorts()) {
            expectedGraph.appendTask(new SfcFlowClassifierDeleteTask().create(sgm.getSecurityGroup(), port));
        }
        return expectedGraph;
    }

    public ApiFactoryService getApiFactoryServiceMock() {
        return this.apiFactoryServiceMock;
    }

    private void initializeApiFactory(SecurityGroupMember sgm) {
        try {
            when(this.apiFactoryServiceMock.supportsNeutronSFC(sgm.getSecurityGroup())).thenReturn(true);
            when(this.apiFactoryServiceMock
                    .createNetworkRedirectionApi(sgm.getSecurityGroup().getVirtualizationConnector()))
                            .thenReturn(this.redirApiMock);
        } catch (Exception e) {
            throw new RuntimeException();
        }
    }

    private static SecurityGroupMember createBindedSfcSecurityGroupMember(Set<VMPort> ports) {
        SecurityGroupMember sfcSecurityGroupMember = createUnBindedSfcSecurityGroupMember(ports);
        when(sfcSecurityGroupMember.getSecurityGroup().getNetworkElementId()).thenReturn(OPENSTACK_SFC_ID);
        return sfcSecurityGroupMember;
    }

    private static SecurityGroupMember createUnBindedSfcSecurityGroupMember(Set<VMPort> ports) {
        VirtualizationConnector vc = mock(VirtualizationConnector.class);
        when(vc.getControllerType()).thenReturn(NEUTRON_SFC_CONTROLLER);

        SecurityGroup sg = mock(SecurityGroup.class);
        when(sg.getVirtualizationConnector()).thenReturn(vc);
        when(sg.getServiceFunctionChain()).thenReturn(mock(ServiceFunctionChain.class));

        SecurityGroupMember sgm = mock(SecurityGroupMember.class);
        when(sgm.getSecurityGroup()).thenReturn(sg);
        when(sgm.getType()).thenReturn(SecurityGroupMemberType.values()[new Random().nextInt(2)]);
        when(sgm.getMemberName()).thenReturn("member_" + RandomStringUtils.random(5));
        when(sgm.getVmPorts()).thenReturn(ports);
        when(sgm.getId()).thenReturn(SGM_ID);

        return sgm;
    }

    private static Set<VMPort> getSfcPorts(boolean markDeleted) {
        return getSfcPorts(markDeleted, true);
    }

    private static Set<VMPort> getSfcPorts(boolean markDeleted, boolean addFlowClassifier) {
        Set<VMPort> ports = new HashSet<>();
        for (int i = 0; i < 2; i++) {
            VMPort mockPort = mock(VMPort.class);
            when(mockPort.getVm()).thenReturn(mock(VM.class));
            when(mockPort.getMarkedForDeletion()).thenReturn(markDeleted);
            if (addFlowClassifier) {
                when(mockPort.getInspectionHookId()).thenReturn(OPENSTACK_PORT_CLASSIFIER + i);
            }
            ports.add(mockPort);
        }
        return ports;
    }

    /**
     * First 2 ports will be normal ports, second 2 ports will be marked for deletion
     *
     * @return
     */
    private static Set<VMPort> getMixedMarkedForDeletedSfcPorts() {
        Set<VMPort> ports = getSfcPorts(false);
        ports.addAll(getSfcPorts(true));

        return ports;
    }

}
