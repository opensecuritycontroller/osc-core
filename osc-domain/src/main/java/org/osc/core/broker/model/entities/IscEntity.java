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
package org.osc.core.broker.model.entities;

import java.util.Date;

public interface IscEntity {

    /**
     * @return the markedForDeletion
     */
    Boolean getMarkedForDeletion();

    /**
     * @param markedForDeletion
     *            the markedForDeletion to set
     */
    void setMarkedForDeletion(Boolean markedForDeletion);

    /**
     * @return the createdTimestamp
     */
    Date getCreatedTimestamp();

    /**
     * @param createdTimestamp
     *            the createdTimestamp to set
     */
    void setCreatedTimestamp(Date createdTimestamp);

    /**
     * @return the updatedTimestamp
     */
    Date getUpdatedTimestamp();

    /**
     * @param updatedTimestamp
     *            the updatedTimestamp to set
     */
    void setUpdatedTimestamp(Date updatedTimestamp);

    /**
     * @return the deletedTimestamp
     */
    Date getDeletedTimestamp();

    /**
     * @param deletedTimestamp
     *            the deletedTimestamp to set
     */
    void setDeletedTimestamp(Date deletedTimestamp);

    /**
     * @return the createdBy
     */
    String getCreatedBy();

    /**
     * @param createdBy
     *            the createdBy to set
     */
    void setCreatedBy(String createdBy);

    /**
     * @return the updatedBy
     */
    String getUpdatedBy();

    /**
     * @param updatedBy
     *            the updatedBy to set
     */
    void setUpdatedBy(String updatedBy);

    /**
     * @return the deletedBy
     */
    String getDeletedBy();

    /**
     * @param deletedBy
     *            the deletedBy to set
     */
    void setDeletedBy(String deletedBy);

    /**
     * @return the id
     */
    Long getId();

    /**
     * @param id
     *            the id to set
     */
    void setId(Long id);

    /**
     * @return the version
     */
    Long getVersion();

    /**
     * @param version
     *            the version to set
     */
    void setVersion(Long version);

}
