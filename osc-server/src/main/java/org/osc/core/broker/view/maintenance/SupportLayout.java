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
package org.osc.core.broker.view.maintenance;

import com.vaadin.server.ExternalResource;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.Link;

public class SupportLayout extends FormLayout {

    private static final long serialVersionUID = 1L;

    public SupportLayout() {
        Link link = new Link("Open Security Controller", new ExternalResource("http://www.intel.com/osc"));
        link.setTargetName("_blank");
        addComponent(link);
    }
}
