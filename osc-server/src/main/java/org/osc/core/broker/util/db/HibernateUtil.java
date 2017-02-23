package org.osc.core.broker.util.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.apache.commons.lang.RandomStringUtils;
import org.apache.log4j.Logger;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.service.ServiceRegistryBuilder;
import org.osc.core.broker.model.entities.ReleaseInfo;
import org.osc.core.broker.model.entities.SslCertificateAttr;
import org.osc.core.broker.model.entities.User;
import org.osc.core.broker.model.entities.appliance.Appliance;
import org.osc.core.broker.model.entities.appliance.ApplianceSoftwareVersion;
import org.osc.core.broker.model.entities.appliance.DistributedAppliance;
import org.osc.core.broker.model.entities.appliance.DistributedApplianceInstance;
import org.osc.core.broker.model.entities.appliance.VirtualSystem;
import org.osc.core.broker.model.entities.appliance.VirtualSystemMgrFile;
import org.osc.core.broker.model.entities.appliance.VirtualSystemPolicy;
import org.osc.core.broker.model.entities.archive.JobsArchive;
import org.osc.core.broker.model.entities.events.Alarm;
import org.osc.core.broker.model.entities.events.Alert;
import org.osc.core.broker.model.entities.events.EmailSettings;
import org.osc.core.broker.model.entities.job.JobObject;
import org.osc.core.broker.model.entities.job.JobRecord;
import org.osc.core.broker.model.entities.job.TaskObject;
import org.osc.core.broker.model.entities.job.TaskRecord;
import org.osc.core.broker.model.entities.management.ApplianceManagerConnector;
import org.osc.core.broker.model.entities.management.Domain;
import org.osc.core.broker.model.entities.management.Policy;
import org.osc.core.broker.model.entities.virtualization.SecurityGroup;
import org.osc.core.broker.model.entities.virtualization.SecurityGroupInterface;
import org.osc.core.broker.model.entities.virtualization.SecurityGroupMember;
import org.osc.core.broker.model.entities.virtualization.VirtualizationConnector;
import org.osc.core.broker.model.entities.virtualization.openstack.AvailabilityZone;
import org.osc.core.broker.model.entities.virtualization.openstack.DeploymentSpec;
import org.osc.core.broker.model.entities.virtualization.openstack.Host;
import org.osc.core.broker.model.entities.virtualization.openstack.HostAggregate;
import org.osc.core.broker.model.entities.virtualization.openstack.Network;
import org.osc.core.broker.model.entities.virtualization.openstack.OsFlavorReference;
import org.osc.core.broker.model.entities.virtualization.openstack.OsImageReference;
import org.osc.core.broker.model.entities.virtualization.openstack.OsSecurityGroupReference;
import org.osc.core.broker.model.entities.virtualization.openstack.Subnet;
import org.osc.core.broker.model.entities.virtualization.openstack.VM;
import org.osc.core.broker.model.entities.virtualization.openstack.VMPort;
import org.osc.core.util.KeyStoreProvider.KeyStoreProviderException;

public class HibernateUtil {

    private static final Logger log = Logger.getLogger(HibernateUtil.class);

    private static ServiceRegistry serviceRegistry;
    private static SessionFactory sessionFactory;

    public static Connection getSQLConnection(DBConnectionParameters params) throws SQLException, KeyStoreProviderException {
        ensureInitialized();

        return DriverManager.getConnection(params.getConnectionURL(), params.getLogin(), params.getPassword());
    }

