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
package org.osc.core.broker.view.vc.securitygroup;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.osc.core.broker.service.api.BindSecurityGroupServiceApi;
import org.osc.core.broker.service.api.ListSecurityGroupBindingsBySgServiceApi;
import org.osc.core.broker.service.api.server.ServerApi;
import org.osc.core.broker.service.dto.PolicyDto;
import org.osc.core.broker.service.dto.SecurityGroupDto;
import org.osc.core.broker.service.dto.VirtualSystemPolicyBindingDto;
import org.osc.core.broker.service.exceptions.ActionNotSupportedException;
import org.osc.core.broker.service.request.BaseIdRequest;
import org.osc.core.broker.service.request.BindSecurityGroupRequest;
import org.osc.core.broker.service.response.BaseJobResponse;
import org.osc.core.broker.view.CRUDBaseView;
import org.osc.core.broker.view.common.StyleConstants;
import org.osc.core.broker.view.common.VmidcMessages;
import org.osc.core.broker.view.common.VmidcMessages_;
import org.osc.core.broker.view.util.ViewUtil;
import org.osc.core.broker.window.CRUDBaseWindow;
import org.osc.core.broker.window.button.OkCancelButtonModel;
import org.slf4j.LoggerFactory;
import org.osc.sdk.controller.FailurePolicyType;
import org.slf4j.Logger;

import com.vaadin.data.Item;
import com.vaadin.data.Property;
import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.data.util.BeanItemContainer;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.Component;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Notification;
import com.vaadin.ui.Table;
import com.vaadin.ui.VerticalLayout;

public class BindSecurityGroupWindow extends CRUDBaseWindow<OkCancelButtonModel> {

	private static final String PROPERTY_ID_POLICY = "Inspection Policy";
	private static final String PROPERTY_ID_DA = "Distributed Appliance";
	private static final String PROPERTY_ID_ENABLED = "Enabled";
	private static final String PROPERTY_ID_FAILURE_POLICY = "Chaining Failure Policy";
	private static final String PROPERTY_ID_CHAIN_ORDER = "Order";

	private static final long serialVersionUID = 1L;

	final String CAPTION = "Bind Policy to Security Group";

	private static final Logger log = LoggerFactory.getLogger(BindSecurityGroupWindow.class);

	private final SecurityGroupDto currentSecurityGroup;
	private Table serviceTable = null;
	private Long vcId;

	private final BindSecurityGroupServiceApi bindSecurityGroupService;
    private final ListSecurityGroupBindingsBySgServiceApi listSecurityGroupBindingsBySgService;
    private final ServerApi server;

	public BindSecurityGroupWindow(SecurityGroupDto sgDto,
	        BindSecurityGroupServiceApi bindSecurityGroupService,
	        ListSecurityGroupBindingsBySgServiceApi listSecurityGroupBindingsBySgService,
	        ServerApi server, Long vcId) throws Exception {
		this.currentSecurityGroup = sgDto;
        this.bindSecurityGroupService = bindSecurityGroupService;
        this.listSecurityGroupBindingsBySgService = listSecurityGroupBindingsBySgService;
        this.server = server;
        this.vcId = vcId;
		createWindow(this.CAPTION + " - " + this.currentSecurityGroup.getName());
	}

	@Override
	public void populateForm() throws ActionNotSupportedException {
		this.content.addStyleName(StyleConstants.VMIDC_WINDOW_CONTENT_WRAPPER);
		this.content.addComponent(getServicePanel());
	}

	public List<Long> getSelectedServicesId() {
		List<Long> selectedServices = new ArrayList<>();
		for (Object id : this.serviceTable.getItemIds()) {
			if (this.serviceTable.getContainerProperty(id, PROPERTY_ID_ENABLED).getValue().equals(true)) {
				selectedServices.add((Long) id);
			}
		}
		return selectedServices;
	}

	@Override
	public boolean validateForm() {
		return true;
	}

