package org.osc.core.broker.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.log4j.Logger;
import org.hibernate.Session;
import org.osc.core.broker.job.Job;
import org.osc.core.broker.job.JobEngine;
import org.osc.core.broker.job.TaskGraph;
import org.osc.core.broker.job.TaskNode;
import org.osc.core.broker.job.Job.JobCompletionListener;
import org.osc.core.broker.job.Job.TaskChangeListener;
import org.osc.core.broker.job.lock.LockObjectReference;
import org.osc.core.broker.model.entities.appliance.DistributedApplianceInstance;
import org.osc.core.broker.model.entities.appliance.VirtualSystem;
import org.osc.core.broker.model.entities.events.SystemFailureType;
import org.osc.core.broker.model.plugin.manager.ManagerApiFactory;
import org.osc.core.broker.service.alert.AlertGenerator;
import org.osc.core.broker.service.exceptions.VmidcBrokerValidationException;
import org.osc.core.broker.service.exceptions.VmidcException;
import org.osc.core.broker.service.persistence.DistributedApplianceInstanceEntityMgr;
import org.osc.core.broker.service.persistence.EntityManager;
import org.osc.core.broker.service.request.PropagateVSMgrFileRequest;
import org.osc.core.broker.service.response.BaseJobResponse;
import org.osc.core.broker.service.tasks.mgrfile.MgrFileChangePropagateTask;
import org.osc.core.broker.service.tasks.mgrfile.MgrFileChangePropagateToDaiTask;
import org.osc.sdk.manager.api.IscJobNotificationApi;

public class PropagateVSMgrFileService extends ServiceDispatcher<PropagateVSMgrFileRequest, BaseJobResponse> {

    private static final Logger log = Logger.getLogger(PropagateVSMgrFileService.class);

    @Override
    public BaseJobResponse exec(PropagateVSMgrFileRequest request, Session session) throws Exception {

        BaseJobResponse response = new BaseJobResponse();

        List<DistributedApplianceInstance> daiList = validate(session, request);

        Long jobId = startMgrfilePropagateJob(request, daiList);
        response.setJobId(jobId);

        return response;
    }

    private List<DistributedApplianceInstance> validate(Session session, PropagateVSMgrFileRequest request)
            throws Exception {

        String vsName = request.getVsName();

        if (vsName == null || vsName.isEmpty()) {
            throw new VmidcBrokerValidationException("Invalid Virtual System Name.");
        }

        EntityManager<VirtualSystem> emgr = new EntityManager<VirtualSystem>(VirtualSystem.class, session);
        Long vsId = VirtualSystem.getVsIdFromName(vsName);
        VirtualSystem vs = emgr.findByPrimaryKey(vsId);

        if (vs == null) {
            throw new VmidcBrokerValidationException("Virtual System with name: " + vsName + " not found.");
        }

        if (request.getMgrFileName() == null || request.getMgrFileName().isEmpty()) {
            throw new VmidcBrokerValidationException("Invalid Manager file name.");
        }

        if (request.getMgrFile() == null) {
            throw new VmidcBrokerValidationException("Invalid Manager File content.");
        }

        List<DistributedApplianceInstance> daiList = new ArrayList<DistributedApplianceInstance>();
        if (request.getDaiList() != null && !request.getDaiList().isEmpty()) {

            EntityManager<DistributedApplianceInstance> daiEmgr = new EntityManager<DistributedApplianceInstance>(
                    DistributedApplianceInstance.class, session);

            for (String daiName : request.getDaiList()) {
                DistributedApplianceInstance dai = daiEmgr.findByFieldName("name", daiName);

                if (dai != null) {
                    if (!dai.getVirtualSystem().equals(vs)) {
                        throw new VmidcException("DAI '" + daiName + "' is not a member of VSS '" + vs.getName() + "'.");
                    }
                    daiList.add(dai);
                } else {
                    throw new VmidcException("DAI '" + daiName + "' not found.");
                }
            }

        } else {

            daiList = DistributedApplianceInstanceEntityMgr.listByVsId(session, vsId);
            if (daiList == null || daiList.isEmpty()) {
                throw new VmidcException("VSS '" + vs.getName() + "' does not have members.");
            }

        }

        return daiList;
    }

    private Long startMgrfilePropagateJob(PropagateVSMgrFileRequest req, List<DistributedApplianceInstance> daiList)
            throws Exception {

        TaskGraph tg = new TaskGraph();

        final VirtualSystem vs = daiList.get(0).getVirtualSystem();
        tg.addTask(new MgrFileChangePropagateTask(req.getMgrFileName(), req.getMgrFile(), daiList));

        IscJobNotificationApi mgrApi = ManagerApiFactory.createIscJobNotificationApi(vs);

        Job job = null;

        if (mgrApi == null) {
            job = JobEngine.getEngine().submit(
                    "Propagating Manager File '" + req.getMgrFileName() + "' to DAIs for Virtual System: '"
                            + req.getVsName() + "'", tg, LockObjectReference.getObjectReferences(vs));
        } else {

            job = JobEngine.getEngine().submit(
                    "Propagating Manager File '" + req.getMgrFileName() + "' to DAIs for Virtual System: '"
                            + req.getVsName() + "'", tg, LockObjectReference.getObjectReferences(vs),
                    new JobCompletionListener() {

                        @Override
                        public void completed(Job job) {
                            IscJobNotificationApi mgrApi = null;
                            try {
                                mgrApi = ManagerApiFactory.createIscJobNotificationApi(vs);
                                mgrApi.reportJobEvent(job);
                            } catch (Exception e) {
                                log.error("Failed to report job event", e);
                            } finally {
                                if (mgrApi != null) {
                                    mgrApi.close();
                                }
                            }
                        }
                    }, new TaskChangeListener() {

                        @Override
                        public void taskChanged(TaskNode taskNode) {
                            IscJobNotificationApi mgrApi = null;
                            try {
                                mgrApi = ManagerApiFactory.createIscJobNotificationApi(vs);
                                
                                if (!taskNode.getState().isTerminalState()) {
                                    return;
                                }

                                if (taskNode.getTask() instanceof MgrFileChangePropagateToDaiTask) {
                                    MgrFileChangePropagateToDaiTask task = (MgrFileChangePropagateToDaiTask) taskNode
                                            .getTask();

                                    HashMap<String, String> objects = new HashMap<>();
                                    objects.put("DAI", task.getDai().getName());

                                    mgrApi.reportJobTaskEvent(taskNode, objects);
                                }

                            } catch (Exception e) {
                                log.error("Failed to report job task event", e);
                                AlertGenerator.processSystemFailureEvent(
                                        SystemFailureType.MGR_PROPAGATION_JOB_NOTIFCATION, new LockObjectReference(vs
                                                .getDistributedAppliance().getApplianceManagerConnector()),
                                        "Manager reported an error '" + e.getMessage()
                                                + "' while notifying Task completion (job Id:"
                                                + taskNode.getJob().getId() + "'");
                            } finally {
                                if (mgrApi != null) {
                                    mgrApi.close();
                                }
                            }
                            
                        }
                    });
        }

        return job.getId();
    }
}
