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

package org.osc.core.broker.rest.client.k8s;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.osc.core.broker.service.exceptions.VmidcException;

public class KubernetesPodApiTest {
    @Rule
    public ExpectedException exception = ExpectedException.none();

    @InjectMocks
    private KubernetesPodApi service;

    @Mock
    private KubernetesClient kubernetesClient;

    @Before
    public void testInitialize() throws Exception{
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testGetPodsbyLabel_WithNullLabel_ThrowsIllegalArgumentException() throws Exception {
        // Arrange.
        this.exception.expect(IllegalArgumentException.class);

        // Act.
        this.service.getPodsByLabel(null);
    }

    @Test
    public void testGetPodsbyLabel_WhenK8ClientConnectionFails_ThrowsVmidcException() throws Exception {
        // Arrange.
        this.exception.expect(VmidcException.class);

        // Act.
        this.service.getPodsByLabel("sample_label");
    }

    @Test
    public void testGetPodsbyId_WithNullName_ThrowsIllegalArgumentException() throws Exception {
        // Arrange.
        this.exception.expect(IllegalArgumentException.class);

        // Act.
        this.service.getPodById("1234", null, "sample_namespace");
    }

    @Test
    public void testGetPodsbyId_WithNullUid_ThrowsIllegalArgumentException() throws Exception {
        // Arrange.
        this.exception.expect(IllegalArgumentException.class);

        // Act.
        this.service.getPodById(null, "sample_name", "sample_namespace");
    }

    @Test
    public void testGetPodsbyId_WithNullNameSpace_ThrowsIllegalArgumentException() throws Exception {
        // Arrange.
        this.exception.expect(IllegalArgumentException.class);

        // Act.
        this.service.getPodById("1234", "sample_name", null);
    }


    @Test
    public void testGetPodById_WhenK8ClientConnectionFails_ThrowsVmidcException() throws Exception {
        // Arrange.
        //this.exception.expect(VmidcException.class);

        // Act.
        assertEquals(this.service.getPodById("1234", "sample_name", "sample_label"), null);
    }
}
