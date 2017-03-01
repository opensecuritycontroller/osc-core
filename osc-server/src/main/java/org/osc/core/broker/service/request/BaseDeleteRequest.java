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

public class BaseDeleteRequest extends BaseIdRequest {
    private boolean forceDelete = false; // default false

    public BaseDeleteRequest(long id, long parentId, boolean forceDelete) {
        super(id, parentId);
        this.forceDelete = forceDelete;
    }

    public BaseDeleteRequest(long id) {
        super(id);
    }

    public BaseDeleteRequest(long id, boolean forceDelete) {
        super(id);
        this.forceDelete = forceDelete;
    }

    public BaseDeleteRequest() {
        super();
    }

    public boolean isForceDelete() {
        return this.forceDelete;
    }

    public void setForceDelete(boolean isForceDeleted) {
        this.forceDelete = isForceDeleted;
    }

}
