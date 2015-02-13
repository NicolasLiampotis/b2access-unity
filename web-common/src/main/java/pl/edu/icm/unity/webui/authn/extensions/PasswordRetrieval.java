/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.authn.extensions;

import java.util.Collection;
import java.util.Collections;

import org.apache.log4j.Logger;

import pl.edu.icm.unity.Constants;
import pl.edu.icm.unity.exceptions.IllegalCredentialException;
import pl.edu.icm.unity.exceptions.IllegalIdentityValueException;
import pl.edu.icm.unity.exceptions.InternalException;
import pl.edu.icm.unity.server.authn.AuthenticationResult;
import pl.edu.icm.unity.server.authn.AuthenticationResult.Status;
import pl.edu.icm.unity.server.authn.CredentialExchange;
import pl.edu.icm.unity.server.authn.CredentialRetrieval;
import pl.edu.icm.unity.server.authn.remote.SandboxAuthnResultCallback;
import pl.edu.icm.unity.server.utils.I18nStringJsonUtil;
import pl.edu.icm.unity.server.utils.Log;
import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.stdext.credential.PasswordExchange;
import pl.edu.icm.unity.stdext.credential.PasswordVerificatorFactory;
import pl.edu.icm.unity.types.I18nDescribedObject;
import pl.edu.icm.unity.types.I18nString;
import pl.edu.icm.unity.webui.authn.UsernameComponent;
import pl.edu.icm.unity.webui.authn.VaadinAuthentication;
import pl.edu.icm.unity.webui.authn.credreset.CredentialReset1Dialog;
import pl.edu.icm.unity.webui.common.Styles;
import pl.edu.icm.unity.webui.common.credentials.CredentialEditor;
import pl.edu.icm.unity.webui.common.credentials.CredentialEditorRegistry;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.vaadin.server.UserError;
import com.vaadin.server.VaadinRequest;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.Component;
import com.vaadin.ui.PasswordField;
import com.vaadin.ui.VerticalLayout;

import eu.unicore.util.configuration.ConfigurationException;

/**
 * Retrieves passwords using a Vaadin widget.
 * 
 * @author K. Benedyczak
 */
public class PasswordRetrieval implements CredentialRetrieval, VaadinAuthentication
{
	private Logger log = Log.getLogger(Log.U_SERVER_WEB, PasswordRetrieval.class);
	private UnityMessageSource msg;
	private PasswordExchange credentialExchange;
	private I18nString name;
	private String registrationFormForUnknown;
	private CredentialEditorRegistry credEditorReg;

	public PasswordRetrieval(UnityMessageSource msg, CredentialEditorRegistry credEditorReg)
	{
		this.msg = msg;
		this.credEditorReg = credEditorReg;
	}

	@Override
	public String getBindingName()
	{
		return VaadinAuthentication.NAME;
	}

	@Override
	public String getSerializedConfiguration()
	{
		ObjectNode root = Constants.MAPPER.createObjectNode();
		root.set("i18nName", I18nStringJsonUtil.toJson(name));
		root.put("registrationFormForUnknown", registrationFormForUnknown);
		try
		{
			return Constants.MAPPER.writeValueAsString(root);
		} catch (JsonProcessingException e)
		{
			throw new InternalException("Can't serialize web-based password retrieval configuration to JSON", e);
		}
	}

	@Override
	public void setSerializedConfiguration(String json)
	{
		try
		{
			JsonNode root = Constants.MAPPER.readTree(json);
			name = I18nStringJsonUtil.fromJson(root.get("i18nName"), root.get("name"));
			if (name.isEmpty())
				name = I18nDescribedObject.loadI18nStringFromBundle(
						"WebPasswordRetrieval.password", msg);
			JsonNode formNode = root.get("registrationFormForUnknown");
			if (formNode != null && !formNode.isNull())
				registrationFormForUnknown = formNode.asText();
		} catch (Exception e)
		{
			throw new ConfigurationException("The configuration of the web-" +
					"based password retrieval can not be parsed", e);
		}
	}

	@Override
	public void setCredentialExchange(CredentialExchange e)
	{
		this.credentialExchange = (PasswordExchange) e;
	}


