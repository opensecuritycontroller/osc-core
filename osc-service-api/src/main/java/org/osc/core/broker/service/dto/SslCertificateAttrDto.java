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

import io.swagger.annotations.ApiModelProperty;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

// SSL Certificate attributes Data Transfer Object associated with VC entity
@XmlRootElement(name = "sslCertificateAttributes")
@XmlAccessorType(XmlAccessType.FIELD)
public class SslCertificateAttrDto extends BaseDto {

    @ApiModelProperty(value = "SSL certificate alias")
    private String alias = "";

    @ApiModelProperty(value = "SHA1 fingerprint of the certificate")
    private String sha1 = "";

    public String getAlias() {
        return alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    public String getSha1() {
        return sha1;
    }

    public void setSha1(String sha1) {
        this.sha1 = sha1;
    }

    @Override
    public String toString() {
        return "SslCertificateAttrDto{" +
                "alias='" + alias + '\'' +
                ", sha1='" + sha1 + '\'' +
                '}';
    }
}
