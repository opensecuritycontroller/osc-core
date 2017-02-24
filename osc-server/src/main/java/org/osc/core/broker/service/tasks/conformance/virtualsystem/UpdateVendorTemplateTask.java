package org.osc.core.broker.service.tasks.conformance.virtualsystem;

import java.util.Set;

import org.apache.log4j.Logger;
import org.hibernate.Session;
import org.osc.core.broker.job.TaskInput;
import org.osc.core.broker.job.lock.LockObjectReference;
import org.osc.core.broker.model.entities.appliance.VirtualSystemPolicy;
import org.osc.core.broker.model.plugin.sdncontroller.VMwareSdnApiFactory;
import org.osc.core.broker.service.tasks.TransactionalTask;
import org.osc.sdk.sdn.api.VendorTemplateApi;

public class UpdateVendorTemplateTask extends TransactionalTask {
    private static final Logger LOG = Logger.getLogger(UpdateVendorTemplateTask.class);

    private VirtualSystemPolicy vsp;
    private String newPolicyName;

    public UpdateVendorTemplateTask(VirtualSystemPolicy vsp, String newPolicyName) {
        this.vsp = vsp;
        this.name = getName();
        this.newPolicyName = newPolicyName;
    }

    @TaskInput
    public String svcId;

    @Override
    public void executeTransaction(Session session) throws Exception {

        LOG.info("Start excecuting UpdateVendorTemplate Task");

        this.vsp = (VirtualSystemPolicy) session.get(VirtualSystemPolicy.class, this.vsp.getId());
        String templateId = this.vsp.getNsxVendorTemplateId();

        if(templateId != null && !templateId.isEmpty()) {
            VendorTemplateApi templateApi = VMwareSdnApiFactory.createVendorTemplateApi(this.vsp.getVirtualSystem());
            templateApi.updateVendorTemplate(
                    this.vsp.getVirtualSystem().getNsxServiceId(),
                    templateId,
                    this.newPolicyName,
                    this.vsp.getPolicy().getId().toString());
        }
    }

    @Override
    public String getName() {
        return "Updating Policy '" + this.vsp.getPolicy().getName() + "' in Virtual System '"
                + this.vsp.getVirtualSystem().getVirtualizationConnector().getName() + "'";
    }

    @Override
    public Set<LockObjectReference> getObjects() {
        return LockObjectReference.getObjectReferences(this.vsp.getVirtualSystem());
    }

}