	@Override
	public void submitForm() {
		try {
			if (validateForm()) {

				BindSecurityGroupRequest bindRequest = new BindSecurityGroupRequest();

				bindRequest.setVcId(this.vcId);
				bindRequest.setSecurityGroupId(this.currentSecurityGroup.getId());
				bindRequest.setSfcId(this.currentSecurityGroup.getServiceFunctionChainId());

				List<VirtualSystemPolicyBindingDto> allBindings = this.listSecurityGroupBindingsBySgService
						.dispatch(new BaseIdRequest(this.currentSecurityGroup.getId())).getMemberList();

				for (Long selectedVsId : getSelectedServicesId()) {
					VirtualSystemPolicyBindingDto previousBinding = null;
					for (VirtualSystemPolicyBindingDto binding : allBindings) {
						if (binding.getVirtualSystemId().equals(selectedVsId)) {
							previousBinding = binding;
							break;
						}
					}
					Item selectedService = this.serviceTable.getItem(selectedVsId);

					// TODO Larkins: Fix UI to receive the set of policies from the User
					// Do not update the policy binding from the UI.
					// Policy mapping for manager supporting multiple policies is not supported through UI.
					Set<Long> policyIdSet = null;
					if (previousBinding.isMultiplePoliciesSupported() && previousBinding.isBinded()) {
						policyIdSet = previousBinding.getPolicyIds();
					} else {
						Object selectedPolicy = ((ComboBox) selectedService.getItemProperty(PROPERTY_ID_POLICY)
								.getValue()).getValue();
						policyIdSet = selectedPolicy == null ? null
								: new HashSet<>(Arrays.asList(((PolicyDto) selectedPolicy).getId()));
					}
					String serviceName = (String) selectedService.getItemProperty(PROPERTY_ID_DA).getValue();
					ComboBox failurePolicyComboBox = (ComboBox) selectedService
							.getItemProperty(PROPERTY_ID_FAILURE_POLICY).getValue();
					FailurePolicyType failurePolicyType = FailurePolicyType.NA;
					if (failurePolicyComboBox.getData() != null) {
						// If failure policy is supported, the data is not null
						// and we need to use the failure policy
						// specified in the combobox
						failurePolicyType = (FailurePolicyType) failurePolicyComboBox.getValue();
					}

                    Long order = null;
                    if (this.currentSecurityGroup.getServiceFunctionChainId() == null) {
                        order = (Long) selectedService.getItemProperty(PROPERTY_ID_CHAIN_ORDER).getValue();
                    }

					// send null if user did not modify this set
					bindRequest.addServiceToBindTo(new VirtualSystemPolicyBindingDto(selectedVsId, serviceName,
							policyIdSet, failurePolicyType, order));
				}

				BaseJobResponse response = this.bindSecurityGroupService.dispatch(bindRequest);

				close();
				ViewUtil.showJobNotification(response.getJobId(), this.server);
			}

		} catch (Exception e) {
			log.error("Fail to Bind Security Group", e);
			ViewUtil.iscNotification(e.getMessage(), Notification.Type.ERROR_MESSAGE);
		}
	}

	@SuppressWarnings("serial")
	private Component getServicePanel() throws ActionNotSupportedException {
		try {

			this.serviceTable = new Table();
			this.serviceTable.setCaption("Services:");
			this.serviceTable.setPageLength(5);
			this.serviceTable.setImmediate(true);
			this.serviceTable.setSelectable(true);
			this.serviceTable.setMultiSelect(false);
			this.serviceTable.setNullSelectionAllowed(false);
			this.serviceTable.setNullSelectionItemId(CRUDBaseView.NULL_SELECTION_ITEM_ID);
			this.serviceTable.addGeneratedColumn(PROPERTY_ID_ENABLED, new CheckBoxGenerator());

			populateServiceTable();

			VerticalLayout selectorButtonLayout = new VerticalLayout();
			selectorButtonLayout.addStyleName(StyleConstants.SELECTOR_BUTTON_LAYOUT);

			Button moveUpButton = new Button(VmidcMessages.getString(VmidcMessages_.ORDER_MOVE_UP_TEXT));
			moveUpButton.setHtmlContentAllowed(true);
			moveUpButton.setDescription(VmidcMessages.getString(VmidcMessages_.ORDER_MOVE_UP_DESC));
			moveUpButton.addStyleName(StyleConstants.SELECTOR_BUTTON);
			moveUpButton.addClickListener(new ClickListener() {

				@Override
				public void buttonClick(ClickEvent event) {
					moveItem(true);
				}

			});
			Button moveDownButton = new Button(VmidcMessages.getString(VmidcMessages_.ORDER_MOVE_DOWN_TEXT));
			moveDownButton.setHtmlContentAllowed(true);
			moveDownButton.setDescription(VmidcMessages.getString(VmidcMessages_.ORDER_MOVE_DOWN_DESC));
			moveDownButton.addStyleName(StyleConstants.SELECTOR_BUTTON);
			moveDownButton.addClickListener(new ClickListener() {

				@Override
				public void buttonClick(ClickEvent event) {
					moveItem(false);
				}
			});

			selectorButtonLayout.addComponent(moveUpButton);
			selectorButtonLayout.addComponent(moveDownButton);

			HorizontalLayout selectorLayout = new HorizontalLayout();

			selectorLayout.addComponent(selectorButtonLayout);
			selectorLayout.addComponent(this.serviceTable);

			return selectorLayout;
		} catch (ActionNotSupportedException actionNotSupportedException) {
			throw actionNotSupportedException;
		} catch (Exception e) {
			ViewUtil.iscNotification(e.getMessage(), Notification.Type.ERROR_MESSAGE);
			log.error("Error while creating Services panel", e);
		}

		return null;
	}

