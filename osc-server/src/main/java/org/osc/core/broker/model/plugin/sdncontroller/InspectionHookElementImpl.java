package org.osc.core.broker.model.plugin.sdncontroller;

import org.osc.sdk.controller.FailurePolicyType;
import org.osc.sdk.controller.TagEncapsulationType;
import org.osc.sdk.controller.element.InspectionHookElement;
import org.osc.sdk.controller.element.InspectionPortElement;
import org.osc.sdk.controller.element.NetworkElement;

public class InspectionHookElementImpl implements InspectionHookElement {
    String id;
    Long tag;
    Long order;
    TagEncapsulationType encapsulationType;
    FailurePolicyType failurePolicyType;
    NetworkElement inspectedPort;
    InspectionPortElement inspectionPort;

    public InspectionHookElementImpl(
            String id,
            NetworkElement inspectedPort,
            InspectionPortElement inspectionPort,
            Long tag,
            TagEncapsulationType encapsulationType,
            Long order,
            FailurePolicyType failurePolicyType) {
        this.id = id;
        this.tag = tag;
        this.order = order;
        this.encapsulationType = encapsulationType;
        this.failurePolicyType = failurePolicyType;
        this.inspectedPort = inspectedPort;
        this.inspectionPort = inspectionPort;
    }

    @Override
    public String getHookId() {
        return this.id;
    }

    @Override
    public Long getTag() {
        return this.tag;
    }

    @Override
    public Long getOrder() {
        return this.order;
    }

    @Override
    public TagEncapsulationType getEncType() {
        return this.encapsulationType;
    }

    @Override
    public FailurePolicyType getFailurePolicyType() {
        return this.failurePolicyType;
    }

    @Override
    public NetworkElement getInspectedPort() {
        return this.inspectedPort;
    }

    @Override
    public InspectionPortElement getInspectionPort() {
        return this.inspectionPort;
    }

}
