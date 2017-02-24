package org.osc.core.broker.service.persistence;

import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.osc.core.broker.model.entities.virtualization.SecurityGroup;
import org.osc.core.broker.model.entities.virtualization.SecurityGroupMember;
import org.osc.core.broker.model.entities.virtualization.SecurityGroupMemberType;
import org.osc.core.broker.model.entities.virtualization.openstack.Network;
import org.osc.core.broker.model.entities.virtualization.openstack.Subnet;
import org.osc.core.broker.model.entities.virtualization.openstack.VM;
import org.osc.core.broker.service.securitygroup.SecurityGroupMemberItemDto;

public class SecurityGroupMemberEntityMgr {

    public static void fromEntity(SecurityGroupMember entity, SecurityGroupMemberItemDto dto) {
        SecurityGroupMemberType type = entity.getType();

        dto.setId(entity.getId());
        dto.setType(type);
        if (type == SecurityGroupMemberType.VM) {

            VM vm = entity.getVm();
            dto.setName(vm.getName());
            dto.setOpenstackId(vm.getOpenstackId());
            dto.setRegion(vm.getRegion());
        } else if (type == SecurityGroupMemberType.NETWORK) {

            Network nw = entity.getNetwork();
            dto.setName(nw.getName());
            dto.setOpenstackId(nw.getOpenstackId());
            dto.setRegion(nw.getRegion());

        } else if (type == SecurityGroupMemberType.SUBNET) {

            Subnet subnet = entity.getSubnet();
            dto.setName(subnet.getName());
            dto.setOpenstackId(subnet.getOpenstackId());
            dto.setRegion(subnet.getRegion());
            dto.setParentOpenStackId(subnet.getNetworkId());
            dto.setProtectExternal(subnet.isProtectExternal());

        }

    }

    @SuppressWarnings("unchecked")
    public static List<SecurityGroupMember> listActiveSecurityGroupMembersBySecurityGroup(Session session,
            SecurityGroup sg) {
        Criteria criteria = session.createCriteria(SecurityGroupMember.class)
                .add(Restrictions.eq("markedForDeletion", false)).add(Restrictions.eq("securityGroup", sg))
                .addOrder(Order.asc("type")).setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY);
        return criteria.list();
    }

}
