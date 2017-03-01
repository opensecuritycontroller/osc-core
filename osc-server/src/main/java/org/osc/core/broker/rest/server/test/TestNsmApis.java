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
package org.osc.core.broker.rest.server.test;

import java.io.File;
import java.security.PublicKey;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.core.MediaType;

import org.apache.log4j.Logger;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.junit.Test;
import org.osc.core.broker.model.entities.appliance.VirtualSystem;
import org.osc.core.broker.model.entities.management.ApplianceManagerConnector;
import org.osc.core.broker.rest.server.model.MgrFile;
import org.osc.core.broker.service.PropagateVSMgrFileService;
import org.osc.core.broker.service.persistence.EntityManager;
import org.osc.core.broker.service.request.PropagateVSMgrFileRequest;
import org.osc.core.broker.util.db.HibernateUtil;
import org.osc.core.rest.client.RestBaseClient;
import org.osc.core.rest.client.VmidcAgentServerRestClient;
import org.osc.core.rest.client.agent.model.input.AgentUpdateMgrFileRequest;
import org.osc.core.rest.client.util.LoggingUtil;
import org.osc.core.util.PKIUtil;

public class TestNsmApis {

    class NsmApiRestClient extends RestBaseClient {

        Logger log = Logger.getLogger(NsmApiRestClient.class);

        private static final String NSM_API_REST_URL_BASE = "/api/nsm/v1";

        public NsmApiRestClient(String vmidcServer, int port, String loginName, String password, boolean isHttps) {

            super(NSM_API_REST_URL_BASE, MediaType.APPLICATION_XML);

            initRestBaseClient(vmidcServer, port, loginName, password, isHttps);
        }
    }

