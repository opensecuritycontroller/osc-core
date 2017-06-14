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
package org.osc.core.broker.service.test;

import static org.junit.Assert.assertNotNull;

import javax.persistence.EntityManager;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.osc.core.broker.model.entities.ReleaseInfo;
import org.osc.core.broker.model.entities.RoleType;
import org.osc.core.broker.model.entities.User;
import org.osc.core.broker.model.entities.appliance.Appliance;
import org.osc.core.broker.model.entities.appliance.ApplianceSoftwareVersion;
import org.osc.core.broker.model.entities.appliance.DistributedAppliance;
import org.osc.core.broker.model.entities.appliance.DistributedApplianceInstance;
import org.osc.core.broker.model.entities.appliance.VirtualSystem;
import org.osc.core.broker.model.entities.appliance.VirtualizationType;
import org.osc.core.broker.model.entities.job.JobRecord;
import org.osc.core.broker.model.entities.job.JobStatus;
import org.osc.core.broker.model.entities.management.ApplianceManagerConnector;
import org.osc.core.broker.model.entities.management.Domain;
import org.osc.core.broker.model.entities.virtualization.VirtualizationConnector;
import org.osc.core.broker.model.plugin.manager.ManagerType;
import org.osc.core.broker.service.persistence.OSCEntityManager;
import org.osc.core.broker.util.TransactionalBroadcastUtil;
import org.osc.core.test.util.TestTransactionControl;

@RunWith(MockitoJUnitRunner.class)
public class ValidateDbCreate {

    @Mock(answer = Answers.CALLS_REAL_METHODS)
    private TestTransactionControl txControl;

    @Mock
    private TransactionalBroadcastUtil txBroadcastUtil;

    EntityManager em;

    @Before
    public void setUp() throws Exception {
        // initializing in-memory db
        this.em = InMemDB.init().createEntityManager();

        this.txControl.setEntityManager(this.em);
    }

    @Test
    public void testDatabaseSanityAndValidation() {
        // We must open a new transaction before doing anything with the DB
        this.txControl.required(() -> {

            addUserEntity(this.em);
            addReleaseInfoEntity(this.em);
            Appliance appliance = addApplianceEntity(this.em);
            ApplianceSoftwareVersion applianceSwVer = addApplianceSoftwareVersion(this.em, appliance);
            ApplianceManagerConnector applianceMgrCon = addApplianceManagerConnectorEntity(this.em);
            VirtualizationConnector virtualizationCon = addVirtualizationConnectorEntity(this.em);
            JobRecord jobRecord = addJobRecord(this.em);
            DistributedAppliance distributedAppliance = addDistributedApplianceEntity(this.em, appliance, applianceMgrCon, jobRecord);
            Domain domain = addDomainEntity(this.em, applianceMgrCon);
            VirtualSystem virtualSystem = addVirtualSystemEntity(this.em, applianceSwVer, virtualizationCon, distributedAppliance, domain);
            addDistributedApplianceInstanceEntity(this.em, virtualSystem);

            // We can now close the transaction and persist the changes
            return null;
        });
    }

    private void addDistributedApplianceInstanceEntity(EntityManager em, VirtualSystem virtualSystem) {
        DistributedApplianceInstance distributedApplianceInst = new DistributedApplianceInstance(virtualSystem);
        distributedApplianceInst.setIpAddress("123.4.5.7");
        distributedApplianceInst.setName("Agent1");

        OSCEntityManager.create(em, distributedApplianceInst, this.txBroadcastUtil);

        // retrieve back and validate
        distributedApplianceInst = em.find(DistributedApplianceInstance.class,
                distributedApplianceInst.getId());
        assertNotNull(distributedApplianceInst);
    }

    private VirtualSystem addVirtualSystemEntity(EntityManager em, ApplianceSoftwareVersion applianceSwVer, VirtualizationConnector virtualizationCon, DistributedAppliance distributedAppliance, Domain domain) {
        VirtualSystem virtualSystem = new VirtualSystem(distributedAppliance);
        virtualSystem.setDomain(domain);
        virtualSystem.setApplianceSoftwareVersion(applianceSwVer);
        virtualSystem.setVirtualizationConnector(virtualizationCon);

        OSCEntityManager.create(em, virtualSystem, this.txBroadcastUtil);

        // retrieve back and validate
        virtualSystem = em.find(VirtualSystem.class, virtualSystem.getId());
        assertNotNull(virtualSystem);
        return virtualSystem;
    }

    private Domain addDomainEntity(EntityManager em, ApplianceManagerConnector applianceMgrCon) {
        Domain domain = new Domain(applianceMgrCon);
        domain.setName("DC-1");
        domain.setMgrId("domain-id-3");

        OSCEntityManager.create(em, domain, this.txBroadcastUtil);

        // retrieve back and validate
        domain = em.find(Domain.class, domain.getId());
        assertNotNull(domain);
        return domain;
    }

    private JobRecord addJobRecord(EntityManager em){
        JobRecord jobRecord = new JobRecord();
        jobRecord.setName("testJob");
        OSCEntityManager.create(em, jobRecord, this.txBroadcastUtil);

        // retrieve back and validate
        jobRecord = em.find(JobRecord.class, jobRecord.getId());
        assertNotNull(jobRecord);
        return jobRecord;
    }

