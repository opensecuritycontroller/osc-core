/*******************************************************************************
 * Copyright (c) 2017 Intel Corporation
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
package org.osc.core.broker.service.alert;

import java.util.List;

import org.osc.core.broker.service.dto.BaseDto;
import org.osc.core.broker.service.request.BaseRequest;

public class AlertRequest extends BaseRequest<BaseDto> {

    private List<AlertDto> dtoList = null;
    private boolean acknowledge;

    public List<AlertDto> getDtoList() {
        return this.dtoList;
    }

    public void setDtoList(List<AlertDto> dtoList) {
        this.dtoList = dtoList;
    }

    public boolean isAcknowledge() {
        return this.acknowledge;
    }

    public void setAcknowledge(boolean acknowledge) {
        this.acknowledge = acknowledge;
    }
}
