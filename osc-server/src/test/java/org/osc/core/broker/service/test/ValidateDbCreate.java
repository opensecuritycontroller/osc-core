package org.osc.core.broker.service.test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.osc.core.broker.job.JobStatus;
import org.osc.core.broker.model.entities.ReleaseInfo;
import org.osc.core.broker.model.entities.RoleType;
import org.osc.core.broker.model.entities.User;
import org.osc.core.broker.model.entities.appliance.AgentType;
import org.osc.core.broker.model.entities.appliance.Appliance;
import org.osc.core.broker.model.entities.appliance.ApplianceSoftwareVersion;
import org.osc.core.broker.model.entities.appliance.DistributedAppliance;
import org.osc.core.broker.model.entities.appliance.DistributedApplianceInstance;
import org.osc.core.broker.model.entities.appliance.VirtualSystem;
import org.osc.core.broker.model.entities.job.JobRecord;
import org.osc.core.broker.model.entities.management.ApplianceManagerConnector;
import org.osc.core.broker.model.entities.management.Domain;
import org.osc.core.broker.model.entities.virtualization.VirtualizationConnector;
import org.osc.core.broker.model.plugin.manager.ManagerType;
import org.osc.core.broker.model.virtualization.VirtualizationType;
import org.osc.core.broker.service.persistence.EntityManager;

public class ValidateDbCreate {

    @Before
    public void setUp() {
        // initializing in-memory db
        InMemDB.init();
    }

    @Test
    public void testDatabaseSanityAndValidation() {
        Session session = null;
        Transaction tx = null;

        try {
            SessionFactory sessionFactory = InMemDB.getSessionFactory();
            session = sessionFactory.openSession();

            // We must open a new transaction before doing anything with the DB
            tx = session.beginTransaction();

            addUserEntity(session);
            addReleaseInfoEntity(session);
            Appliance appliance = addApplianceEntity(session);
            ApplianceSoftwareVersion applianceSwVer = addApplianceSoftwareVersion(session, appliance);
            ApplianceManagerConnector applianceMgrCon = addApplianceManagerConnectorEntity(session);
            VirtualizationConnector virtualizationCon = addVirtualizationConnectorEntity(session);
            JobRecord jobRecord = addJobRecord(session);
            DistributedAppliance distributedAppliance = addDistributedApplianceEntity(session, appliance, applianceMgrCon, jobRecord);
            Domain domain = addDomainEntity(session, applianceMgrCon);
            VirtualSystem virtualSystem = addVirtualSystemEntity(session, applianceSwVer, virtualizationCon, distributedAppliance, domain);
            addDistributedApplianceInstanceEntity(session, virtualSystem);

            // We can now close the transaction and persist the changes
            tx.commit();

        } catch (RuntimeException re) {
            if (tx != null && tx.isActive()) {
                try {
                    // Second try catch as the rollback could fail as well
                    tx.rollback();
                } catch (HibernateException he) {
                    fail("db validation test fails with transaction rollback exception: " + he);
                }
            }
            fail("db validation test fails with runtime exception: " + re);
        } catch (Exception ex) {
            fail("db validation test fails with exception: " + ex);
        } finally {
            session.close();
        }
    }

    private void addDistributedApplianceInstanceEntity(Session session, VirtualSystem virtualSystem) {
        DistributedApplianceInstance distributedApplianceInst = new DistributedApplianceInstance(virtualSystem, AgentType.AGENT);
        distributedApplianceInst.setIpAddress("123.4.5.7");
        distributedApplianceInst.setName("Agent1");

        EntityManager.create(session, distributedApplianceInst);

        // retrieve back and validate
        distributedApplianceInst = (DistributedApplianceInstance) session.get(DistributedApplianceInstance.class,
                distributedApplianceInst.getId());
        assertNotNull(distributedApplianceInst);
    }

    private VirtualSystem addVirtualSystemEntity(Session session, ApplianceSoftwareVersion applianceSwVer, VirtualizationConnector virtualizationCon, DistributedAppliance distributedAppliance, Domain domain) {
        VirtualSystem virtualSystem = new VirtualSystem(distributedAppliance);
        virtualSystem.setDomain(domain);
        virtualSystem.setApplianceSoftwareVersion(applianceSwVer);
        virtualSystem.setNsxServiceId("nsx-service-4");
        virtualSystem.setNsxServiceInstanceId("nsx-service-inst-5");
        virtualSystem.setNsxServiceManagerId("nsx-servicemgr-4");
        virtualSystem.setVirtualizationConnector(virtualizationCon);

        EntityManager.create(session, virtualSystem);

        // retrieve back and validate
        virtualSystem = (VirtualSystem) session.get(VirtualSystem.class, virtualSystem.getId());
        assertNotNull(virtualSystem);
        return virtualSystem;
    }

    private Domain addDomainEntity(Session session, ApplianceManagerConnector applianceMgrCon) {
        Domain domain = new Domain(applianceMgrCon);
        domain.setName("DC-1");
        domain.setMgrId("domain-id-3");

        EntityManager.create(session, domain);

        // retrieve back and validate
        domain = (Domain) session.get(Domain.class, domain.getId());
        assertNotNull(domain);
        return domain;
    }

