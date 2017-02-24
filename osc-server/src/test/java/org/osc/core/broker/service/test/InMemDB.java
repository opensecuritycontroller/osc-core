package org.osc.core.broker.service.test;

import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.service.ServiceRegistryBuilder;
import org.osc.core.broker.util.db.HibernateUtil;

class InMemDB {
    private static ServiceRegistry serviceRegistry;
    private static SessionFactory sessionFactory = init();

    static SessionFactory init() {

        try {
            Configuration configuration = new Configuration();

            configuration.setProperty("hibernate.connection.driver_class", "org.h2.Driver");
            configuration.setProperty("hibernate.connection.url", "jdbc:h2:mem"); // in-memory db
            configuration.setProperty("hibernate.dialect", "org.hibernate.dialect.H2Dialect");
            configuration.setProperty("hibernate.show_sql", "true");
            configuration.setProperty("hibernate.hbm2ddl.auto", "create"); // create brand-new db schema in memory

            HibernateUtil.addAnnotatedClasses(configuration);

            serviceRegistry = new ServiceRegistryBuilder().applySettings(configuration.getProperties()).buildServiceRegistry();
            sessionFactory = configuration.buildSessionFactory(serviceRegistry);

            return sessionFactory;

        } catch (Throwable ex) {
            System.out.println("Initial SessionFactory creation failed." + ex);
            throw new ExceptionInInitializerError(ex);
        }
    }

    static SessionFactory getSessionFactory() {
        return sessionFactory;
    }

    static void shutdown() {
        // Close caches and connection pools
        getSessionFactory().close();
        InMemDB.serviceRegistry = null; // for faster garbage collection cleanup
        InMemDB.sessionFactory = null;
    }

}
