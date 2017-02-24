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
package org.osc.core.test.util;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang3.tuple.Pair;
import org.hibernate.Criteria;
import org.hibernate.HibernateException;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Projection;
import org.hibernate.transform.ResultTransformer;

/**
 * Represents the content of a {@link Criteria} to be used as a key
 * to map a criteria to mocked result.
 */
public class TestCriteriaContent {
    private List<TestCriterion> criterionCollection;
    private int firstResult;
    private int maxResults;
    private Projection projection;
    private List<Pair<String, String>> aliasCollection;
    private ResultTransformer resultTransformer;

    public TestCriteriaContent() {
        this.criterionCollection = new ArrayList<TestCriterion>();
        this.aliasCollection = new ArrayList<Pair<String, String>>();
    }

    public void addCriterion(Criterion criterion) {
        this.criterionCollection.add(new TestCriterion(criterion));
    }

    public void addAlias(String name, String value) throws HibernateException {
        this.aliasCollection.add(Pair.of(name, value));
    }

    public void setProjection(Projection projection) {
        this.projection = projection;
    }

    public void setResultTransformer(ResultTransformer resultTransformer) {
        this.resultTransformer = resultTransformer;
    }

    public void setMaxResults(int maxResults) {
        this.maxResults = maxResults;
    }

    public void setFirstResult(int firstResult) {
        this.firstResult = firstResult;
    }

    @Override
    public boolean equals(Object object) {
        if (object == null) {
            return false;
        }
        if (getClass() != object.getClass()) {
            return false;
        }
        if (this == object) {
            return true;
        }

        TestCriteriaContent other = (TestCriteriaContent) object;
        return new EqualsBuilder()
                .append(this.criterionCollection, other.criterionCollection)
                .append(this.firstResult, other.firstResult)
                .append(this.maxResults, other.maxResults)
                .append(this.projectionString(), other.projectionString())
                .append(this.aliasCollection, other.aliasCollection)
                .append(this.resultTransformer, other.resultTransformer)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(this.criterionCollection)
                .append(this.firstResult)
                .append(this.maxResults)
                .append(this.projectionString())
                .append(this.aliasCollection)
                .append(this.resultTransformer)
                .toHashCode();
    }

    private String projectionString() {
        return this.projection == null ? "null" : this.projection.toString();
    }

    private class TestCriterion {
        private Criterion criterion;

        public TestCriterion(Criterion criterion) {
            this.criterion = criterion;
        }

        @Override
        public boolean equals(Object object) {
            if (object == null) {
                return false;
            }
            if (getClass() != object.getClass()) {
                return false;
            }
            if (this == object) {
                return true;
            }

            TestCriterion other = (TestCriterion) object;

            return new EqualsBuilder()
                    .append(this.criterion.toString(), other.criterion.toString())
                    .isEquals();
        }

        @Override
        public int hashCode() {
            return new HashCodeBuilder()
                    .append(this.criterion.toString())
                    .toHashCode();
        }
    }
}
