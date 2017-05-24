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

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.Callable;

import javax.persistence.EntityManager;

import org.apache.log4j.Logger;
import org.hibernate.StaleObjectStateException;
import org.hibernate.exception.ConstraintViolationException;
import org.osc.core.broker.service.api.ServiceDispatcherApi;
import org.osc.core.broker.service.api.server.UserContextApi;
import org.osc.core.broker.service.exceptions.VmidcDbConcurrencyException;
import org.osc.core.broker.service.exceptions.VmidcDbConstraintViolationException;
import org.osc.core.broker.service.exceptions.VmidcException;
import org.osc.core.broker.service.request.Request;
import org.osc.core.broker.service.response.Response;
import org.osc.core.broker.service.ssl.SslCertificatesExtendedException;
import org.osc.core.broker.util.TransactionalBroadcastUtil;
import org.osc.core.broker.util.db.DBConnectionManager;
import org.osc.core.server.Server;
import org.osc.core.util.ServerUtil;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.transaction.control.ScopedWorkException;
import org.osgi.service.transaction.control.TransactionControl;

import com.google.common.annotations.VisibleForTesting;

public abstract class ServiceDispatcher<I extends Request, O extends Response> implements ServiceDispatcherApi<I, O> {

    private static final Logger log = Logger.getLogger(ServiceDispatcher.class);
    private EntityManager em = null;

    /**
     * This field is why we have: <p><code>
     *
     *     -dsannotations-options: inherit
     *
     * </code><p> in the bnd file. This is a non-default
     * option, but it is much neater than forcing every service
     * to implement a getUserConext() method.
     */
    @Reference
    protected UserContextApi userContext;

    @Reference
    protected DBConnectionManager dbConnectionManager;

    @Reference
    protected TransactionalBroadcastUtil txBroadcastUtil;

    private final Queue<ChainedDispatch<O>> chainedDispatches = new LinkedList<>();

    // generalized method to dispatch incoming requests to the appropriate
    // service handler
    @Override
    public O dispatch(I request) throws Exception {
        log.info("Service dispatch " + this.getClass().getSimpleName() + ". User: " + this.userContext.getCurrentUser()
        + ", Request: " + request);

        if (Server.isInMaintenance()) {
            log.warn("Incoming request (pid:" + ServerUtil.getCurrentPid() + ") while server is in maintenance mode.");
            throw new VmidcException(Server.PRODUCT_NAME + " server is in maintenance mode.");
        }

        if (this.em == null) {
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

		ChainedDispatch<O> nextDispatch;
		while ((nextDispatch = popChain()) != null) {
			try {
				final O previousResponse = response;
				final ChainedDispatch<O> tempNext = nextDispatch;
				response = txControl.required(() -> tempNext.dispatch(previousResponse, this.em));
			} catch (ScopedWorkException e) {
				handleException((Exception) e.getCause());
			}
		}

        log.info("Service response: " + response);
        return response;
    }

    private ChainedDispatch<O> popChain() {
    	synchronized (this.chainedDispatches) {
    		return this.chainedDispatches.poll();
		}
    }

	/**
	 * <p>
	 * Chain an additional transaction step that will be executed in its own
	 * transaction, after the successful completion of the main transaction and
	 * all previously chained transaction steps. It is legal to add a chained
	 * step during the execution of the main transaction and/or during a
	 * previously added step.
	 * </p>
	 *
	 * <p>
	 * This variation of the method is convenient for chaining method
	 * references, for example:
	 * </p>
	 *
	 * <pre>
	 * &#64;Override
	 * protected B exec(A a, EntityManager em) throws Exception {
	 *     B interimResult = ...;
	 *     chain(this::doNextPart);
	 *     return interimResult;
	 * }
	 * private B doNextPart(B input, EntityManager em) {
	 *     // input comes from 'interimResult' above
	 *     em.merge(input);
	 *     return new B(...);
	 * }
	 * </pre>
	 *
	 * @param dispatch
	 *            A function that shall be executed in its own transaction. The
	 *            input to the function shall be the result from the previous
	 *            step or, if this is the first step, the result from the main
	 *            transaction. If this is the last step then the return value of
	 *            the provided function is used as the result for the entire
	 *            dispatch.
	 */
	protected void chain(ChainedDispatch<O> dispatch) {
		synchronized (this.chainedDispatches) {
			this.chainedDispatches.add(dispatch);
		}
	}

	/**
	 * <p>See {@link #chain(ChainedDispatch)}.</p>
	 *
	 * <p>This variation of the method is useful for chaining lambdas, which can
	 * have visibility of the outer scope. For example:</p>
	 *
	 * <pre>
	 * &#64;Override
	 * protected B exec(A a, EntityManager em) throws Exception {
	 *     B interimResult = ...;
	 *     chain(() -> {
	 *         em.merge(interimResult);
	 *         return new B(...);
	 *     });
	 *     return interimResult;
	 * }
	 *
	 * </pre>
	 *
	 * @param call
	 * @see #chain(ChainedDispatch)
	 */
	protected void chain(Callable<O> call) {
		ChainedDispatch<O> dispatch = (em, o) -> call.call();
		synchronized (this.chainedDispatches) {
			this.chainedDispatches.add(dispatch);
		}
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
        return this.dbConnectionManager.getTransactionalEntityManager();
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
        return this.dbConnectionManager.getTransactionControl();
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
