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

import java.util.HashMap;
import java.util.Map;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

import org.osc.core.broker.service.dto.BaseDto;
import org.osc.core.broker.util.ValidateUtil;

@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class BaseIdRequest extends BaseRequest<BaseDto> {
    private Long id;
    private Long parentId;

    public BaseIdRequest(long id, long parentId) {
        this.id = id;
        this.parentId = parentId;
    }

    public BaseIdRequest(long id) {
        this.id = id;
        this.parentId = null; // parent unassigned
    }

    public BaseIdRequest() {
        this.id = null; // id unassigned
        this.parentId = null; // parent unassigned
    }

    public Long getId() {
        return this.id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public Long getParentId() {
        return this.parentId;
    }

    public void setParentId(long parentId) {
        this.parentId = parentId;
    }

    public static void checkForNullId(BaseIdRequest req) throws Exception {

        // build a map of (field,value) pairs to be checked for null/empty
        // values
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("Id", req.getId());
        ValidateUtil.checkForNullFields(map);
    }

    public static void checkForNullIdAndParentNullId(BaseIdRequest req) throws Exception {

        // build a map of (field,value) pairs to be checked for null/empty
        // values
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("Id", req.getId());
        map.put("Parent Id", req.getParentId());
        ValidateUtil.checkForNullFields(map);
    }

}