    @Test
    public void test() {

        boolean isTestVerify_agentPubKey = false;
        boolean isTest_PKI = false;
        boolean isTest_nsmPubKey = false;
        boolean isTestPut_agentMgrFile = false;
        boolean isTest_mgrFilePropagatingJob = false;
        boolean isTest_propagateMgrFileAPI = true;

        //String serverIP = "10.71.86.189";
        //Long vsId = new Long(33);

        if (isTest_propagateMgrFileAPI) {

            System.out.println("===== Start Test propagate mgrfile");

            try {

                //for (int vs = 1; vs < 2; vs++) {

                NsmApiRestClient nsmApiClient = new NsmApiRestClient("localhost", 8090, "admin", "admin123", true);

                MgrFile input = new MgrFile();
                File mgrFile = new File("C:\\sigfile1.gz");

                input.setMgrfile(PKIUtil.readBytesFromFile(mgrFile));
                input.setMgrFileName("sigfile1.gz");


                Set<String> daiSet = new HashSet<String>();
                daiSet.add("DATestMgrFile_1_2");

                //input.setApplianceInstances(null); //ALL option
                input.setApplianceInstances(daiSet);

                LoggingUtil.logPojoToJson(input);
                nsmApiClient.putResource("/propagateMgrFile/vs/" + "DA-3_1", input);

                // }

                System.out.println("TestNsmApis: done testing propagate mgrfile");

            } catch (Exception ex) {
                ex.printStackTrace();
                System.out.println("TestNsmApis: put exception: " + ex);
            }
        }

        if (isTestPut_agentMgrFile) {

            System.out.println("===== Start Test put agent mgrfile");

            try {

                System.out.println("Start time: " + new Date());

                AgentUpdateMgrFileRequest input = new AgentUpdateMgrFileRequest();
                byte[] temp = PKIUtil.readBytesFromFile(new File("/tmp/sigfile1"));

                input.setMgrFile(temp);
                input.setMgrFileName("sigfile.gz");

                /*
                 * for (int i = 0; i < 499; i++) {
                 */
                VmidcAgentServerRestClient client = new VmidcAgentServerRestClient("10.71.86.212", 8090, "agent",
                        "abc12345", true);
                client.putResource("updateMgrFile", input);

                System.out.println("TestNsmApis: done testing PUT agent mgrfile");

                /*
                 * if (i % 20 == 0) {
                 * System.out.println("garbage collection for i: " + (i + 1));
                 *
                 * Runtime.getRuntime().gc(); }
                 */
                /* } */

                System.out.println("End time: " + new Date());

            } catch (Exception ex) {
                ex.printStackTrace();

                System.out.println("TestNsmApis: put agent mgrfile exception: " + ex);
            }
        }

        if (isTestVerify_agentPubKey) {

            System.out.println("===== Start Test verify agent pubkey");

            Session session = null;
            Transaction tx = null;

            byte[] ks_1 = null;
            PublicKey pubkey_1 = null;
            byte[] ks_2 = null;

            // get pubkey and keystore for record #1
            try {

                SessionFactory sessionFactory = HibernateUtil.getSessionFactory();
                session = sessionFactory.getCurrentSession();

                // We must open a new transaction before doing anything with the
                // DB
                tx = session.beginTransaction();

                EntityManager<VirtualSystem> emgr = new EntityManager<VirtualSystem>(VirtualSystem.class, session);
                VirtualSystem vs = emgr.findByPrimaryKey(new Long(97));

                ks_1 = vs.getKeyStore();
                pubkey_1 = PKIUtil.getPubKey(ks_1);

                // We can now close the transaction and persist the changes
                tx.commit();

            } catch (RuntimeException re) {
                System.out.println("test verify agent pubkey: got runtime exception: " + re);

                if (tx != null && tx.isActive()) {
                    try {
                        // Second try catch as the rollback could fail as well
                        tx.rollback();
                    } catch (HibernateException he) {
                        // logger.debug("Error rolling back transaction");
                    }
                    // throw again the first exception
                    throw re;
                }
            } catch (Exception ex) {
                System.out.println("test verify agent pubkey: got general exception: " + ex);
            }

            // get pubkey and keystore for record #2
            try {

                SessionFactory sessionFactory = HibernateUtil.getSessionFactory();
                session = sessionFactory.getCurrentSession();

                // We must open a new transaction before doing anything with the
                // DB
                tx = session.beginTransaction();

                EntityManager<VirtualSystem> emgr = new EntityManager<VirtualSystem>(VirtualSystem.class, session);
                VirtualSystem vs = emgr.findByPrimaryKey(new Long(165));

                ks_2 = vs.getKeyStore();

                // We can now close the transaction and persist the changes
                tx.commit();

            } catch (RuntimeException re) {
                System.out.println("test verify agent pubkey: got runtime exception: " + re);

                if (tx != null && tx.isActive()) {
                    try {
                        // Second try catch as the rollback could fail as well
                        tx.rollback();
                    } catch (HibernateException he) {
                        // logger.debug("Error rolling back transaction");
                    }
                    // throw again the first exception
                    throw re;
                }
            } catch (Exception ex) {
                System.out.println("test verify agent pubkey: got general exception: " + ex);
            }

            System.out.println("=========== verify agent pubkey: start positive test case");

            // now doing positive test case, i.e. verify pubkey_1 with ks_1,
            // expected matched.
            if (!PKIUtil.verifyKeyPair(ks_1, pubkey_1)) {
                System.out.println("not expected: pubkey_1 does NOT match ks_1 !!!");
            } else {
                System.out.println("expected: pubkey_1 does match ks_1");
            }

            System.out.println("=========== verify agent pubkey: start negative test case");

            // now doing negative test case, i.e. verify pubkey_1 with ks_2,
            // expected not matched.
            if (!PKIUtil.verifyKeyPair(ks_2, pubkey_1)) {
                System.out.println("expected: pubkey_1 does NOT match ks_2");
            } else {
                System.out.println("not expected: pubkey_1 does match ks_2 !!!");
            }

        }

        if (isTest_PKI) {

            System.out.println("===== Start Test PKI");

            try {
                byte[] ks = PKIUtil.generateKeyStore();

                ks = PKIUtil.generateKeyStore();

                PKIUtil.writeBytesToFile(PKIUtil.extractCertificate(ks), "/tmp", "pubCert.pem");
                PKIUtil.writeBytesToFile(PKIUtil.extractPrivateKey(ks), "/tmp", "privKey.pem");
                PKIUtil.writeBytesToFile(PKIUtil.extractCertificate(ks), "/tmp", "pubKey.pem");

            } catch (Exception ex) {
                System.out.println("test pki: got general exception: " + ex);

            }

            System.out.println("===== done Test PKI");

        }

        if (isTest_nsmPubKey) {

            System.out.println("===== Start Test creating nsm pub key");

            Session session = null;
            Transaction tx = null;

            try {

                SessionFactory sessionFactory = HibernateUtil.getSessionFactory();
                session = sessionFactory.getCurrentSession();

                // We must open a new transaction before doing anything with the
                // DB
                tx = session.beginTransaction();

                EntityManager<ApplianceManagerConnector> emgr = new EntityManager<ApplianceManagerConnector>(
                        ApplianceManagerConnector.class, session);
                ApplianceManagerConnector mc = emgr.findByPrimaryKey(1L);
                byte[] pubKey = "abcdef".getBytes();
                mc.setPublicKey(pubKey);

                emgr.update(mc);

                // We can now close the transaction and persist the changes
                tx.commit();

            } catch (RuntimeException re) {
                System.out.println("test creating nsm pubkey: got runtime exception: " + re);

                if (tx != null && tx.isActive()) {
                    try {
                        // Second try catch as the rollback could fail as well
                        tx.rollback();
                    } catch (HibernateException he) {
                        // logger.debug("Error rolling back transaction");
                    }
                    // throw again the first exception
                    throw re;
                }
            } catch (Exception ex) {
                System.out.println("test creating nsm pubkey: got general exception: " + ex);
            }

        }

        if (isTest_mgrFilePropagatingJob) {

            System.out.println("================= Start Test mgrfile propagating job  ================");

            PropagateVSMgrFileRequest req = new PropagateVSMgrFileRequest();

            String vsName = "DA-3_1";
            // long daiID = 5L;
            String daiName = "vSensorVMIPS-100_2";

            Set<String> daiSet = new HashSet<String>();
            daiSet.add(daiName);
            MgrFile input = new MgrFile();
            File mgrFile = new File("C:\\sigfile1.gz");

            input.setMgrfile(PKIUtil.readBytesFromFile(mgrFile));
            input.setMgrFileName("sigfile.gz");

            try {

                req.setDaiList(daiSet);
                req.setVsName(vsName);

                PropagateVSMgrFileService svc = new PropagateVSMgrFileService();
                svc.dispatch(req);

                while (true) {
                    Thread.sleep(60 * 60 * 1000);
                }

            } catch (Exception ex) {

                ex.printStackTrace();
            }

        }
    }
}