    private DistributedAppliance addDistributedApplianceEntity(EntityManager em, Appliance appliance, ApplianceManagerConnector applianceMgrCon, JobRecord jobRecord) {
        DistributedAppliance distributedAppliance = new DistributedAppliance(applianceMgrCon);
        distributedAppliance.setLastJob(jobRecord);
        distributedAppliance.getLastJob().setStatus(JobStatus.PASSED);
        distributedAppliance.setName("distributedappliance-1");
        distributedAppliance.setMgrSecretKey("secret-2");
        distributedAppliance.setAppliance(appliance);
        distributedAppliance.setApplianceVersion("1.0");

        OSCEntityManager.create(em, distributedAppliance, this.txBroadcastUtil);

        // retrieve back and validate
        distributedAppliance = em.find(DistributedAppliance.class, distributedAppliance.getId());
        assertNotNull(distributedAppliance);
        return distributedAppliance;
    }

    private VirtualizationConnector addVirtualizationConnectorEntity(EntityManager em) {
        VirtualizationConnector virtualizationCon = new VirtualizationConnector();

        virtualizationCon.setName("openstack-1");
        virtualizationCon.setControllerIpAddress("172.3.4.5");
        virtualizationCon.setControllerPassword("abc123");
        virtualizationCon.setControllerUsername("user1");
        virtualizationCon.setProviderIpAddress("123.4.5.6");
        virtualizationCon.setProviderUsername("nsmuser");
        virtualizationCon.setProviderPassword("abc2");
        virtualizationCon.setVirtualizationSoftwareVersion("12.3");
        virtualizationCon.setVirtualizationType(VirtualizationType.OPENSTACK);

        OSCEntityManager.create(em, virtualizationCon, this.txBroadcastUtil);

        // retrieve back and validate
        virtualizationCon = em.find(VirtualizationConnector.class, virtualizationCon.getId());
        assertNotNull(virtualizationCon);
        return virtualizationCon;
    }

    private ApplianceManagerConnector addApplianceManagerConnectorEntity(EntityManager em) {
        ApplianceManagerConnector applianceMgrCon = new ApplianceManagerConnector();

        applianceMgrCon.setName("nsm-1");
        applianceMgrCon.setManagerType(ManagerType.NSM.getValue());
        applianceMgrCon.setServiceType("IPS_IDS");
        applianceMgrCon.setUsername("admin");
        applianceMgrCon.setPassword("pass123");
        applianceMgrCon.setIpAddress("172.34.5.677");

        OSCEntityManager.create(em, applianceMgrCon, this.txBroadcastUtil);

        // retrieve back and validate
        applianceMgrCon = em.find(ApplianceManagerConnector.class, applianceMgrCon.getId());
        assertNotNull(applianceMgrCon);
        return applianceMgrCon;
    }

    private ApplianceSoftwareVersion addApplianceSoftwareVersion(EntityManager em, Appliance appliance) {
        ApplianceSoftwareVersion applianceSwVer = new ApplianceSoftwareVersion(appliance);

        applianceSwVer.setApplianceSoftwareVersion("sw_1.0");
        applianceSwVer.setImageUrl("http://imageUrl");
        applianceSwVer.setVirtualizarionSoftwareVersion("vir_1.0");
        applianceSwVer.setVirtualizationType(VirtualizationType.OPENSTACK);

        OSCEntityManager.create(em, applianceSwVer, this.txBroadcastUtil);

        // retrieve back and validate
        applianceSwVer = em.find(ApplianceSoftwareVersion.class, applianceSwVer.getId());
        assertNotNull(applianceSwVer);
        return applianceSwVer;
    }

    private Appliance addApplianceEntity(EntityManager em) {
        Appliance appliance = new Appliance();

        appliance.setManagerType(ManagerType.NSM.getValue());
        appliance.setModel("model-1");
        appliance.setManagerSoftwareVersion("1.2");

        OSCEntityManager.create(em, appliance, this.txBroadcastUtil);

        // retrieve back and validate
        appliance = em.find(Appliance.class, appliance.getId());
        assertNotNull(appliance);
        return appliance;
    }

    private void addReleaseInfoEntity(EntityManager em) {
        ReleaseInfo releaseInfo = new ReleaseInfo();

        releaseInfo.setDbVersion(0);
        OSCEntityManager.create(em, releaseInfo, this.txBroadcastUtil);

        // retrieve back and validate
        releaseInfo = em.find(ReleaseInfo.class, releaseInfo.getId());
        assertNotNull(releaseInfo);
    }

    private void addUserEntity(EntityManager em) {
        User user = new User();

        user.setFirstName("Trinh");
        user.setLastName("Lee");
        user.setLoginName("tlee");
        user.setRole(RoleType.ADMIN);
        user.setEmail("trinh_lee@dummy.com");

        OSCEntityManager.create(em, user, this.txBroadcastUtil);

        // retrieve back and validate
        user = em.find(User.class, user.getId());

        assertNotNull(user);
    }

    @After
    public void tearDown() {
        InMemDB.shutdown(); // clean up db caches and connection pools
    }
}