    private JobRecord addJobRecord(Session session){
        JobRecord jobRecord = new JobRecord();
        jobRecord.setName("testJob");
        EntityManager.create(session, jobRecord);

        // retrieve back and validate
        jobRecord = (JobRecord) session.get(JobRecord.class, jobRecord.getId());
        assertNotNull(jobRecord);
        return jobRecord;
    }

    private DistributedAppliance addDistributedApplianceEntity(Session session, Appliance appliance, ApplianceManagerConnector applianceMgrCon, JobRecord jobRecord) {
        DistributedAppliance distributedAppliance = new DistributedAppliance(applianceMgrCon);
        distributedAppliance.setLastJob(jobRecord);
        distributedAppliance.getLastJob().setStatus(JobStatus.PASSED);
        distributedAppliance.setName("distributedappliance-1");
        distributedAppliance.setMgrSecretKey("secret-2");
        distributedAppliance.setAppliance(appliance);
        distributedAppliance.setApplianceVersion("1.0");

        EntityManager.create(session, distributedAppliance);

        // retrieve back and validate
        distributedAppliance = (DistributedAppliance) session.get(DistributedAppliance.class, distributedAppliance.getId());
        assertNotNull(distributedAppliance);
        return distributedAppliance;
    }

    private VirtualizationConnector addVirtualizationConnectorEntity(Session session) {
        VirtualizationConnector virtualizationCon = new VirtualizationConnector();

        virtualizationCon.setName("vmware-1");
        virtualizationCon.setControllerIpAddress("172.3.4.5");
        virtualizationCon.setControllerPassword("abc123");
        virtualizationCon.setControllerUsername("user1");
        virtualizationCon.setProviderIpAddress("123.4.5.6");
        virtualizationCon.setProviderUsername("nsmuser");
        virtualizationCon.setProviderPassword("abc2");
        virtualizationCon.setVirtualizationSoftwareVersion("12.3");
        virtualizationCon.setVirtualizationType(VirtualizationType.VMWARE);

        EntityManager.create(session, virtualizationCon);

        // retrieve back and validate
        virtualizationCon = (VirtualizationConnector) session.get(VirtualizationConnector.class, virtualizationCon.getId());
        assertNotNull(virtualizationCon);
        return virtualizationCon;
    }

    private ApplianceManagerConnector addApplianceManagerConnectorEntity(Session session) {
        ApplianceManagerConnector applianceMgrCon = new ApplianceManagerConnector();

        applianceMgrCon.setName("nsm-1");
        applianceMgrCon.setManagerType(ManagerType.NSM);
        applianceMgrCon.setServiceType("IPS_IDS");
        applianceMgrCon.setUsername("admin");
        applianceMgrCon.setPassword("pass123");
        applianceMgrCon.setIpAddress("172.34.5.677");

        EntityManager.create(session, applianceMgrCon);

        // retrieve back and validate
        applianceMgrCon = (ApplianceManagerConnector) session.get(ApplianceManagerConnector.class, applianceMgrCon.getId());
        assertNotNull(applianceMgrCon);
        return applianceMgrCon;
    }

    private ApplianceSoftwareVersion addApplianceSoftwareVersion(Session session, Appliance appliance) {
        ApplianceSoftwareVersion applianceSwVer = new ApplianceSoftwareVersion(appliance);

        applianceSwVer.setApplianceSoftwareVersion("sw_1.0");
        applianceSwVer.setImageUrl("http://imageUrl");
        applianceSwVer.setVirtualizarionSoftwareVersion("vir_1.0");
        applianceSwVer.setVirtualizationType(VirtualizationType.VMWARE);

        EntityManager.create(session, applianceSwVer);

        // retrieve back and validate
        applianceSwVer = (ApplianceSoftwareVersion) session.get(ApplianceSoftwareVersion.class, applianceSwVer.getId());
        assertNotNull(applianceSwVer);
        return applianceSwVer;
    }

    private Appliance addApplianceEntity(Session session) {
        Appliance appliance = new Appliance();

        appliance.setManagerType(ManagerType.NSM);
        appliance.setModel("model-1");
        appliance.setManagerSoftwareVersion("1.2");

        EntityManager.create(session, appliance);

        // retrieve back and validate
        appliance = (Appliance) session.get(Appliance.class, appliance.getId());
        assertNotNull(appliance);
        return appliance;
    }

    private void addReleaseInfoEntity(Session session) {
        ReleaseInfo releaseInfo = new ReleaseInfo();

        releaseInfo.setDbVersion(0);
        EntityManager.create(session, releaseInfo);

        // retrieve back and validate
        releaseInfo = (ReleaseInfo) session.get(ReleaseInfo.class, releaseInfo.getId());
        assertNotNull(releaseInfo);
    }

    private void addUserEntity(Session session) {
        User user = new User();

        user.setFirstName("Trinh");
        user.setLastName("Lee");
        user.setLoginName("tlee");
        user.setRole(RoleType.ADMIN);
        user.setEmail("trinh_lee@mcafee.com");

        EntityManager.create(session, user);

        // retrieve back and validate
        user = (User) session.get(User.class, user.getId());

        assertNotNull(user);
    }

    @After
    public void tearDown() {
        InMemDB.shutdown(); // clean up db caches and connection pools
    }
}
