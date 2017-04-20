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
package org.osc.core.broker.rest.client.nsx.model;

import org.osc.core.broker.service.annotations.VmidcLogHidden;
import org.osc.sdk.sdn.element.ServiceManagerElement;

public class ServiceManager implements ServiceManagerElement {
    public static final String VENDOR_NAME = "Open";
    public static final String PRODUCT_NAME = "Security Controller";
    public static final String SHORT_NAME = "OSC";
    public static final String VENDOR_DESCRIPTION = VENDOR_NAME + " " + PRODUCT_NAME;
    public static final String VMIDC_THUMB_PRINT = "C8:FF:27:52:07:2E:14:41:73:7D:BD:6B:0D:D0:6F:05:0E:37:CB:D7";

    private String objectId;
    private String vsmUuid;
    private String revision;
    private String name;
    private String description;
    private String vendorName;
    private String vendorId;
    private String status;
    private String login;
    @VmidcLogHidden
    private String password;
    @VmidcLogHidden
    private String verifyPassword;
    private String thumbprint;
    private String restUrl;
    private String objectTypeName;

    public ServiceManager(
            String name,
            String vendorName,
            String vendorId,
            String callbackUrl,
            String oscUserName,
            String oscPassword,
            String verifyOscPassword) {
        this.name = name;
        this.vendorName = vendorName;
        this.vendorId = vendorId;
        this.restUrl = callbackUrl;
        this.login = oscUserName;
        this.password = oscPassword;
        this.verifyPassword = verifyOscPassword;
        this.thumbprint = VMIDC_THUMB_PRINT;
    }

    public ServiceManager(ServiceManagerElement serviceManager) {
        this.objectId = serviceManager.getId();
        this.vsmUuid = serviceManager.getVsmId();
        this.revision = serviceManager.getRevision();
        this.name = serviceManager.getName();
        this.description = serviceManager.getDescription();
        this.vendorName = serviceManager.getVendorName();
        this.vendorId = serviceManager.getVendorId();
        this.status = serviceManager.getStatus();
        this.login = serviceManager.getOscUserName();
        this.password = serviceManager.getOscPassword();
        this.verifyPassword = serviceManager.getOscVerifyPassword();
        this.thumbprint = serviceManager.getOscCertThumbprint();
        this.restUrl = serviceManager.getCallbackUrl();
        this.objectTypeName = serviceManager.getType();
    }

    @Override
    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public String getVendorName() {
        return this.vendorName;
    }

    public void setVendorName(String vendorName) {
        this.vendorName = vendorName;
    }

    @Override
    public String getVendorId() {
        return this.vendorId;
    }

    public void setVendorId(String vendorId) {
        this.vendorId = vendorId;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setLogin(String login) {
        this.login = login;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setVerifyPassword(String verifyPassword) {
        this.verifyPassword = verifyPassword;
    }

    public void setRestUrl(String restUrl) {
        this.restUrl = restUrl;
    }

    @Override
    public String toString() {
        return "ServiceManager [objectId=" + this.objectId + ", revision=" + this.revision + ", name=" + this.name + ", description="
                + this.description + ", vendorName=" + this.vendorName + ", vendorId=" + this.vendorId + ", status=" + this.status
                + ", login=" + this.login + ", restUrl=" + this.restUrl + "]";
    }

    @Override
    public String getId() {
        return this.objectId;
    }

    @Override
    public String getVsmId() {
        return this.vsmUuid;
    }

    @Override
    public String getCallbackUrl() {
        return this.restUrl;
    }

    @Override
    public String getOscUserName() {
        return this.login;
    }

    @Override
    public String getOscPassword() {
        return this.password;
    }

    @Override
    public String getOscVerifyPassword() {
        return this.verifyPassword;
    }

    @Override
    public String getOscCertThumbprint() {
        return this.thumbprint;
    }

    @Override
    public String getRevision() {
        return this.revision;
    }

    @Override
    public String getDescription() {
        return this.description;
    }

    @Override
    public String getStatus() {
        return this.status;
    }

    @Override
    public String getType() {
        return this.objectTypeName;
    }
}