	@SuppressWarnings("unchecked")
	protected void moveItem(boolean moveUp) {
		if (this.serviceTable.getValue() != null) {
			long selectedItemId = (long) this.serviceTable.getValue();
			Property<Long> selectedItemChainProperty = this.serviceTable.getContainerProperty(selectedItemId,
					PROPERTY_ID_CHAIN_ORDER);
			Long currentOrderValue = selectedItemChainProperty.getValue();
			Object nextItemIdObj = this.serviceTable.nextItemId(selectedItemId);
			Object previousItemIdObj = this.serviceTable.prevItemId(selectedItemId);

			if (moveUp && previousItemIdObj != null) {
				Property<Long> previousItemChainProperty = this.serviceTable.getContainerProperty(previousItemIdObj,
						PROPERTY_ID_CHAIN_ORDER);
				Long previousItemOrderValue = previousItemChainProperty.getValue();

				selectedItemChainProperty.setValue(previousItemOrderValue);
				previousItemChainProperty.setValue(currentOrderValue);
				sortByChainOrder();
			} else if (!moveUp && nextItemIdObj != null) {
				Property<Long> nextItemChainProperty = this.serviceTable.getContainerProperty(nextItemIdObj,
						PROPERTY_ID_CHAIN_ORDER);
				Long nextItemOrderValue = nextItemChainProperty.getValue();

				selectedItemChainProperty.setValue(nextItemOrderValue);
				nextItemChainProperty.setValue(currentOrderValue);

				sortByChainOrder();
			}
		}
	}

	private void sortByChainOrder() {
		this.serviceTable.sort(new Object[] { PROPERTY_ID_CHAIN_ORDER }, new boolean[] { true });
	}

	@SuppressWarnings("unchecked")
	private void populateServiceTable() throws Exception {
		// TODO: Future. Convert this table into Bean container and add DTOs in
		// it.
		// creating Virtual System Table
		this.serviceTable.addContainerProperty(PROPERTY_ID_CHAIN_ORDER, Long.class, null);
		this.serviceTable.addContainerProperty(PROPERTY_ID_ENABLED, Boolean.class, false);
		this.serviceTable.addContainerProperty(PROPERTY_ID_DA, String.class, null);
		this.serviceTable.addContainerProperty(PROPERTY_ID_POLICY, ComboBox.class, null);
		this.serviceTable.addContainerProperty(PROPERTY_ID_FAILURE_POLICY, ComboBox.class, null);

		this.serviceTable.removeAllItems();

		List<VirtualSystemPolicyBindingDto> allBindings = this.listSecurityGroupBindingsBySgService
				.dispatch(new BaseIdRequest(this.currentSecurityGroup.getId())).getMemberList();

		for (VirtualSystemPolicyBindingDto binding : allBindings) {
			List<PolicyDto> policies = binding.getPolicies();
			ComboBox policyComboBox = getPolicyComboBox(policies);
			if (binding.isMultiplePoliciesSupported()) {
				policyComboBox.setEnabled(false);
			} else {
				policyComboBox.setRequired(policies != null && policies.size() > 0);
			}

			ComboBox failurePolicyComboBox = getFailurePolicyComboBox();

			this.serviceTable.addItem(
					new Object[] { binding.getOrder(), binding.getName(), policyComboBox, failurePolicyComboBox },
					binding.getVirtualSystemId());

			if (binding.isBinded() && !binding.getPolicyIds().isEmpty()) {
				// For any existing bindings, set enabled and set the order
				// value
				this.serviceTable.getContainerProperty(binding.getVirtualSystemId(), PROPERTY_ID_ENABLED)
						.setValue(true);

				ComboBox comboBoxPolicy = (ComboBox) this.serviceTable
						.getContainerProperty(binding.getVirtualSystemId(), PROPERTY_ID_POLICY).getValue();
				if (binding.isMultiplePoliciesSupported()) {
					comboBoxPolicy.setEnabled(false);
				} else {
					comboBoxPolicy.setEnabled(policies != null && policies.size() > 0);
				}
				for (Object comboBoxItemId : comboBoxPolicy.getContainerDataSource().getItemIds()) {
					if (comboBoxPolicy.getItem(comboBoxItemId).getItemProperty("id").getValue()
							.equals(binding.getPolicyIds().iterator().next())) {
						comboBoxPolicy.select(comboBoxItemId);
						break;
					}
				}
			}

			ComboBox comboBoxFailurePolicy = (ComboBox) this.serviceTable
					.getContainerProperty(binding.getVirtualSystemId(), PROPERTY_ID_FAILURE_POLICY).getValue();
			if (binding.getFailurePolicyType() != FailurePolicyType.NA) {
				if (binding.isBinded()) {
					comboBoxFailurePolicy.setEnabled(true);
				}
				comboBoxFailurePolicy.setData(binding.getFailurePolicyType());
				comboBoxFailurePolicy.select(binding.getFailurePolicyType());
			} else {
				comboBoxFailurePolicy.setData(null);
				comboBoxFailurePolicy.setEnabled(false);
			}
		}

		sortByChainOrder();
		if (this.serviceTable.getItemIds().size() > 0) {
			this.serviceTable.select(this.serviceTable.getItemIds().iterator().next());
		}
	}

