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

import org.apache.log4j.Logger;
import org.osc.core.broker.job.Job;
import org.osc.core.broker.job.JobEngine;
import org.osc.core.broker.job.TaskGraph;
import org.osc.core.broker.model.entities.RoleType;
import org.osc.core.broker.model.entities.User;
import org.osc.core.broker.rest.server.NsxAuthFilter;
import org.osc.core.broker.rest.server.OscAuthFilter;
import org.osc.core.broker.service.dto.UserDto;
import org.osc.core.broker.service.persistence.OSCEntityManager;
import org.osc.core.broker.service.persistence.UserEntityMgr;
import org.osc.core.broker.service.request.UpdateUserRequest;
import org.osc.core.broker.service.response.UpdateUserResponse;
import org.osc.core.broker.service.tasks.passwordchange.PasswordChangePropagateDaiMetaTask;
import org.osc.core.broker.service.tasks.passwordchange.PasswordChangePropagateMgrMetaTask;
import org.osc.core.broker.service.tasks.passwordchange.PasswordChangePropagateNsxMetaTask;
import org.osc.core.broker.service.validator.DtoValidator;
import org.osc.core.broker.service.validator.UserDtoValidator;
import org.osc.core.util.EncryptionUtil;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import com.mcafee.vmidc.server.Server;

@Component(service = UpdateUserService.class)
public class UpdateUserService extends ServiceDispatcher<UpdateUserRequest, UpdateUserResponse> {

    private static final Logger log = Logger.getLogger(UpdateUserService.class);
    private DtoValidator<UserDto, User> validator;

    @Reference
    private PasswordChangePropagateNsxMetaTask passwordChangePropagateNsxMetaTask;

    @Override
    public UpdateUserResponse exec(UpdateUserRequest request, EntityManager em) throws Exception {
        OSCEntityManager<User> emgr = new OSCEntityManager<User>(User.class, em);

        if (this.validator == null) {
            this.validator = new UserDtoValidator(em);
        }

        // validate and retrieve existing entity from the database.
        User user = this.validator.validateForUpdate(request);

        UserEntityMgr.toEntity(user, request);
        emgr.update(user);
        UpdateUserResponse response = new UpdateUserResponse();

        // If user changes password, need to reflect it in auth-filter objects
        if (user.getLoginName().equals(OscAuthFilter.OSC_DEFAULT_LOGIN)) {

            OscAuthFilter.OSC_DEFAULT_PASS = EncryptionUtil.decryptAESCTR(user.getPassword());
            user.setRole(RoleType.ADMIN);
            response.setJobId(startPasswordPropagateMgrJob());

        } else if (user.getLoginName().equals(NsxAuthFilter.VMIDC_NSX_LOGIN)) {

            NsxAuthFilter.VMIDC_NSX_PASS = EncryptionUtil.decryptAESCTR(user.getPassword());
            user.setRole(RoleType.SYSTEM_NSX);
            response.setJobId(startPasswordPropagateNsxJob());
        }

        return response;
    }

    public Long startPasswordPropagateNsxJob() throws Exception {

        log.info("Start propagating new password to all NSX managers");

        TaskGraph tg = new TaskGraph();

        tg.addTask(this.passwordChangePropagateNsxMetaTask);

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
