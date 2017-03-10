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
package org.osc.core.test.util;

import java.util.List;

import org.apache.commons.lang.NotImplementedException;
import org.hibernate.CacheMode;
import org.hibernate.Criteria;
import org.hibernate.FetchMode;
import org.hibernate.FlushMode;
import org.hibernate.HibernateException;
import org.hibernate.LockMode;
import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projection;
import org.hibernate.sql.JoinType;
import org.hibernate.transform.ResultTransformer;

/**
 * This test class represents a {@link Criteria} and is used
 * by tests to stub criteria methods.
 */
public class TestCriteria implements Criteria {
    private TestCriteriaContent criteriaContent;

    public TestCriteria() {
        this.criteriaContent = new TestCriteriaContent();
    }

    public TestCriteriaContent getCriteriaContent() {
        return this.criteriaContent;
    }

    @Override
    public Criteria add(Criterion criterion) {
        this.criteriaContent.addCriterion(criterion);
        return this;
    }

    @Override
    public Criteria addOrder(Order arg0) {
        throw new NotImplementedException();
    }

    @Override
    public Criteria createAlias(String name, String value) throws HibernateException {
        this.criteriaContent.addAlias(name, value);
        return this;
    }

    @Override
    public Criteria createAlias(String arg0, String arg1, JoinType arg2) throws HibernateException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Criteria createAlias(String arg0, String arg1, int arg2) throws HibernateException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Criteria createAlias(String arg0, String arg1, JoinType arg2, Criterion arg3) throws HibernateException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Criteria createAlias(String arg0, String arg1, int arg2, Criterion arg3) throws HibernateException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Criteria createCriteria(String arg0) throws HibernateException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Criteria createCriteria(String arg0, JoinType arg1) throws HibernateException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Criteria createCriteria(String arg0, int arg1) throws HibernateException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Criteria createCriteria(String arg0, String arg1) throws HibernateException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Criteria createCriteria(String arg0, String arg1, JoinType arg2) throws HibernateException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Criteria createCriteria(String arg0, String arg1, int arg2) throws HibernateException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Criteria createCriteria(String arg0, String arg1, JoinType arg2, Criterion arg3)
            throws HibernateException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Criteria createCriteria(String arg0, String arg1, int arg2, Criterion arg3) throws HibernateException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getAlias() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean isReadOnly() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean isReadOnlyInitialized() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public List<?> list() throws HibernateException {
        throw new NotImplementedException();
    }

    @Override
    public ScrollableResults scroll() throws HibernateException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ScrollableResults scroll(ScrollMode arg0) throws HibernateException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Criteria setCacheMode(CacheMode arg0) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Criteria setCacheRegion(String arg0) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Criteria setCacheable(boolean arg0) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Criteria setComment(String arg0) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Criteria setFetchMode(String arg0, FetchMode arg1) throws HibernateException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Criteria setFetchSize(int arg0) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Criteria setFirstResult(int firstResult) {
        this.criteriaContent.setFirstResult(firstResult);
        return this;
    }

    @Override
    public Criteria setFlushMode(FlushMode arg0) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Criteria setLockMode(LockMode arg0) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Criteria setLockMode(String arg0, LockMode arg1) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Criteria setMaxResults(int maxResults) {
        this.criteriaContent.setMaxResults(maxResults);
        return this;
    }

    @Override
    public Criteria setProjection(Projection projection) {
        this.criteriaContent.setProjection(projection);
        return this;
    }

    @Override
    public Criteria setReadOnly(boolean arg0) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Criteria setResultTransformer(ResultTransformer resultTransformer) {
        this.criteriaContent.setResultTransformer(resultTransformer);
        return this;
    }

    @Override
    public Criteria setTimeout(int arg0) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Object uniqueResult() throws HibernateException {
        // TODO Auto-generated method stub
        return null;
    }

    public void clearCriteria() {
        this.criteriaContent = new TestCriteriaContent();
    }

    @Override
    public Criteria addQueryHint(String arg0) {
        // TODO Auto-generated method stub
        return null;
    }
}

