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

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

/**
 * ReleaseInfo Model: capture build and database versions.

 */

@Entity
@Table(name = "RELEASE_INFO", uniqueConstraints = @UniqueConstraint(columnNames = { "build_version", "db_version" }))
public class ReleaseInfo extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @Column(name = "db_version")
    private int dbVersion;

    @Column(name = "build_version")
    private String buildVersion;

    public ReleaseInfo() {
        super();
    }

    public int getDbVersion() {
        return dbVersion;
    }

    public void setDbVersion(int dbVersion) {
        this.dbVersion = dbVersion;
    }

    public String getBuildVersion() {
        return buildVersion;
    }

    public void setBuildVersion(String buildVersion) {
        this.buildVersion = buildVersion;
    }

    @Override
    public String toString() {
        return "ReleaseInfo [dbVersion=" + dbVersion + ", getId()=" + getId() + "]";
    }

}
