/*******************************************************************************
 * Copyright (c) 2017 Intel Corporation
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
package org.osc.core.broker.util;

import static org.mockito.Matchers.*;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.mockito.ArgumentMatcher;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.osc.core.broker.model.entities.IscEntity;
import org.osc.core.broker.model.entities.appliance.Appliance;
import org.osc.core.broker.model.entities.appliance.ApplianceSoftwareVersion;
import org.osc.core.broker.model.entities.appliance.DistributedApplianceInstance;
import org.osc.core.broker.model.entities.appliance.VirtualSystem;
import org.osc.core.broker.model.entities.appliance.VirtualSystemPolicy;
import org.osc.core.broker.model.virtualization.VirtualizationType;
import org.osc.core.test.util.TestCriteria;
import org.osc.core.test.util.TestCriteriaContent;

public class SessionStub {
    public Session mock;
    private TestCriteria criteria;
    private Map<TestCriteriaContent, Object> criteriaResultMap;

    public SessionStub(Session sessionMock) {
        this.mock = sessionMock;
        this.criteria = Mockito.spy(TestCriteria.class);
        this.criteriaResultMap = new HashMap<TestCriteriaContent, Object>();
        when(this.mock.createCriteria(any(Class.class))).thenReturn(this.criteria);
        when(this.mock.createCriteria(any(Class.class), anyString())).thenReturn(this.criteria);
    }

    public void stubIsExistingEntity(Class<?> classType, String entityClassFieldName, String fieldValue, boolean exists) {
        Long result = exists ? 1L : 0L;
        TestCriteria expectedCriteria = new TestCriteria();
        expectedCriteria.add(Restrictions.eq(entityClassFieldName, fieldValue).ignoreCase());
        expectedCriteria.setProjection(Projections.rowCount());
        expectedCriteria.setFirstResult(0);
        expectedCriteria.setMaxResults(1);
        this.criteriaResultMap.put(expectedCriteria.getCriteriaContent(), result);

        doAnswer(new CriteriaAnswer()).when(this.criteria).uniqueResult();
    }

    public void stubFindByFieldName(String entityClassFieldName, String fieldValue, Object returnValue) {
        TestCriteria expectedCriteria = new TestCriteria();
        expectedCriteria.add(Restrictions.eq(entityClassFieldName, fieldValue).ignoreCase());
        expectedCriteria.setFirstResult(0);
        expectedCriteria.setMaxResults(1);

        List<Object> result = new ArrayList<Object>();
        if (returnValue != null) {
            result.add(returnValue);
        }

        this.criteriaResultMap.put(expectedCriteria.getCriteriaContent(), result);

        doAnswer(new CriteriaAnswer()).when(this.criteria).list();
    }

    public void stubFindApplianceByModel(String model, Appliance returnValue) {
        TestCriteria expectedCriteria = new TestCriteria();
        expectedCriteria.add(Restrictions.eq("model", model).ignoreCase());
        this.criteriaResultMap.put(expectedCriteria.getCriteriaContent(), returnValue);

        doAnswer(new CriteriaAnswer()).when(this.criteria).uniqueResult();
    }

    public void stubFindApplianceSoftwareVersionByImageUrl(String imageUrl, ApplianceSoftwareVersion returnValue) {
        TestCriteria expectedCriteria = new TestCriteria();
        expectedCriteria.add(Restrictions.eq("imageUrl", imageUrl));
        expectedCriteria.setFirstResult(0);
        expectedCriteria.setMaxResults(1);
        this.criteriaResultMap.put(expectedCriteria.getCriteriaContent(), returnValue);

        doAnswer(new CriteriaAnswer()).when(this.criteria).uniqueResult();
    }

    public void stubFindApplianceSoftwareVersion(Long applianceId, String version, VirtualizationType vt,
            String virtualizationVersion, ApplianceSoftwareVersion returnValue) {
        TestCriteria expectedCriteria = new TestCriteria();
        expectedCriteria.createAlias("appliance", "a");
        expectedCriteria.add(Restrictions.eq("a.id", applianceId));
        expectedCriteria.add(Restrictions.eq("applianceSoftwareVersion", version).ignoreCase());
        expectedCriteria.add(Restrictions.eq("virtualizationType", vt));
        expectedCriteria.add(Restrictions.eq("virtualizationSoftwareVersion", virtualizationVersion).ignoreCase());
        expectedCriteria.setFirstResult(0);
        expectedCriteria.setMaxResults(1);
        this.criteriaResultMap.put(expectedCriteria.getCriteriaContent(), returnValue);

        doAnswer(new CriteriaAnswer()).when(this.criteria).uniqueResult();
    }

    public void stubListVSPolicyByPolicyID(Long policyId, List<VirtualSystemPolicy> returnValue) {
        TestCriteria expectedCriteria = new TestCriteria();
        expectedCriteria.createAlias("policy", "pol");
        expectedCriteria.add(Restrictions.eq("pol.id", policyId).ignoreCase());
        expectedCriteria.setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY);

        this.criteriaResultMap.put(expectedCriteria.getCriteriaContent(), returnValue);

        doAnswer(new CriteriaAnswer()).when(this.criteria).list();
    }

    public void stubFindVirtualSystem(Long applianceId, Long virtualizationConnectorId, VirtualSystem returnValue) {
        TestCriteria expectedCriteria = new TestCriteria();
        expectedCriteria.createAlias("virtualizationConnector", "vc");
        expectedCriteria.createAlias("distributedAppliance", "da");
        expectedCriteria.add(Restrictions.eq("vc.id", virtualizationConnectorId));
        expectedCriteria.add(Restrictions.eq("da.id", applianceId));
        expectedCriteria.setFirstResult(0);
        expectedCriteria.setMaxResults(1);

        List<VirtualSystem> result = new ArrayList<VirtualSystem>();
        if (returnValue != null) {
            result.add(returnValue);
        }

        this.criteriaResultMap.put(expectedCriteria.getCriteriaContent(), result);

        doAnswer(new CriteriaAnswer()).when(this.criteria).list();
    }

    public void stubListByDaId(Long daId, List<Long> returnValue) {
        TestCriteria expectedCriteria = new TestCriteria();
        expectedCriteria.createAlias("dai.virtualSystem", "vs");
        expectedCriteria.createAlias("vs.distributedAppliance", "da");
        expectedCriteria.add(Restrictions.eq("da.id", daId));
        expectedCriteria.setProjection(Projections.property("id"));
        expectedCriteria.setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY);

        this.criteriaResultMap.put(expectedCriteria.getCriteriaContent(), returnValue);

        doAnswer(new CriteriaAnswer()).when(this.criteria).list();
    }

    public void listByDsIdAndAvailabilityZone(Long dsId, String azName, List<DistributedApplianceInstance> returnValue) {
        TestCriteria expectedCriteria = new TestCriteria();
        expectedCriteria.createAlias("dai.deploymentSpec", "ds");
        expectedCriteria.add(Restrictions.eq("ds.id", dsId));
        expectedCriteria.add(Restrictions.eq("osAvailabilityZone", azName));
        expectedCriteria.setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY);

        this.criteriaResultMap.put(expectedCriteria.getCriteriaContent(), returnValue);

        doAnswer(new CriteriaAnswer()).when(this.criteria).list();
    }

    public void stubListByVsId(Long vsId, List<DistributedApplianceInstance> returnValue) {
        TestCriteria expectedCriteria = new TestCriteria();
        expectedCriteria.createAlias("dai.virtualSystem", "vs");
        expectedCriteria.add(Restrictions.eq("vs.id", vsId));
        expectedCriteria.setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY);

        this.criteriaResultMap.put(expectedCriteria.getCriteriaContent(), returnValue);

        doAnswer(new CriteriaAnswer()).when(this.criteria).list();
    }

    public void stubListReferencedVSBySecurityGroup(Long sdId, List<VirtualSystem> returnValue) {
        TestCriteria expectedCriteria = new TestCriteria();
        expectedCriteria.createAlias("securityGroupInterfaces", "sgi");
        expectedCriteria.createAlias("sgi.securityGroups", "sg");
        expectedCriteria.add(Restrictions.eq("sg.id", sdId));
        expectedCriteria.setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY);

        this.criteriaResultMap.put(expectedCriteria.getCriteriaContent(), returnValue);

        doAnswer(new CriteriaAnswer()).when(this.criteria).list();
    }

    public void stubSaveEntity(ArgumentMatcher<Object> matcher, final Long id) {
        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) {
                Object[] args = invocation.getArguments();
                ((IscEntity)args[0]).setId(id);
                return null;
            }
        }).when(this.mock).save(argThat(matcher));
    }

    private class CriteriaAnswer implements Answer<Object> {

        @Override
        public Object answer(InvocationOnMock invocation) {
            TestCriteria criteria = (TestCriteria)invocation.getMock();
            TestCriteriaContent criteriaContent = criteria.getCriteriaContent();
            Object result = null;
            // If the criteria matches the expected criteria
            // then return the desired result.
            if (SessionStub.this.criteriaResultMap.containsKey(criteriaContent)) {
                result = SessionStub.this.criteriaResultMap.get(criteriaContent);
            }

            criteria.clearCriteria();
            return result;
        }
    }
}
