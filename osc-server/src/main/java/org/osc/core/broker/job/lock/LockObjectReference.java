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
package org.osc.core.broker.job.lock;

import java.util.HashSet;
import java.util.Set;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

import org.osc.core.broker.job.Job;
import org.osc.core.broker.model.entities.BaseEntity;
import org.osc.core.broker.model.entities.appliance.DistributedAppliance;
import org.osc.core.broker.model.entities.appliance.DistributedApplianceInstance;
import org.osc.core.broker.model.entities.appliance.VirtualSystem;
import org.osc.core.broker.model.entities.management.ApplianceManagerConnector;
import org.osc.core.broker.model.entities.virtualization.SecurityGroup;
import org.osc.core.broker.model.entities.virtualization.SecurityGroupInterface;
import org.osc.core.broker.model.entities.virtualization.VirtualizationConnector;
import org.osc.core.broker.model.entities.virtualization.openstack.DeploymentSpec;
import org.osc.core.broker.service.dto.SecurityGroupInterfaceDto;

import io.swagger.annotations.ApiModelProperty;

@XmlRootElement(name = "objectReference")
@XmlAccessorType(XmlAccessType.FIELD)
public class LockObjectReference {

    public enum ObjectType {
        VIRTUALIZATION_CONNECTOR("Virtualization Connector"),
        APPLIANCE_MANAGER_CONNECTOR("Manager Connector"),
        DISTRIBUTED_APPLIANCE("Distributed Appliance"),
        VIRTUAL_SYSTEM("Virtual System"),
        DEPLOYMENT_SPEC("Deployment Specification"),
        DISTRIBUTED_APPLIANCE_INSTANCE("Distributed Appliance Instance"),
        SECURITY_GROUP("Security Group"),
        SECURITY_GROUP_INTERFACE("Security Group Interface"),
        SSL_CONFIGURATION("SSL Configuration"),
        JOB("Job"),
        EMAIL(""),
        NETWORK(""),
        ARCHIVE(""),
        ALERT("Alert");

        private String name;

        private ObjectType(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return this.name;
        }
    }

    @ApiModelProperty(required = true)
    private Long id;

    @ApiModelProperty(required = true)
    private String name;

    @ApiModelProperty(required = true)
    private ObjectType type;

    LockObjectReference() {
    }

    public LockObjectReference(DistributedAppliance da) {
        this(da.getId(), da.getName(), ObjectType.DISTRIBUTED_APPLIANCE);
    }

    public LockObjectReference(VirtualizationConnector vc) {
        this(vc.getId(), vc.getName(), ObjectType.VIRTUALIZATION_CONNECTOR);
    }

    public LockObjectReference(ApplianceManagerConnector mc) {
        this(mc.getId(), mc.getName(), ObjectType.APPLIANCE_MANAGER_CONNECTOR);
    }

    public LockObjectReference(VirtualSystem vs) {
        this(vs.getId(), vs.getName(), LockObjectReference.ObjectType.VIRTUAL_SYSTEM);
    }

    public LockObjectReference(DeploymentSpec ds) {
        this(ds.getId(), ds.getName(), LockObjectReference.ObjectType.DEPLOYMENT_SPEC);
    }

    public LockObjectReference(DistributedApplianceInstance dai) {
        this(dai.getId(), dai.getName(), LockObjectReference.ObjectType.DISTRIBUTED_APPLIANCE_INSTANCE);
    }

    public LockObjectReference(SecurityGroup sg) {
        this(sg.getId(), sg.getName(), LockObjectReference.ObjectType.SECURITY_GROUP);
    }

    public LockObjectReference(SecurityGroupInterface sgi) {
        this(sgi.getId(), sgi.getName(), LockObjectReference.ObjectType.SECURITY_GROUP_INTERFACE);
    }

    public LockObjectReference(SecurityGroupInterfaceDto sgi) {
        this(sgi.getId(), sgi.getName(), LockObjectReference.ObjectType.SECURITY_GROUP_INTERFACE);
    }

    public LockObjectReference(Job job) {
        this(job.getId(), job.getName(), LockObjectReference.ObjectType.JOB);
    }

    public static Set<LockObjectReference> getObjectReferences(BaseEntity... objects) {
        if (objects == null) {
            return null;
        }

        Set<LockObjectReference> references = new HashSet<LockObjectReference>();
        for (BaseEntity o : objects) {
            LockObjectReference ref = getLockObjectReference(o);
            if (ref != null) {
                references.add(ref);
            }
        }
        return references;
    }

    /**
     * @param Base
     *            Entity object
     * @param defaultLockObject
     *            if entity is unknown returns defaultLockObject
     * @return
     *         new LockObjectReference or defaultObjectReference
     */
    public static LockObjectReference getLockObjectReference(BaseEntity o, LockObjectReference defaultLockObject) {
        LockObjectReference ref = getLockObjectReference(o);
        if (ref == null) {
            return defaultLockObject;
        }
        return ref;
    }

    /**
     * @param Base
     *            Entity object
     * @return
     *         new Lock Object Reference or null
     */
    public static LockObjectReference getLockObjectReference(BaseEntity o) {
        if (o instanceof DistributedAppliance) {
            return new LockObjectReference((DistributedAppliance) o);
        } else if (o instanceof VirtualizationConnector) {
            return new LockObjectReference((VirtualizationConnector) o);
        } else if (o instanceof ApplianceManagerConnector) {
            return new LockObjectReference((ApplianceManagerConnector) o);
        } else if (o instanceof VirtualSystem) {
            return new LockObjectReference((VirtualSystem) o);
        } else if (o instanceof DeploymentSpec) {
            return new LockObjectReference((DeploymentSpec) o);
        } else if (o instanceof SecurityGroup) {
            return new LockObjectReference((SecurityGroup) o);
        } else if (o instanceof SecurityGroupInterface) {
            return new LockObjectReference((SecurityGroupInterface) o);
        } else if (o instanceof DistributedApplianceInstance) {
            return new LockObjectReference((DistributedApplianceInstance) o);
        }
        return null;
    }

    public LockObjectReference(Long id, String name, ObjectType type) {
        super();
        this.id = id;
        this.name = name;
        this.type = type;
    }

    public Long getId() {
        return this.id;
    }

    public String getName() {
        return this.name;
    }

    public ObjectType getType() {
        return this.type;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (this.id == null ? 0 : this.id.hashCode());
        result = prime * result + (this.type == null ? 0 : this.type.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof LockObjectReference)) {
            return false;
        }
        LockObjectReference other = (LockObjectReference) obj;
        if (this.id == null) {
            if (other.id != null) {
                return false;
            }
        } else if (!this.id.equals(other.id)) {
            return false;
        }
        if (this.type != other.type) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "LockObjectReference [id=" + this.id + ", type=" + this.type + "]";
    }

}
