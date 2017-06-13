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

import java.io.Serializable;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;
import javax.persistence.Version;

@MappedSuperclass
public class BaseEntity implements Serializable, IscEntity {
    private static final long serialVersionUID = 1L;

    // Hibernate 5 requires a sequence for AUTO generation. Using IDENTITY
    // avoids this having to be created
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Id
    @Column(name = "id")
    private Long id;

    @Column(name = "created_timestamp")
    private Date createdTimestamp;
    @Column(name = "updated_timestamp")
    private Date updatedTimestamp;
    @Column(name = "deleted_timestamp")
    private Date deletedTimestamp;

    @Column(name = "created_by")
    private String createdBy;
    @Column(name = "updated_by")
    private String updatedBy;
    @Column(name = "deleted_by")
    private String deletedBy;

    @Column(name = "marked_for_deletion")
    private Boolean markedForDeletion = false;

    @Version
    private Long version; // used for Database Optimistic Locking mechanism

    public BaseEntity() {

    }

    public BaseEntity(BaseEntity originalBe) {
        this.id = originalBe.id;
        this.createdTimestamp = originalBe.createdTimestamp;
        this.updatedTimestamp = originalBe.updatedTimestamp;
        this.deletedTimestamp = originalBe.deletedTimestamp;
        this.createdBy = originalBe.createdBy;
        this.updatedBy = originalBe.updatedBy;
        this.deletedBy = originalBe.deletedBy;
        this.markedForDeletion = originalBe.markedForDeletion;
        this.version = originalBe.version;
    }

    /**
     * @return the markedForDeletion
     */
    @Override
    public Boolean getMarkedForDeletion() {
        return this.markedForDeletion;
    }

    /**
     * @param markedForDeletion
     *            the markedForDeletion to set
     */
    @Override
    public void setMarkedForDeletion(Boolean markedForDeletion) {
        this.markedForDeletion = markedForDeletion;
    }

    /**
     * @return the createdTimestamp
     */
    @Override
    public Date getCreatedTimestamp() {
        return this.createdTimestamp;
    }

    /**
     * @param createdTimestamp
     *            the createdTimestamp to set
     */
    @Override
    public void setCreatedTimestamp(Date createdTimestamp) {
        this.createdTimestamp = createdTimestamp;
    }

    /**
     * @return the updatedTimestamp
     */
    @Override
    public Date getUpdatedTimestamp() {
        return this.updatedTimestamp;
    }

    /**
     * @param updatedTimestamp
     *            the updatedTimestamp to set
     */
    @Override
    public void setUpdatedTimestamp(Date updatedTimestamp) {
        this.updatedTimestamp = updatedTimestamp;
    }

    /**
     * @return the deletedTimestamp
     */
    @Override
    public Date getDeletedTimestamp() {
        return this.deletedTimestamp;
    }

    /**
     * @param deletedTimestamp
     *            the deletedTimestamp to set
     */
    @Override
    public void setDeletedTimestamp(Date deletedTimestamp) {
        this.deletedTimestamp = deletedTimestamp;
    }

    /**
     * @return the createdBy
     */
    @Override
    public String getCreatedBy() {
        return this.createdBy;
    }

    /**
     * @param createdBy
     *            the createdBy to set
     */
    @Override
    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    /**
     * @return the updatedBy
     */
    @Override
    public String getUpdatedBy() {
        return this.updatedBy;
    }

    /**
     * @param updatedBy
     *            the updatedBy to set
     */
    @Override
    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
    }

    /**
     * @return the deletedBy
     */
    @Override
    public String getDeletedBy() {
        return this.deletedBy;
    }

    /**
     * @param deletedBy
     *            the deletedBy to set
     */
    @Override
    public void setDeletedBy(String deletedBy) {
        this.deletedBy = deletedBy;
    }


    /**
     * @return the id
     */
    @Override
    public Long getId() {
        return this.id;
    }

    /**
     * @param id
     *            the id to set
     */
    @Override
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * @return the version
     */
    @Override
    public Long getVersion() {
        return this.version;
    }

    /**
     * @param version
     *            the version to set
     */
    @Override
    public void setVersion(Long version) {
        this.version = version;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        if (getId() != null) {
            hash += getId().hashCode();
        } else {
            return super.hashCode();
        }
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        if(getId() == null) {
            return super.equals(object);
        }
        if (this == object) {
            return true;
        }
        if (object == null) {
            return false;
        }
        if (getClass() != object.getClass()) {
            return false;
        }

        BaseEntity other = (BaseEntity) object;
        if (!this.id.equals(other.id)) {
            return false;
        }
        return true;
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "BaseEntity [id=" + this.id + ", createdTimestamp=" + this.createdTimestamp + ", updatedTimestamp="
                + this.updatedTimestamp + ", deletedTimestamp=" + this.deletedTimestamp + ", createdBy=" + this.createdBy
                + ", updatedBy=" + this.updatedBy + ", deletedBy=" + this.deletedBy + "]";
    }
}