    public static void addAnnotatedClasses(Configuration configuration) {

        configuration.addAnnotatedClass(User.class);
        configuration.addAnnotatedClass(ReleaseInfo.class);
        configuration.addAnnotatedClass(Alarm.class);
        configuration.addAnnotatedClass(Alert.class);
        configuration.addAnnotatedClass(Appliance.class);
        configuration.addAnnotatedClass(ApplianceSoftwareVersion.class);
        configuration.addAnnotatedClass(DistributedAppliance.class);
        configuration.addAnnotatedClass(DistributedApplianceInstance.class);
        configuration.addAnnotatedClass(EmailSettings.class);
        configuration.addAnnotatedClass(ApplianceManagerConnector.class);
        configuration.addAnnotatedClass(VirtualSystem.class);
        configuration.addAnnotatedClass(Domain.class);
        configuration.addAnnotatedClass(Policy.class);
        configuration.addAnnotatedClass(SecurityGroupInterface.class);
        configuration.addAnnotatedClass(VirtualizationConnector.class);
        configuration.addAnnotatedClass(JobRecord.class);
        configuration.addAnnotatedClass(JobObject.class);
        configuration.addAnnotatedClass(TaskRecord.class);
        configuration.addAnnotatedClass(TaskObject.class);
        configuration.addAnnotatedClass(VirtualSystemPolicy.class);
        configuration.addAnnotatedClass(VirtualSystemMgrFile.class);
        configuration.addAnnotatedClass(DeploymentSpec.class);
        configuration.addAnnotatedClass(AvailabilityZone.class);
        configuration.addAnnotatedClass(Host.class);
        configuration.addAnnotatedClass(OsImageReference.class);
        configuration.addAnnotatedClass(HostAggregate.class);
        configuration.addAnnotatedClass(OsFlavorReference.class);
        configuration.addAnnotatedClass(SecurityGroup.class);
        configuration.addAnnotatedClass(SecurityGroupMember.class);
        configuration.addAnnotatedClass(VM.class);
        configuration.addAnnotatedClass(VMPort.class);
        configuration.addAnnotatedClass(JobsArchive.class);
        configuration.addAnnotatedClass(Network.class);
        configuration.addAnnotatedClass(Subnet.class);
        configuration.addAnnotatedClass(OsSecurityGroupReference.class);
        configuration.addAnnotatedClass(SslCertificateAttr.class);
    }

    public static void initSessionFactory() {
        if (sessionFactory != null) {
            sessionFactory.close();
            sessionFactory = null;
        }

        ensureInitialized();
    }

    private static SessionFactory init() {
        /*
         * Increase lock timeout and avoid table lock for updates as per below.
         * http
         * ://stackoverflow.com/questions/4162557/timeout-error-trying-to-lock
         * -table-in-h2
         */

        try {

            DBConnectionParameters connectionParams = new DBConnectionParameters();

            Configuration configuration = new Configuration();

            connectionParams.fillHibernateConfiguration(configuration);

            addAnnotatedClasses(configuration);

            serviceRegistry = new ServiceRegistryBuilder().applySettings(configuration.getProperties())
                    .buildServiceRegistry();
            sessionFactory = configuration.buildSessionFactory(serviceRegistry);

            return sessionFactory;

        } catch (Throwable ex) {

            log.error("Initial SessionFactory creation failed.", ex);
            throw new ExceptionInInitializerError(ex);
        }
    }

    public static void replaceDefaultDBPassword(DBConnectionParameters params) throws Exception {
        try (Connection connection = HibernateUtil.getSQLConnection(params);
                PreparedStatement changePasswordStatement = connection.prepareStatement("ALTER USER " + params.getLogin().toUpperCase() + " SET PASSWORD ?")) {

            connection.setAutoCommit(false);

            String newPassword = RandomStringUtils.randomAscii(32);

            try {
                // change password in DB
                changePasswordStatement.setString(1, newPassword);
                changePasswordStatement.execute();

                // put password in keystore
                params.updatePassword(newPassword);

                // reinitialize session factory
                HibernateUtil.initSessionFactory();
                connection.commit();
            } catch (Exception ex) {
                log.error("Error while changing DB password.", ex);

                try {
                    connection.rollback();
                } catch (Exception he) {
                    log.error("Error rolling back transaction", he);
                }

                throw ex;
            }
        }
    }

    public static SessionFactory getSessionFactory() {
        ensureInitialized();

        return sessionFactory;
    }

    private static void ensureInitialized() {
        if (sessionFactory == null) {
            sessionFactory = init();
        }
    }

    public static void shutdown() {
        SessionFactory sessionFactory = HibernateUtil.getSessionFactory();
        Session session = sessionFactory.openSession();
        try {
            session.createSQLQuery("SHUTDOWN COMPACT").executeUpdate();
        } catch (HibernateException e) {
            log.error("Error during shutdown of DB.", e);
            // Ignore errors
        } finally {
            session.close();
        }
        // Close caches and connection pools
        getSessionFactory().close();
    }

}
