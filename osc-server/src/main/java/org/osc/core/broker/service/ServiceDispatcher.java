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
package org.osc.core.broker.service;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;

import org.apache.log4j.Logger;
import org.hibernate.HibernateException;
import org.hibernate.StaleObjectStateException;
import org.hibernate.exception.ConstraintViolationException;
import org.osc.core.broker.service.exceptions.VmidcDbConcurrencyException;
import org.osc.core.broker.service.exceptions.VmidcDbConstraintViolationException;
import org.osc.core.broker.service.exceptions.VmidcException;
import org.osc.core.broker.service.request.Request;
import org.osc.core.broker.service.request.SslCertificatesExtendedException;
import org.osc.core.broker.service.response.Response;
import org.osc.core.broker.util.SessionUtil;
import org.osc.core.broker.util.TransactionalBroadcastUtil;
import org.osc.core.broker.util.db.HibernateUtil;
import org.osc.core.util.ServerUtil;

import com.google.common.annotations.VisibleForTesting;
import com.mcafee.vmidc.server.Server;

public abstract class ServiceDispatcher<I extends Request, O extends Response> {

    private static final Logger log = Logger.getLogger(ServiceDispatcher.class);
    private EntityTransaction tx = null;
    private EntityManager em = null;

    // generalized method to dispatch incoming requests to the appropriate
    // service handler
    public O dispatch(I request) throws Exception {
        log.info("Service dispatch " + this.getClass().getSimpleName() + ". User: " + SessionUtil.getCurrentUser()
        + ", Request: " + request);

        if (Server.isInMaintenance()) {
            log.warn("Incoming request (pid:" + ServerUtil.getCurrentPid() + ") while server is in maintenance mode.");
            throw new VmidcException(Server.PRODUCT_NAME + " server is in maintenance mode.");
        }

        if (this.em == null) {
            EntityManagerFactory emf = getEntityManagerFactory();
            this.em = emf.createEntityManager();
        }

        // add session with empty list in pendingBroadcastMessageMap
        O response = null;
        try {

            // Initializing transaction
            this.tx = this.em.getTransaction();
            this.tx.begin();
            // calling service implementation
            response = exec(request, this.em);

            // if no exception, commit the transaction
            commitChanges(false);

        } catch (Exception e) {

            handleException(this.em, e);

        } finally {
            if (this.em != null && this.em.isOpen()) {
                this.em.close();
            }
        }

        log.info("Service response: " + response);
        return response;
    }

    /**
     * Created for the testing the class. Which helps to create the mock object of SessionFactory.
     *
     * @return
     */
    @VisibleForTesting
    protected EntityManagerFactory getEntityManagerFactory() {
        return HibernateUtil.getEntityManagerFactory();
    }

    /**
     * Commits the open transaction and wraps exceptions as appropriate.
     *
     * Generally the commits are handled automatically by the service, but in some case we might need to commit
     * the transaction before we start a long running operation(like a job).
     *
     * @param startNewTransaction
     *            starts a new transaction if set to true. If set to false, commits the transaction and
     *            closes the session.
     *
     */
    protected void commitChanges(boolean startNewTransaction) throws Exception {
        try {
            if (this.tx != null) {
                this.tx.commit();
            }
            this.tx = null;
            TransactionalBroadcastUtil.broadcast(this.em);
            if (startNewTransaction) {
                this.tx = this.em.getTransaction();
                this.tx.begin();
            } else {
                if (this.em.isOpen()) {
                    this.em.close();
                }
            }
        } catch (Exception e) {
            handleException(this.em, e);
        }
    }

    protected abstract O exec(I request, EntityManager em) throws Exception;

    private void handleException(EntityManager em, Exception e) throws VmidcDbConstraintViolationException,
    VmidcDbConcurrencyException, Exception {
        if(e instanceof SslCertificatesExtendedException){
            throw e;
        }else if (e instanceof VmidcException) {
            log.warn("Service request failed (logically): " + e.getMessage());
        } else {
            log.error("Service request failed (unexpectedly): " + e.getMessage(), e);
        }

        try {
            if (this.tx != null) {
                this.tx.rollback();
                TransactionalBroadcastUtil.removeSessionFromMap(em);
            }
        } catch (HibernateException he) {
            log.error("Error rolling back database transaction", he);
        }

        if (e instanceof ConstraintViolationException) {
            log.error("Got database constraint violation exception", e);

            throw new VmidcDbConstraintViolationException("Database Constraint Violation Exception.");

        } else if (e instanceof StaleObjectStateException) {
            log.error("Got database concurrency exception", e);

            throw new VmidcDbConcurrencyException("Database Concurrency Exception.");
        }

        throw e;
    }

}
