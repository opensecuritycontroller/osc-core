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
package org.osc.core.server.scheduler;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import javax.persistence.EntityManager;

import org.apache.log4j.Logger;
import org.osc.core.broker.model.entities.appliance.DistributedAppliance;
import org.osc.core.broker.model.entities.appliance.DistributedApplianceInstance;
import org.osc.core.broker.model.entities.appliance.VirtualSystem;
import org.osc.core.broker.model.entities.management.ApplianceManagerConnector;
import org.osc.core.broker.model.plugin.ApiFactoryService;
import org.osc.core.broker.model.plugin.manager.DistributedApplianceInstanceElementImpl;
import org.osc.core.broker.service.persistence.OSCEntityManager;
import org.osc.core.broker.util.StaticRegistry;
import org.osc.core.broker.util.db.HibernateUtil;
import org.osc.sdk.manager.api.ManagerDeviceMemberApi;
import org.osc.sdk.manager.element.ManagerDeviceMemberStatusElement;
import org.osgi.service.transaction.control.ScopedWorkException;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

public class ApplianceAgentsJob implements Job {

    private static final Logger log = Logger.getLogger(ApplianceAgentsJob.class);

    public ApplianceAgentsJob() {

    }

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        ApiFactoryService apiFactoryService =  (ApiFactoryService) context.get(ApiFactoryService.class.getName());

        try {
            EntityManager em = HibernateUtil.getTransactionalEntityManager();
            List<DistributedAppliance> das = HibernateUtil.getTransactionControl().required(() -> {
                OSCEntityManager<DistributedAppliance> emgr = new OSCEntityManager<DistributedAppliance>(
                        DistributedAppliance.class, em, StaticRegistry.transactionalBroadcastUtil());

                return emgr.listAll();
            });

            for (DistributedAppliance da : das) {
                for (VirtualSystem vs : da.getVirtualSystems()) {

                    ApplianceManagerConnector apmc = vs.getDistributedAppliance().getApplianceManagerConnector();
                    ManagerDeviceMemberApi agentApi =  apiFactoryService.createManagerDeviceMemberApi(apmc, vs);

                    if (apiFactoryService.providesDeviceStatus(vs)) {
                        List<ManagerDeviceMemberStatusElement> agentElems = agentApi.getFullStatus(
                                vs.getDistributedApplianceInstances().stream()
                                .map(DistributedApplianceInstanceElementImpl::new)
                                .collect(Collectors.toList()));
                        for (DistributedApplianceInstance dai : vs.getDistributedApplianceInstances()) {
                            getAgentFullStatus(dai, agentElems);
                        }
                    }
                }
            }

        } catch (Exception ex) {
            log.error("Fail to sync DAs", ex);
        }
    }

    private void getAgentFullStatus(DistributedApplianceInstance dai, List<ManagerDeviceMemberStatusElement> statusList) throws Exception {
        try {
            EntityManager em = HibernateUtil.getTransactionalEntityManager();
            HibernateUtil.getTransactionControl().required(() -> {
                ManagerDeviceMemberStatusElement memberStatus = findDeviceMemberStatus(dai, statusList);

                if (memberStatus != null) {
                    dai.setLastStatus(new Date());
                    if (memberStatus.getRx() != null) {
                        dai.setPackets(memberStatus.getRx());
                    }
                }

                OSCEntityManager.update(em, dai, StaticRegistry.transactionalBroadcastUtil());
                return null;
            });

        } catch (ScopedWorkException ex) {
            log.error("Fail to get full status for dai '" + dai.getName() + "'. " + ex.getCause().getMessage());
        } catch (Exception ex) {
            log.error("Fail to get full status for dai '" + dai.getName() + "'. " + ex.getMessage());
        }
    }

    private ManagerDeviceMemberStatusElement findDeviceMemberStatus(DistributedApplianceInstance dai, List<ManagerDeviceMemberStatusElement> statusList) {
        for (ManagerDeviceMemberStatusElement memberStatus : statusList) {
            if (memberStatus.getDistributedApplianceInstanceElement().getId().equals(dai.getId())) {
                return memberStatus;
            }
        }

        return null;
    }
}
