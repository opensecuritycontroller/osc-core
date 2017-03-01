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
package org.osc.core.broker.window.add;

import java.util.Optional;

import org.apache.log4j.Logger;
import org.osc.core.broker.service.exceptions.VmidcBrokerInvalidEntryException;
import org.osc.core.broker.util.ValidateUtil;
import org.osc.core.broker.view.common.VmidcMessages;
import org.osc.core.broker.view.common.VmidcMessages_;
import org.osc.core.broker.view.util.ViewUtil;
import org.osc.core.broker.window.CRUDBaseWindow;
import org.osc.core.broker.window.button.OkCancelButtonModel;

import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Notification;
import com.vaadin.ui.PasswordField;

public class PasswordWindow extends CRUDBaseWindow<OkCancelButtonModel> {
	private static final Logger LOG = Logger.getLogger(PasswordWindow.class);
	private static final long serialVersionUID = -7979397047792926898L;
	
	private PasswordField passwordField;
	private Optional<SubmitFormListener> submitFormListener;
	
	public interface SubmitFormListener {
		void submit(String password);
	}
	
	public PasswordWindow() throws Exception {
        createWindow("Create database backup password");
    }
	
	@Override
	public void populateForm() throws Exception {
		this.form.setMargin(true);
        this.form.setSizeUndefined();

        HorizontalLayout layout = new HorizontalLayout();
        this.passwordField = new PasswordField();
        this.passwordField.setRequired(true);
        
        layout.addComponent(this.passwordField);
        layout.setCaption(VmidcMessages.getString(VmidcMessages_.PASSWORD_CAPTION));

        this.form.addComponent(layout);
	}

	@Override
	public boolean validateForm() {
		try {
			ValidateUtil.checkForValidPassword(this.passwordField.getValue());
		} catch (VmidcBrokerInvalidEntryException e) {
			LOG.error("Invalid password defined by user.", e);
			ViewUtil.iscNotification(e.getMessage(), Notification.Type.ERROR_MESSAGE);
			return false;
		}

		return true;
	}

	@Override
	public void submitForm() {
		if (validateForm()) {
			if (submitFormListener.isPresent()) {
				submitFormListener.get().submit(this.passwordField.getValue());
			}
			close();
		}	
	}

	public void setSubmitFormListener(SubmitFormListener listener) {
		submitFormListener = Optional.of(listener);
	}
}
