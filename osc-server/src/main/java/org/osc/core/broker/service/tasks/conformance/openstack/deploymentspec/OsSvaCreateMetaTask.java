package org.osc.core.broker.service.tasks.conformance.openstack.deploymentspec;

import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.hibernate.Session;
import org.osc.core.broker.job.TaskGraph;
import org.osc.core.broker.job.lock.LockObjectReference;
import org.osc.core.broker.model.entities.appliance.AgentType;
import org.osc.core.broker.model.entities.appliance.DistributedApplianceInstance;
import org.osc.core.broker.model.entities.virtualization.openstack.DeploymentSpec;
import org.osc.core.broker.model.plugin.manager.ManagerApiFactory;
import org.osc.core.broker.rest.server.AgentAuthFilter;
import org.osc.core.broker.service.persistence.EntityManager;
import org.osc.core.broker.service.tasks.TransactionalMetaTask;
import org.osc.core.util.EncryptionUtil;
import org.osc.sdk.manager.api.ApplianceManagerApi;

/**
 * Creates an SVA on openstack
 */
class OsSvaCreateMetaTask extends TransactionalMetaTask {

    final Logger log = Logger.getLogger(OsSvaCreateMetaTask.class);

    private TaskGraph tg;

    private DeploymentSpec ds;
    private DistributedApplianceInstance dai;
    private String availabilityZone;
    private final String hypervisorHostName;

    /**
     * Constructor. All arguments are required(unless specified)
     *
     * @param ds
     *            deployment spec
     * @param hypervisorName
     *            the hypervisor to deploy to
     * @param availabilityZone
     *            the availability zone to deploy to
     */
    public OsSvaCreateMetaTask(DeploymentSpec ds, String hypervisorHostName, String availabilityZone) {
        this.availabilityZone = availabilityZone;
        this.ds = ds;
        this.hypervisorHostName = hypervisorHostName;
        this.dai = null;
    }

    public OsSvaCreateMetaTask(DistributedApplianceInstance dai) {
        this.dai = dai;
        this.ds = dai.getDeploymentSpec();
        this.availabilityZone = dai.getOsAvailabilityZone();
        this.hypervisorHostName = this.dai.getOsHostName();
    }

    @Override
    public void executeTransaction(Session session) throws Exception {
        this.tg = new TaskGraph();

        this.ds = (DeploymentSpec) session.get(DeploymentSpec.class, this.ds.getId());

        this.dai = getDAI(session, this.ds, this.dai);

        // Reset all discovered attributes
        this.dai.resetAllDiscoveredAttributes();
        // Recalculate AZ for this host. It is possible our DB contains a staled AZ.
        this.dai.setOsAvailabilityZone(OpenstackUtil.getHostAvailibilityZone(this.ds, this.ds.getRegion(),
                this.dai.getOsHostName()));
        this.availabilityZone = this.dai.getOsAvailabilityZone();

        EntityManager.update(session, this.dai);

        this.tg.addTask(new OsSvaServerCreateTask(this.dai, this.hypervisorHostName, this.availabilityZone));
        this.tg.appendTask(new OsSvaEnsureActiveTask(this.dai));
        if (!StringUtils.isBlank(this.ds.getFloatingIpPoolName())) {
            this.tg.appendTask(new OsSvaCheckFloatingIpTask(this.dai));
        }
        OpenstackUtil.scheduleSecurityGroupJobsRelatedToDai(session, this.dai, this);
    }

    /**
     * Gets a DAI based on the dai id passed in. If the passed in id is null, creates a new DAI.
     *
     * @param session
     *            the session
     * @param vs
     *            the vs to associate the DAI(newly created) with
     * @param ds
     *            the ds to associate the dai(newly created) with
     * @param daiIdToLoad
     *            the dai to load
     * @return
     * @throws Exception
     */
    private DistributedApplianceInstance getDAI(Session session, DeploymentSpec ds,
            DistributedApplianceInstance daiToLoad) throws Exception {
        DistributedApplianceInstance dai = null;

        if (daiToLoad != null) {
            dai = (DistributedApplianceInstance) session.load(DistributedApplianceInstance.class, daiToLoad.getId());
        } else {
            String daiName;
            ApplianceManagerApi managerApi = ManagerApiFactory.createApplianceManagerApi(ds.getVirtualSystem());
            AgentType agentType = managerApi.isAgentManaged() ? AgentType.AGENT : AgentType.AGENTLESS;
            dai = new DistributedApplianceInstance(ds.getVirtualSystem(), agentType);
            dai.setOsHostName(this.hypervisorHostName);
            dai.setOsAvailabilityZone(this.availabilityZone);
            dai.setDeploymentSpec(ds);

            //dai.setName("Temporary" + UUID.randomUUID().toString()); :TODO sridhar
            dai.setName("Temporary"); // setting temporary name since it is mandatory field
            dai = EntityManager.create(session, dai);

            // Generate a unique, intuitive and immutable name
            daiName = ds.getVirtualSystem().getName() + "-" + dai.getId().toString();
            this.log.info("Creating DAI using name '" + daiName + "'");

            dai.setName(daiName);
            dai.setPolicyMapOutOfSync(true);
            dai.setPassword(EncryptionUtil.encryptAESCTR(AgentAuthFilter.VMIDC_AGENT_PASS));

            EntityManager.update(session, dai);
            this.log.info("Creating new DAI " + dai);
        }
        return dai;
    }

    @Override
    public String getName() {
        return String.format("Create SVA for '%s' in Availability zone '%s' in Region '%s'", this.hypervisorHostName,
                this.availabilityZone, this.ds.getRegion());
    }

    @Override
    public TaskGraph getTaskGraph() {
        return this.tg;
    }

    @Override
    public Set<LockObjectReference> getObjects() {
        return LockObjectReference.getObjectReferences(this.ds);
    }

}
