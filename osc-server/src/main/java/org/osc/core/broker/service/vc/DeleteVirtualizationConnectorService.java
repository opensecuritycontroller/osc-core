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
package org.osc.core.broker.service.vc;

import javax.persistence.EntityManager;

import org.osc.core.broker.job.lock.LockRequest.LockType;
import org.osc.core.broker.model.entities.virtualization.VirtualizationConnector;
import org.osc.core.broker.service.LockUtil;
import org.osc.core.broker.service.ServiceDispatcher;
import org.osc.core.broker.service.api.vc.DeleteVirtualizationConnectorServiceApi;
import org.osc.core.broker.service.exceptions.VmidcBrokerValidationException;
import org.osc.core.broker.service.persistence.OSCEntityManager;
import org.osc.core.broker.service.persistence.SslCertificateAttrEntityMgr;
import org.osc.core.broker.service.persistence.VirtualizationConnectorEntityMgr;
import org.osc.core.broker.service.request.BaseIdRequest;
import org.osc.core.broker.service.response.EmptySuccessResponse;
import org.osc.core.broker.service.tasks.conformance.UnlockObjectMetaTask;
import org.osgi.service.component.annotations.Component;

@Component
public class DeleteVirtualizationConnectorService extends ServiceDispatcher<BaseIdRequest, EmptySuccessResponse>
    implements DeleteVirtualizationConnectorServiceApi {

    @Override
    public EmptySuccessResponse exec(BaseIdRequest request, EntityManager em) throws Exception {
        validate(em, request);

        OSCEntityManager<VirtualizationConnector> vcEntityMgr = new OSCEntityManager<>(VirtualizationConnector.class, em, this.txBroadcastUtil);
        VirtualizationConnector vc = vcEntityMgr.findByPrimaryKey(request.getId());

        UnlockObjectMetaTask vcUnlock = null;
		try {
			vcUnlock = LockUtil.tryLockVC(vc, LockType.WRITE_LOCK);

			SslCertificateAttrEntityMgr sslCertificateAttrEntityMgr = new SslCertificateAttrEntityMgr(em, this.txBroadcastUtil);
			sslCertificateAttrEntityMgr.removeCertificateList(vc.getSslCertificateAttrSet());
			vcEntityMgr.delete(request.getId());
		} finally {
			LockUtil.releaseLocks(vcUnlock);
		}

        return new EmptySuccessResponse();
    }

    void validate(EntityManager em, BaseIdRequest request) throws Exception {
        VirtualizationConnector vc = em.find(VirtualizationConnector.class, request.getId());

        // entry must pre-exist in db
        if (vc == null) { // note: we cannot use name here in error msg since del req does not have name, only ID
            throw new VmidcBrokerValidationException("Virtualization Connector entry with ID " + request.getId() + " is not found.");
        }

        VirtualizationConnectorEntityMgr.validateCanBeDeleted(em, vc);
    }

}
