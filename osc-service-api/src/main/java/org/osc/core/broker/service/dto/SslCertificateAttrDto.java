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
package org.osc.core.broker.service.dto;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

import io.swagger.annotations.ApiModelProperty;

// SSL Certificate attributes Data Transfer Object associated with VC entity
@XmlRootElement(name = "sslCertificateAttributes")
@XmlAccessorType(XmlAccessType.FIELD)
public class SslCertificateAttrDto extends BaseDto {

    @ApiModelProperty(value = "SSL certificate alias")
    private String alias = "";

    @ApiModelProperty(value = "SHA1 fingerprint of the certificate")
    private String sha1 = "";

    public SslCertificateAttrDto() {
    }

    public SslCertificateAttrDto(String alias, String sha1) {
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
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((this.alias == null) ? 0 : this.alias.hashCode());
        result = prime * result + ((this.sha1 == null) ? 0 : this.sha1.hashCode());
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
        if (getClass() != obj.getClass()) {
            return false;
        }
        SslCertificateAttrDto other = (SslCertificateAttrDto) obj;
        if (this.alias == null) {
            if (other.alias != null) {
                return false;
            }
        } else if (!this.alias.equals(other.alias)) {
            return false;
        }
        if (this.sha1 == null) {
            if (other.sha1 != null) {
                return false;
            }
        } else if (!this.sha1.equals(other.sha1)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "SslCertificateAttrDto{" +
                "alias='" + this.alias + '\'' +
                ", sha1='" + this.sha1 + '\'' +
                '}';
    }
}
