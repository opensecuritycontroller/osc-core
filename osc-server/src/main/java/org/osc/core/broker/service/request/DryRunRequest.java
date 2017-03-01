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
package org.osc.core.broker.service.request;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.osc.core.broker.service.dto.BaseDto;
import org.osc.core.broker.service.request.ErrorTypeException.ErrorType;

/**
 * A request which allows you to specify if some errors should be ignored. The service defines which
 * errors can be ignored vs which ones need to be addressed.
 *
 * @param <T> the type of DTO object
 */
public class DryRunRequest<T extends BaseDto> extends BaseRequest<T> {

    private List<ErrorType> errorsToIgnore = new ArrayList<>();
    private boolean skipAllDryRun;

    public DryRunRequest() {
        super();
     }

    public DryRunRequest(T dto) {
       super(dto);
    }

    public DryRunRequest(T dto, boolean skipAllDryRun) {
        super(dto);
        this.skipAllDryRun = skipAllDryRun;
    }

    /**
     * Returns true if all the error types passed in are in the ignore list. False otherwise.
     * @param errorTypes the error types to check for
     *
     * @return Returns true if all the error types passed in are in the ignore list. False otherwise.
     */
    public boolean isIgnoreErrorsAndCommit(ErrorType...errorTypes) {
        if (errorTypes != null) {
            return this.errorsToIgnore.containsAll(Arrays.asList(errorTypes));
        }
        return false;
    }

    /**
     * The errors to ignore while processing the request. Send null to not ignore any errors
     *
     * @param errorTypes
     */
    public void addErrorsToIgnore(List<ErrorType> errorTypes) {
        if (errorTypes != null) {
            this.errorsToIgnore.addAll(errorTypes);
        }
    }

    public boolean isSkipAllDryRun() {
        return this.skipAllDryRun;
    }

    public void setSkipAllDryRun(boolean skipAllDryRun) {
        this.skipAllDryRun = skipAllDryRun;
    }

}
