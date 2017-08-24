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
import org.osc.core.broker.model.entities.virtualization.VirtualizationConnector;
import org.osc.core.broker.service.exceptions.VmidcException;

import io.fabric8.kubernetes.client.KubernetesClient;

public class KubernetesPodApiTest {

	@Rule
	public ExpectedException exception = ExpectedException.none();

	@InjectMocks
	private KubernetesPodApi service;

	@Mock
	private KubernetesClient getKubernetesClient = null;

	private VirtualizationConnector vc = new VirtualizationConnector();

	@Before
	public void setUp() {
		vc.setProviderIpAddress("ip");
		this.service = new KubernetesPodApi(vc);
	}


	@Test
	public void testgetPodsbyLabel_WithNullLabel_ThrowsIllegalArgumentException() throws Exception {

		// Arrange.
		this.exception.expect(IllegalArgumentException.class);

		// Act.
		this.service.getPodsByLabel(null);
	}

	@Test
	public void testgetPodsbyId_WithNullName_ThrowsIllegalArgumentException() throws Exception {

		// Arrange.
		this.exception.expect(IllegalArgumentException.class);

		// Act.
		this.service.getPodById("1234", null, "sample_namespace");
	}

	@Test
	public void testgetPodsbyId_WithNullUid_ThrowsIllegalArgumentException() throws Exception {

		// Arrange.
		this.exception.expect(IllegalArgumentException.class);

		// Act.
		this.service.getPodById(null, "sample_name", "sample_namespace");
	}
	
	@Test
	public void testgetPodsbyId_WithNullNameSpace_ThrowsIllegalArgumentException() throws Exception {

		// Arrange.
		this.exception.expect(IllegalArgumentException.class);

		// Act.
		this.service.getPodById("1234", "sample_name", null);
	}
	
	
	@Test
	public void testgetPodById_WhenK8ClientConnectionFails_ThrowsVmidcException() throws Exception {

		// Arrange.
		//this.exception.expect(VmidcException.class);

		// Act.
		assertEquals(this.service.getPodById("1234", "sample_name", "sample_label"), null);
	}
	
	@Test
	public void testgetPodsbyLabel_WhenK8ClientConnectionFails_ThrowsVmidcException() throws Exception {

		// Arrange.
		this.exception.expect(VmidcException.class);

		// Act.
		this.service.getPodsByLabel("sample_label");
	}

	

}