	private ComboBox getPolicyComboBox(List<PolicyDto> policyDtoList) {
		ComboBox policy = new ComboBox("Select Policy");
		policy.setTextInputAllowed(false);
		policy.setNullSelectionAllowed(false);
		policy.setImmediate(true);
		policy.setRequired(true);
		policy.setRequiredError("Policy cannot be empty");

		BeanItemContainer<PolicyDto> policyListContainer = new BeanItemContainer<>(PolicyDto.class,
				policyDtoList);
		policy.setContainerDataSource(policyListContainer);
		policy.setItemCaptionPropertyId("policyName");

		if (policyListContainer.size() > 0) {
			policy.select(policyListContainer.getIdByIndex(0));
		}

		policy.setEnabled(false);

		return policy;
	}

	private ComboBox getFailurePolicyComboBox() {
		ComboBox failurePolicy = new ComboBox("Select Failure Policy");
		failurePolicy.setTextInputAllowed(false);
		failurePolicy.setNullSelectionAllowed(false);
		failurePolicy.setImmediate(true);
		failurePolicy.setRequired(true);
		failurePolicy.setRequiredError("Failure Policy cannot be empty");

		failurePolicy.addItems(FailurePolicyType.FAIL_OPEN, FailurePolicyType.FAIL_CLOSE);
		failurePolicy.select(FailurePolicyType.FAIL_OPEN);
		failurePolicy.setEnabled(false);

		return failurePolicy;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private void serviceTableClicked(long itemId, boolean setCheckBoxValue) {
		ComboBox policyComboBox = (ComboBox) this.serviceTable.getContainerProperty(itemId, PROPERTY_ID_POLICY)
				.getValue();
		ComboBox failurePolicyComboBox = (ComboBox) this.serviceTable
				.getContainerProperty(itemId, PROPERTY_ID_FAILURE_POLICY).getValue();
		Property itemProperty = this.serviceTable.getContainerProperty(itemId, PROPERTY_ID_ENABLED);

		boolean currentValue = (boolean) itemProperty.getValue();
		if (setCheckBoxValue) {
			itemProperty.setValue(!currentValue);
			if (policyComboBox.getContainerDataSource().size() > 0) {
				policyComboBox.setEnabled(!currentValue);
			}
			policyComboBox.setEnabled(!currentValue);
			if (failurePolicyComboBox.getData() != null) {
				failurePolicyComboBox.setEnabled(!currentValue);
			}

		} else {
			if (policyComboBox.getContainerDataSource().size() > 0) {
				policyComboBox.setEnabled(currentValue);
			}
			if (failurePolicyComboBox.getData() != null) {
				failurePolicyComboBox.setEnabled(currentValue);
			}
		}
	}

	@SuppressWarnings("serial")
	private class CheckBoxGenerator implements Table.ColumnGenerator {
		@Override
		public Object generateCell(Table source, final Object itemId, Object columnId) {
			Property<?> prop = source.getItem(itemId).getItemProperty(columnId);
			CheckBox checkBox = new CheckBox(null, prop);
			checkBox.addValueChangeListener(new ValueChangeListener() {

				@Override
				public void valueChange(ValueChangeEvent event) {
					serviceTableClicked((long) itemId, false);
				}
			});
			return checkBox;
		}
	}
}
