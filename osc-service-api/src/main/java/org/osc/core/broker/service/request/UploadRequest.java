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
package org.osc.core.broker.service.request;

import java.io.InputStream;

import org.osc.core.broker.service.dto.BaseDto;


public class UploadRequest extends BaseRequest<BaseDto> {

    private String fileName;
    private InputStream uploadedInputStream;

    public UploadRequest(String fileName, InputStream uploadedInputStream) {
        this.fileName = fileName;
        this.uploadedInputStream = uploadedInputStream;
    }

    public String getFileName() {
        return this.fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public InputStream getUploadedInputStream() {
        return this.uploadedInputStream;
    }

    public void setUploadedInputStream(InputStream uploadedInputStream) {
        this.uploadedInputStream = uploadedInputStream;
    }
}