	@Override
	public Collection<VaadinAuthenticationUI> createUIInstance()
	{
		return Collections.<VaadinAuthenticationUI>singleton(
				new PasswordRetrievalUI(credEditorReg.getEditor(PasswordVerificatorFactory.NAME)));
	}


	private class PasswordRetrievalUI implements VaadinAuthenticationUI
	{
		private UsernameComponent usernameComponent;
		private PasswordField passwordField;
		private CredentialEditor credEditor;
		private AuthenticationResultCallback callback;
		private SandboxAuthnResultCallback sandboxCallback;

		public PasswordRetrievalUI(CredentialEditor credEditor)
		{
			this.credEditor = credEditor;
		}

		@Override
		public void setAuthenticationResultCallback(AuthenticationResultCallback callback)
		{
			this.callback = callback;
		}

		@Override
		public Component getComponent()
		{
			VerticalLayout ret = new VerticalLayout();
			ret.setSpacing(true);
			
			usernameComponent = new UsernameComponent(msg);
			ret.addComponent(usernameComponent);
			
			String label = name.getValue(msg);
			passwordField = new PasswordField(label + ":");
			passwordField.setId("WebPasswordRetrieval.password");
			ret.addComponent(passwordField);

			if (credentialExchange.getCredentialResetBackend().getSettings().isEnabled())
			{
				Button reset = new Button(msg.getMessage("WebPasswordRetrieval.forgottenPassword"));
				reset.setStyleName(Styles.vButtonLink.toString());
				ret.addComponent(reset);
				ret.setComponentAlignment(reset, Alignment.TOP_RIGHT);
				reset.addClickListener(new ClickListener()
				{
					@Override
					public void buttonClick(ClickEvent event)
					{
						showResetDialog();
					}
				});
			}

			return ret;
		}

		@Override
		public void triggerAuthentication()
		{
			String username = usernameComponent.getUsername();
			String password = passwordField.getValue();
			if (username.equals("") && password.equals(""))
			{
				passwordField.setComponentError(new UserError(
						msg.getMessage("WebPasswordRetrieval.noPassword")));
			}
			
			callback.setAuthenticationResult(getAuthenticationResult(username, password));
		}
		

		private AuthenticationResult getAuthenticationResult(String username, String password)
		{
			if (username.equals("") && password.equals(""))
			{
				return new AuthenticationResult(Status.notApplicable, null);
			}
			try
			{
				AuthenticationResult authenticationResult = credentialExchange.checkPassword(
						username, password, sandboxCallback);
				if (authenticationResult.getStatus() == Status.success)
					passwordField.setComponentError(null);
				else if (authenticationResult.getStatus() == Status.unknownRemotePrincipal && 
						registrationFormForUnknown != null) 
				{
					authenticationResult.setFormForUnknownPrincipal(registrationFormForUnknown);
					passwordField.setValue("");
				} else
				{
					passwordField.setComponentError(new UserError(
							msg.getMessage("WebPasswordRetrieval.wrongPassword")));
					passwordField.setValue("");
				}
				return authenticationResult;
			} catch (Exception e)
			{
				if (!(e instanceof IllegalCredentialException) && 
						!(e instanceof IllegalIdentityValueException))
					log.warn("Password verificator has thrown an exception", e);
				passwordField.setComponentError(new UserError(
						msg.getMessage("WebPasswordRetrieval.wrongPassword")));
				passwordField.setValue("");
				return new AuthenticationResult(Status.deny, null);
			}
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public String getLabel()
		{
			return name.getValue(msg);
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public String getImageURL()
		{
			return null;
		}

		private void showResetDialog()
		{
			CredentialReset1Dialog dialog = new CredentialReset1Dialog(msg, 
					credentialExchange.getCredentialResetBackend(), credEditor);
			dialog.show();
		}

		@Override
		public void cancelAuthentication()
		{
			//do nothing
		}

		@Override
		public void clear()
		{
			passwordField.setValue("");
		}

		@Override
		public void refresh(VaadinRequest request) 
		{
			//nop
		}

		@Override
		public void setSandboxAuthnResultCallback(SandboxAuthnResultCallback callback) 
		{
			sandboxCallback = callback;
		}

		/**
		 * Simple: there is only one authN option in this authenticator so we can return any constant id. 
		 */
		@Override
		public String getId()
		{
			return "password";
		}
	}
}










