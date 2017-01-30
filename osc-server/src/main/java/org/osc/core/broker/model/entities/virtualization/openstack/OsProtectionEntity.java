package org.osc.core.broker.model.entities.virtualization.openstack;

import java.util.Set;

import org.osc.core.broker.model.entities.IscEntity;
import org.osc.core.broker.model.entities.virtualization.SecurityGroupMember;
import org.osc.core.broker.model.entities.virtualization.SecurityGroupMemberType;

/**
 * Objects which can be protected in an openstack environments by ISC need to comply to this interface
 */
public interface OsProtectionEntity extends IscEntity {

    String getName();

    String getRegion();

    String getOpenstackId();

    Set<SecurityGroupMember> getSecurityGroupMembers();

    SecurityGroupMemberType getType();
}
