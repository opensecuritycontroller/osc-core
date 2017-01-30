package org.osc.core.broker.service;

import org.apache.log4j.Logger;
import org.hibernate.Session;
import org.osc.core.broker.job.Job;
import org.osc.core.broker.job.JobEngine;
import org.osc.core.broker.job.TaskGraph;
import org.osc.core.broker.model.entities.RoleType;
import org.osc.core.broker.model.entities.User;
import org.osc.core.broker.rest.server.AgentAuthFilter;
import org.osc.core.broker.rest.server.NsxAuthFilter;
import org.osc.core.broker.rest.server.VmidcAuthFilter;
import org.osc.core.broker.service.dto.DtoValidator;
import org.osc.core.broker.service.dto.UserDto;
import org.osc.core.broker.service.dto.UserDtoValidator;
import org.osc.core.broker.service.persistence.EntityManager;
import org.osc.core.broker.service.persistence.UserEntityMgr;
import org.osc.core.broker.service.request.UpdateUserRequest;
import org.osc.core.broker.service.response.UpdateUserResponse;
import org.osc.core.broker.service.tasks.passwordchange.PasswordChangePropagateDaiMetaTask;
import org.osc.core.broker.service.tasks.passwordchange.PasswordChangePropagateMgrMetaTask;
import org.osc.core.broker.service.tasks.passwordchange.PasswordChangePropagateNsxMetaTask;
import org.osc.core.util.EncryptionUtil;

import com.mcafee.vmidc.server.Server;

public class UpdateUserService extends ServiceDispatcher<UpdateUserRequest, UpdateUserResponse> {

    private static final Logger log = Logger.getLogger(UpdateUserService.class);
    private DtoValidator<UserDto, User> validator;

    @Override
    public UpdateUserResponse exec(UpdateUserRequest request, Session session) throws Exception {
        EntityManager<User> emgr = new EntityManager<User>(User.class, session);

        if (this.validator == null) {
            this.validator = new UserDtoValidator(session);
        }

        // validate and retrieve existing entity from the database.
        User user = this.validator.validateForUpdate(request);

        UserEntityMgr.toEntity(user, request);
        emgr.update(user);
        UpdateUserResponse response = new UpdateUserResponse();

        // If user changes password, need to reflect it in auth-filter objects
        if (user.getLoginName().equals(VmidcAuthFilter.VMIDC_DEFAULT_LOGIN)) {

            VmidcAuthFilter.VMIDC_DEFAULT_PASS = EncryptionUtil.decrypt(user.getPassword());
            user.setRole(RoleType.ADMIN);
            response.setJobId(startPasswordPropagateMgrJob());

        } else if (user.getLoginName().equals(AgentAuthFilter.VMIDC_AGENT_LOGIN)) {

            AgentAuthFilter.VMIDC_AGENT_PASS = EncryptionUtil.decrypt(user.getPassword());
            user.setRole(RoleType.SYSTEM_AGENT);
            response.setJobId(startPasswordPropagateDaiJob());

        } else if (user.getLoginName().equals(NsxAuthFilter.VMIDC_NSX_LOGIN)) {

            NsxAuthFilter.VMIDC_NSX_PASS = EncryptionUtil.decrypt(user.getPassword());
            user.setRole(RoleType.SYSTEM_NSX);
            response.setJobId(startPasswordPropagateNsxJob());
        }

        return response;
    }

    public static Long startPasswordPropagateNsxJob() throws Exception {

        log.info("Start propagating new password to all NSX managers");

        TaskGraph tg = new TaskGraph();

        tg.addTask(new PasswordChangePropagateNsxMetaTask());

        Job job = JobEngine.getEngine().submit("Update NSX manager(s) " + Server.SHORT_PRODUCT_NAME + " Password", tg,
                null);
        log.debug("Done submitting with jobId: " + job.getId());
        return job.getId();

    }

    public static Long startPasswordPropagateDaiJob() throws Exception {

        log.info("Start propagating new password to all DAIs");

        TaskGraph tg = new TaskGraph();

        tg.addTask(new PasswordChangePropagateDaiMetaTask());

        Job job = JobEngine.getEngine().submit(
                "Update " + Server.SHORT_PRODUCT_NAME + " Appliance Instance Agent(s) password", tg, null);
        log.info("Done submitting with jobId: " + job.getId());
        return job.getId();

    }

    public static Long startPasswordPropagateMgrJob() throws Exception {
        log.info("Start propagating new password to all managers");
        TaskGraph tg = new TaskGraph();
        tg.addTask(new PasswordChangePropagateMgrMetaTask());
        Job job = JobEngine.getEngine().submit("Update " + Server.SHORT_PRODUCT_NAME + " Password in manager(s)", tg,
                null);
        log.info("Done submitting with jobId: " + job.getId());
        return job.getId();
    }

}
