package org.osc.core.broker.service.tasks.conformance.manager;

import java.util.ArrayList;
import java.util.List;

import org.mockito.Mockito;
import org.osc.core.broker.job.TaskGraph;
import org.osc.core.broker.model.entities.appliance.DistributedApplianceInstance;
import org.osc.core.broker.model.entities.appliance.VirtualSystem;
import org.osc.core.broker.model.entities.virtualization.VirtualizationConnector;
import org.osc.sdk.manager.element.ManagerDeviceMemberElement;

public class MgrCheckDevicesMetaTaskTestData {
    public static List<VirtualSystem> TEST_VIRTUAL_SYSTEMS = new ArrayList<VirtualSystem>();

    public static ManagerDeviceMemberElement MGR_DEVICE_MEMBER_ELEMENT_NO_DAI = createManagerDeviceMember(
            "MGR_DEVICE_MEMBER_ELEMENT_NO_DAI");
    public static ManagerDeviceMemberElement MGR_DEVICE_MEMBER_ELEMENT_WITH_DAI = createManagerDeviceMember(
            "MGR_DEVICE_MEMBER_ELEMENT_WITH_DAI");

    public static DistributedApplianceInstance DISTRIBUTED_APPLIANCE_INSTANCE_WITH_MANAGER_ID = createDistributedApplianceInstance(
            "DISTRIBUTED_APPIANCE_INSTANCE_WITH_MANAGER_ID", "MGR_DEVICE_ID", null);
    public static DistributedApplianceInstance DISTRIBUTED_APPLIANCE_INSTANCE_WITH_IP_ADDRESS = createDistributedApplianceInstance(
            "DISTRIBUTED_APPIANCE_INSTANCE_WITH_IP_ADDRESS", null, "1.1.1.1");
    public static DistributedApplianceInstance DISTRIBUTED_APPIANCE_INSTANCE_WITH_NO_MANAGER_AND_NO_IP_ADDRESS = createDistributedApplianceInstance(
            "DISTRIBUTED_APPIANCE_INSTANCE_WITH_NO_MANAGER_AND_NO_IP_ADDRESS", null, null);

    public static VirtualSystem MANAGER_ID_PRESENT_NO_DAI_DEVICE_VS = createVirtualSystemWithManager(101L, "MGR_ID");
    public static VirtualSystem MANAGER_ID_NOT_PRESENT_VS = createVirtualSystem(102L);
    public static VirtualSystem MANAGER_DEVICE_ID_PRESENT_VS = createVirtualSystemWithDAIManagerDevice(103L);
    public static VirtualSystem DAI_IP_PRESENT_VS = createVirtualSystemWithDAIIP(104L);
    public static VirtualSystem MANAGER_DEVICE_ID_AND_DAI_IP_NOT_PRESENT_VS = createVirtualSystemWithDaiNoManagerDeviceAndNoIP(
            105L);
    public static VirtualSystem DEVICE_GROUP_NOT_SUPPORTED_VS = createVirtualSystemWithManager(106L,
            "MGR_DEVICE_GROUP_NOT_SUPPORTED_VS_ID");
    public static VirtualSystem MANAGER_ID_AND_DAI_DEVICE_PRESENT_VS = createVirtualSystemWithManager(107L, "MGR_ID");

    public static TaskGraph updateVSSDeviceAndDeleteMemberGraph(VirtualSystem vs) {
        TaskGraph expectedGraph = new TaskGraph();
        expectedGraph.appendTask(new MgrUpdateVSSDeviceTask(vs));
        expectedGraph.appendTask(new MgrDeleteMemberDeviceTask(vs, MGR_DEVICE_MEMBER_ELEMENT_NO_DAI));
        return expectedGraph;
    }

    public static TaskGraph createVSSDeviceGraph(VirtualSystem vs) {
        TaskGraph expectedGraph = new TaskGraph();
        expectedGraph.appendTask(new MgrCreateVSSDeviceTask(vs));
        return expectedGraph;
    }

    public static TaskGraph updateVSSDeviceGraph(VirtualSystem vs) {
        TaskGraph expectedGraph = new TaskGraph();
        expectedGraph.appendTask(new MgrUpdateVSSDeviceTask(vs));
        return expectedGraph;
    }

