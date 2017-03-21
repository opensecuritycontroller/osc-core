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
package org.osc.core.broker.util.db;

import java.sql.Connection;
import java.sql.SQLException;

import javax.persistence.EntityManagerFactory;

import org.apache.log4j.Logger;
import org.osc.core.broker.service.exceptions.VmidcException;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;
import org.osgi.util.tracker.ServiceTracker;

public class HibernateUtil {

    private static final Logger log = Logger.getLogger(HibernateUtil.class);

    private static ServiceTracker<DBConnectionManager, DBConnectionManager> tracker;

    public static Connection getSQLConnection() throws InterruptedException, VmidcException, SQLException {
        DBConnectionManager mgr = getConnectionManager();

        return mgr.getSQLConnection();
    }

    private static DBConnectionManager getConnectionManager() throws InterruptedException, VmidcException {
        ensureInitialized();

        DBConnectionManager mgr = tracker.waitForService(10000);

        if(mgr == null){
            throw new VmidcException("No Database Manager Service could be found");
        }
        return mgr;
    }


    public static void replaceDefaultDBPassword(DBConnectionParameters params) throws Exception {
        DBConnectionManager manager = getConnectionManager();
        manager.replaceDefaultDBPassword();
    }

    public static EntityManagerFactory getEntityManagerFactory() throws InterruptedException, VmidcException {
        DBConnectionManager manager = getConnectionManager();

        return manager.getEmf();
    }

    private static void ensureInitialized() {
        boolean startTracker = false;
        synchronized (HibernateUtil.class) {
            if(tracker == null) {
                Bundle bundle = FrameworkUtil.getBundle(HibernateUtil.class);
                tracker = new ServiceTracker<>(bundle.getBundleContext(),
                        DBConnectionManager.class, null);
                startTracker = true;
            }
        }

        if (startTracker) {
            tracker.open();
        }
    }

    public static void shutdown() {
        ServiceTracker<?, ?> toClose;
        synchronized (HibernateUtil.class) {
            toClose = tracker;
            tracker = null;
        }
        if(toClose != null) {
            toClose.close();
        }
    }

}
