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

import java.util.HashMap;
import java.util.Map;

import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

public class InMemDB {
    private static EntityManagerFactory emf;

    static EntityManagerFactory init() {

        try {
            Map<String, Object> props = new HashMap<>();

            props.put("javax.persistence.jdbc.driver", "org.h2.Driver");
            props.put("javax.persistence.jdbc.url", "jdbc:h2:mem:test"); // in-memory db
            props.put("javax.persistence.schema-generation.database.action", "drop-and-create"); // create brand-new db schema in memory
            props.put("hibernate.dialect", "org.hibernate.dialect.H2Dialect");
            props.put("hibernate.show_sql", "true");


            emf = Persistence.createEntityManagerFactory("osc-server", props);

            return emf;

        } catch (Throwable ex) {
            System.out.println("Initial SessionFactory creation failed." + ex);
            throw new ExceptionInInitializerError(ex);
        }
    }

    public static EntityManagerFactory getEntityManagerFactory() {
        if(emf == null) {
            emf = init();
        }
        return emf;
    }

    public static void shutdown() {
        // Close caches and connection pools
        emf.close();
        emf = null;
    }

}
