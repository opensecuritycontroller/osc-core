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

import java.util.concurrent.Callable;

import javax.persistence.EntityManager;

import org.apache.log4j.Logger;
import org.hibernate.StaleObjectStateException;
import org.hibernate.exception.ConstraintViolationException;
import org.osc.core.broker.service.exceptions.VmidcDbConcurrencyException;
import org.osc.core.broker.service.exceptions.VmidcDbConstraintViolationException;
import org.osc.core.broker.service.exceptions.VmidcException;
import org.osc.core.broker.service.request.Request;
import org.osc.core.broker.service.request.SslCertificatesExtendedException;
import org.osc.core.broker.service.response.Response;
import org.osc.core.broker.util.SessionUtil;
import org.osc.core.broker.util.db.HibernateUtil;
import org.osc.core.util.ServerUtil;
import org.osgi.service.transaction.control.ScopedWorkException;
import org.osgi.service.transaction.control.TransactionControl;

import com.google.common.annotations.VisibleForTesting;
import com.mcafee.vmidc.server.Server;

public abstract class ServiceDispatcher<I extends Request, O extends Response> {

    private static final Logger log = Logger.getLogger(ServiceDispatcher.class);
    private EntityManager em = null;

    private Callable<O> secondaryDispatch;

    // generalized method to dispatch incoming requests to the appropriate
    // service handler
    public O dispatch(I request) throws Exception {
        log.info("Service dispatch " + this.getClass().getSimpleName() + ". User: " + SessionUtil.getCurrentUser()
        + ", Request: " + request);

        if (Server.isInMaintenance()) {
            log.warn("Incoming request (pid:" + ServerUtil.getCurrentPid() + ") while server is in maintenance mode.");
            throw new VmidcException(Server.PRODUCT_NAME + " server is in maintenance mode.");
        }

        if (this.em == null || !this.em.isOpen()) {
            this.em = getEntityManager();
        }

        TransactionControl txControl = getTransactionControl();

        O response = null;
        try {
            // calling service in a transaction
            response = txControl.required(() -> exec(request, this.em));
        } catch (ScopedWorkException e) {

            handleException((Exception) e.getCause());

        }

        if(this.secondaryDispatch != null) {
            try {
                // calling the second step of this service in a transaction
                response = txControl.required(this.secondaryDispatch);
            } catch (ScopedWorkException e) {
                handleException((Exception) e.getCause());
            }
        }

        log.info("Service response: " + response);
        return response;
    }

    protected void chain(Callable<O> toCall) {
        if(this.secondaryDispatch != null) {
            throw new IllegalStateException("An additional transactional step has already been added");
        }
        this.secondaryDispatch = toCall;
    }

    /**
     * Created for the testing the class. Which helps to create the mock object of SessionFactory.
     *
     * @return
     * @throws VmidcException
     * @throws InterruptedException
     */
    @VisibleForTesting
    protected EntityManager getEntityManager() throws InterruptedException, VmidcException {
        return HibernateUtil.getTransactionalEntityManager();
    }

    /**
     * Created for the testing the class. Which helps to create the mock object of SessionFactory.
     *
     * @return
     * @throws VmidcException
     * @throws InterruptedException
     */
    @VisibleForTesting
    protected TransactionControl getTransactionControl() throws InterruptedException, VmidcException {
        return HibernateUtil.getTransactionControl();
    }

    protected abstract O exec(I request, EntityManager em) throws Exception;

    private void handleException(Exception e) throws VmidcDbConstraintViolationException,
    VmidcDbConcurrencyException, Exception {
        if(e instanceof SslCertificatesExtendedException){
            throw e;
        }else if (e instanceof VmidcException) {
            log.warn("Service request failed (logically): " + e.getMessage());
        } else {
            log.error("Service request failed (unexpectedly): " + e.getMessage(), e);
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
