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
package org.osc.core.broker.service.persistence;

import java.util.HashSet;
import java.util.Set;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

import org.osc.core.broker.model.entities.management.ApplianceManagerConnector;
import org.osc.core.broker.model.entities.management.Policy;
import org.osc.core.broker.service.dto.PolicyDto;
import org.osc.core.broker.service.exceptions.VmidcBrokerValidationException;

public class PolicyEntityMgr {

	public static void fromEntity(Policy entity, PolicyDto dto) {
		dto.setId(entity.getId());
		dto.setPolicyName(entity.getName());
		dto.setMgrPolicyId(entity.getMgrPolicyId());
		dto.setMgrDomainId(entity.getDomain().getId());
		dto.setMgrDomainName(entity.getDomain().getName());
	}

	public static Policy findById(EntityManager em, Long id) {
		return em.find(Policy.class, id);
	}

	/**
	 * Verifies if the request contains valid policies supported by security manager available on the OSC.
	 * If the request contains one or more invalid policies, throw an exception.
	 */
	// TODO Larkins: Improve the method not to do the validation
	public static Set<Policy> findPoliciesById(EntityManager em, Set<Long> ids, ApplianceManagerConnector mc)
			throws VmidcBrokerValidationException, Exception {
		Set<Policy> policies = new HashSet<>();
		Set<String> invalidPolicies = new HashSet<>();
		for (Long id : ids) {
			CriteriaBuilder cb = em.getCriteriaBuilder();
			CriteriaQuery<Policy> query = cb.createQuery(Policy.class);
			Root<Policy> root = query.from(Policy.class);
			query = query.select(root).where(cb.equal(root.get("id"), id),
					cb.equal(root.join("applianceManagerConnector").get("id"), mc.getId()));
			try {
				Policy policy = em.createQuery(query).getSingleResult();
				policies.add(policy);
			} catch (NoResultException nre) {
				invalidPolicies.add(id.toString());
			}
		}
		if (invalidPolicies.size() > 0) {
			throw new VmidcBrokerValidationException(
					"Invalid Request. Request contains invalid policies: " + String.join(", ", invalidPolicies));
		}
		return policies;
	}
}
