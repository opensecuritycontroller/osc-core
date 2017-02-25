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

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

@Entity
@Table(name = "SSL_CERTIFICATE_ATTR")
public class SslCertificateAttr extends BaseEntity {

    /**
     *
     */
    private static final long serialVersionUID = 8639430511055269363L;

    @Column(name = "ssl_alias")
    private String alias;

    @Column(name = "ssl_sha1")
    private String sha1;

    public SslCertificateAttr() {
    }

    public SslCertificateAttr(String alias, String sha1) {
        this.alias = alias;
        this.sha1 = sha1;
    }

    public String getAlias() {
        return this.alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    public String getSha1() {
        return this.sha1;
    }

    public void setSha1(String sha1) {
        this.sha1 = sha1;
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

        SslCertificateAttr other = (SslCertificateAttr) object;

        return new EqualsBuilder()
                .append(getAlias(), other.getAlias())
                .append(getSha1(), other.getSha1())
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(getAlias())
                .append(getSha1())
                .toHashCode();
    }

}
