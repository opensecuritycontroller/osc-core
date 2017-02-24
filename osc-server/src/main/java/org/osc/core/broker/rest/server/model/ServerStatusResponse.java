package org.osc.core.broker.rest.server.model;

import java.util.Date;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class ServerStatusResponse {
    private String version;
    private Date currentServerTime;
    private int dbVersion;
    private String pid;

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public Date getCurrentServerTime() {
        return currentServerTime;
    }

    public void setCurrentServerTime(Date currentServerTime) {
        this.currentServerTime = currentServerTime;
    }

    /**
     * @return the dbVersion
     */
    public int getDbVersion() {
        return dbVersion;
    }

    /**
     * @param dbVersion
     *            the dbVersion to set
     */
    public void setDbVersion(int dbVersion) {
        this.dbVersion = dbVersion;
    }

    public String getPid() {
        return pid;
    }
    public void setPid(String pid) {
        this.pid = pid;
    }
}
