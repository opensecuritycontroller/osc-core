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
