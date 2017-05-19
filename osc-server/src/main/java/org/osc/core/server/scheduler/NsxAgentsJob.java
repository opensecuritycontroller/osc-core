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

import java.util.List;

import javax.persistence.EntityManager;

import org.apache.log4j.Logger;
import org.osc.core.broker.model.entities.appliance.DistributedAppliance;
import org.osc.core.broker.model.entities.appliance.DistributedApplianceInstance;
import org.osc.core.broker.model.entities.appliance.VirtualSystem;
import org.osc.core.broker.model.plugin.ApiFactoryService;
import org.osc.core.broker.service.persistence.OSCEntityManager;
import org.osc.core.broker.util.db.HibernateUtil;
import org.osc.sdk.sdn.api.AgentApi;
import org.osc.sdk.sdn.element.AgentElement;
import org.osgi.service.transaction.control.ScopedWorkException;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

// TODO this class is not used
public class NsxAgentsJob implements Job {

    private static final Logger LOG = Logger.getLogger(NsxAgentsJob.class);

    public NsxAgentsJob() {

    }

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {

        try {
            ApiFactoryService apiFactoryService =  (ApiFactoryService) context.getMergedJobDataMap().get(ApiFactoryService.class.getName());
            EntityManager em = HibernateUtil.getTransactionalEntityManager();

            HibernateUtil.getTransactionControl().required(() -> {

                OSCEntityManager<DistributedAppliance> emgr = new OSCEntityManager<DistributedAppliance>(
                        DistributedAppliance.class, em);
                for (DistributedAppliance da : emgr.listAll()) {
                    for (VirtualSystem vs : da.getVirtualSystems()) {
                        AgentApi agentApi = apiFactoryService.createAgentApi(vs);
                        List<AgentElement> agents = agentApi.getAgents(vs.getNsxServiceId());

                        for (DistributedApplianceInstance dai : vs.getDistributedApplianceInstances()) {
                            AgentElement tgtAgent = null;
                            for (AgentElement agent : agents) {
                                if (agent.getIpAddress() == null) {
                                    continue;
                                }
                                if (agent.getIpAddress().equals(dai.getIpAddress())) {
                                    tgtAgent = agent;
                                    break;
                                }
                            }
                            if (tgtAgent == null) {
                                vs.removeDistributedApplianceInstance(dai);
                                OSCEntityManager.delete(em, dai);
                            } else {
                                dai.setNsxAgentId(tgtAgent.getId());
                                dai.setNsxHostId(tgtAgent.getHostId());
                                dai.setNsxHostName(tgtAgent.getHostName());
                                dai.setNsxVmId(tgtAgent.getVmId());
                                dai.setNsxHostVsmUuid(tgtAgent.getHostVsmId());
                                dai.setMgmtGateway(tgtAgent.getGateway());
                                dai.setMgmtSubnetPrefixLength(tgtAgent.getSubnetPrefixLength());

                                OSCEntityManager.update(em, dai);
                            }
                        }
                    }
                }
                return null;
            });
        } catch (ScopedWorkException ex) {
            LOG.error("Fail to sync DAs", ex.getCause());
        } catch (Exception ex) {
            LOG.error("Fail to sync DAs", ex);
        }
    }

}