    public static TaskGraph mgrCreateVSSDeviceAndUpdateMemberGraph(VirtualSystem vs) {
        TaskGraph expectedGraph = new TaskGraph();
        expectedGraph.appendTask(new MgrCreateVSSDeviceTask(vs));
        expectedGraph.appendTask(new MgrUpdateMemberDeviceTask(DISTRIBUTED_APPLIANCE_INSTANCE_WITH_MANAGER_ID));
        return expectedGraph;
    }

    public static TaskGraph mgrCreateVSSDeviceAndCreateMemberGraph(VirtualSystem vs) {
        TaskGraph expectedGraph = new TaskGraph();
        expectedGraph.appendTask(new MgrCreateVSSDeviceTask(vs));
        expectedGraph.appendTask(new MgrCreateMemberDeviceTask(DISTRIBUTED_APPLIANCE_INSTANCE_WITH_IP_ADDRESS));
        return expectedGraph;
    }

    public static TaskGraph updateDAISManagerDeviceIdGraph(VirtualSystem vs) {
        TaskGraph expectedGraph = new TaskGraph();
        expectedGraph.appendTask(new UpdateDAISManagerDeviceId(vs));
        return expectedGraph;
    }

    private static ManagerDeviceMemberElement createManagerDeviceMember(String name) {
        ManagerDeviceMemberElement managerDeviceMember = Mockito.mock(ManagerDeviceMemberElement.class);
        Mockito.doReturn(name).when(managerDeviceMember).getName();
        return managerDeviceMember;
    }

    private static DistributedApplianceInstance createDistributedApplianceInstance(String daiName, String mgrDeviceId,
            String ipAddress) {
        DistributedApplianceInstance dai = new DistributedApplianceInstance();
        dai.setName(daiName);
        dai.setMgrDeviceId(mgrDeviceId);
        dai.setIpAddress(ipAddress);
        return dai;
    }

    private static VirtualSystem createBaseVirtualSystem(Long vsId) {
        VirtualizationConnector vc = new VirtualizationConnector();
        vc.setName("vc_name");
        VirtualSystem vs = new VirtualSystem();
        vs.setId(vsId);
        vs.setVirtualizationConnector(vc);

        return vs;
    }

    private static VirtualSystem createVirtualSystem(Long vsId) {
        VirtualSystem vs = createBaseVirtualSystem(vsId);

        TEST_VIRTUAL_SYSTEMS.add(vs);
        return vs;
    }

    private static VirtualSystem createVirtualSystemWithManager(Long vsId, String mgrId) {
        VirtualSystem vs = createBaseVirtualSystem(vsId);

        vs.setMgrId(mgrId);

        TEST_VIRTUAL_SYSTEMS.add(vs);
        return vs;
    }

    private static VirtualSystem createVirtualSystemWithDAIManagerDevice(Long vsId) {
        VirtualSystem vs = createBaseVirtualSystem(vsId);

        vs.addDistributedApplianceInstance(DISTRIBUTED_APPLIANCE_INSTANCE_WITH_MANAGER_ID);

        TEST_VIRTUAL_SYSTEMS.add(vs);
        return vs;
    }

    private static VirtualSystem createVirtualSystemWithDAIIP(Long vsId) {
        VirtualSystem vs = createBaseVirtualSystem(vsId);

        vs.addDistributedApplianceInstance(DISTRIBUTED_APPLIANCE_INSTANCE_WITH_IP_ADDRESS);

        TEST_VIRTUAL_SYSTEMS.add(vs);
        return vs;
    }

    private static VirtualSystem createVirtualSystemWithDaiNoManagerDeviceAndNoIP(Long vsId) {
        VirtualSystem vs = createBaseVirtualSystem(vsId);

        vs.addDistributedApplianceInstance(DISTRIBUTED_APPIANCE_INSTANCE_WITH_NO_MANAGER_AND_NO_IP_ADDRESS);

        TEST_VIRTUAL_SYSTEMS.add(vs);
        return vs;
    }

}
