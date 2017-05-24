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
package org.osc.core.broker.service.tasks.conformance.virtualizationconnector;

import javax.persistence.EntityManager;

import org.apache.log4j.Logger;
import org.osc.core.broker.job.lock.LockManager;
import org.osc.core.broker.job.lock.LockRequest;
import org.osc.core.broker.job.lock.LockRequest.LockType;
import org.osc.core.broker.model.entities.virtualization.VirtualizationConnector;
import org.osc.core.broker.service.LockUtil;
import org.osc.core.broker.service.persistence.OSCEntityManager;
import org.osc.core.broker.service.persistence.SslCertificateAttrEntityMgr;
import org.osc.core.broker.service.tasks.TransactionalTask;
import org.osc.core.broker.service.tasks.conformance.UnlockObjectTask;
import org.osgi.service.component.annotations.Component;

@Component(service=VCDeleteMetaTask.class)
public class VCDeleteMetaTask extends TransactionalTask {
    private static final Logger log = Logger.getLogger(VCDeleteMetaTask.class);

    private VirtualizationConnector vc;

    public VCDeleteMetaTask create(VirtualizationConnector vc) {
        VCDeleteMetaTask task = new VCDeleteMetaTask();
        task.vc = vc;
        task.name = task.getName();
        task.dbConnectionManager = this.dbConnectionManager;
        task.txBroadcastUtil = this.txBroadcastUtil;

        return task;
    }

    @Override
    public void executeTransaction(EntityManager em) throws Exception {
        log.info("Start executing VCConformanceCheckMetaTask task for VC '" + this.vc.getName() + "'");

        UnlockObjectTask vcUnlockTask = null;
        try {
            this.vc = em.find(VirtualizationConnector.class, this.vc.getId());
            vcUnlockTask = LockUtil.lockVC(this.vc, LockType.WRITE_LOCK);
            OSCEntityManager<VirtualizationConnector> vcEntityMgr = new OSCEntityManager<>(VirtualizationConnector.class, em, this.txBroadcastUtil);
            VirtualizationConnector connector = vcEntityMgr.findByPrimaryKey(this.vc.getId());

            SslCertificateAttrEntityMgr sslCertificateAttrEntityMgr = new SslCertificateAttrEntityMgr(em, this.txBroadcastUtil);
            sslCertificateAttrEntityMgr.removeCertificateList(connector.getSslCertificateAttrSet());
            vcEntityMgr.delete(this.vc.getId());
        } catch (Exception ex) {
            // If we experience any failure, unlock VC.
            if (vcUnlockTask != null) {
                log.info("Releasing lock for VC '" + this.vc.getName() + "'");
                LockManager.getLockManager().releaseLock(new LockRequest(vcUnlockTask));
            }
            throw ex;
        }
    }

    @Override
    public String getName() {
        return "Delete Virtualization Connector '" + this.vc.getName() + "'";
    }

}