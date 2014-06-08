/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.saml.sp.web;

import java.util.Collection;
import java.util.Set;

import org.apache.log4j.Logger;

import pl.edu.icm.unity.saml.sp.RemoteAuthnContext;
import pl.edu.icm.unity.saml.sp.SAMLExchange;
import pl.edu.icm.unity.saml.sp.SAMLSPProperties;
import pl.edu.icm.unity.saml.sp.SAMLSPProperties.Binding;
import pl.edu.icm.unity.saml.sp.SamlContextManagement;
import pl.edu.icm.unity.server.authn.AuthenticationException;
import pl.edu.icm.unity.server.authn.AuthenticationResult;
import pl.edu.icm.unity.server.authn.AuthenticationResult.Status;
import pl.edu.icm.unity.server.utils.Log;
import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.webui.authn.VaadinAuthentication.AuthenticationResultCallback;
import pl.edu.icm.unity.webui.authn.VaadinAuthentication.UsernameProvider;
import pl.edu.icm.unity.webui.authn.VaadinAuthentication.VaadinAuthenticationUI;
import pl.edu.icm.unity.webui.common.ErrorPopup;
import pl.edu.icm.unity.webui.common.Styles;
import xmlbeans.org.oasis.saml2.protocol.AuthnRequestDocument;

import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.server.Page;
import com.vaadin.server.RequestHandler;
import com.vaadin.server.Resource;
import com.vaadin.server.VaadinRequest;
import com.vaadin.server.VaadinServlet;
import com.vaadin.server.VaadinServletService;
import com.vaadin.server.VaadinSession;
import com.vaadin.server.WrappedSession;
import com.vaadin.shared.ui.label.ContentMode;
import com.vaadin.ui.Component;
import com.vaadin.ui.Label;
import com.vaadin.ui.OptionGroup;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.themes.Reindeer;

/**
 * The UI part of the remote SAML authn. Shows widget allowing to choose IdP (if more then one is configured)
 * starts the authN and awaits for answer in the context. When it is there, the validator is contacted for verification.
 * It is also possible to cancel the authentication which is in progress.
 * @author K. Benedyczak
 */
public class SAMLRetrievalUI implements VaadinAuthenticationUI
{	
	private Logger log = Log.getLogger(Log.U_SERVER_SAML, SAMLRetrievalUI.class);
	private UnityMessageSource msg;
	private SAMLExchange credentialExchange;
	private AuthenticationResultCallback callback;
	
	private String selectedIdp;
	private Label messageLabel;
	private Label errorDetailLabel;
	private SamlContextManagement samlContextManagement;
	
	
	public SAMLRetrievalUI(UnityMessageSource msg, SAMLExchange credentialExchange, 
			SamlContextManagement samlContextManagement)
	{
		this.msg = msg;
		this.credentialExchange = credentialExchange;
		this.samlContextManagement = samlContextManagement;
	}

	@Override
	public boolean needsCommonUsernameComponent()
	{
		return false;
	}

	@Override
	public Component getComponent()
	{
		installRequestHandler();
		
		final SAMLSPProperties samlProperties = credentialExchange.getSamlValidatorSettings();
		VerticalLayout ret = new VerticalLayout();
		ret.setSpacing(true);
		
		Label title = new Label(samlProperties.getValue(SAMLSPProperties.DISPLAY_NAME));
		title.addStyleName(Reindeer.LABEL_H2);
		ret.addComponent(title);
		
		Set<String> idps = samlProperties.getStructuredListKeys(SAMLSPProperties.IDP_PREFIX);
		if (idps.size() > 1)
		{
			OptionGroup idpChooser = new OptionGroup(msg.getMessage("WebSAMLRetrieval.selectIdp"));
			idpChooser.setImmediate(true);
			for (String idpKey: idps)
			{
				String name = samlProperties.getValue(idpKey+SAMLSPProperties.IDP_NAME);
				idpChooser.addItem(idpKey);
				idpChooser.setItemCaption(idpKey, name);
			}
			idpChooser.select(idps.iterator().next());
			idpChooser.setNullSelectionAllowed(false);
			idpChooser.addValueChangeListener(new ValueChangeListener()
			{
				@Override
				public void valueChange(ValueChangeEvent event)
				{
					selectedIdp = (String) event.getProperty().getValue();
				}
			});
			ret.addComponent(idpChooser);
		} else
		{
			String idpKey = idps.iterator().next();
			String name = samlProperties.getValue(idpKey+SAMLSPProperties.IDP_NAME);
			Label selectedIdp = new Label(msg.getMessage("WebSAMLRetrieval.selectedIdp", name));
			ret.addComponent(selectedIdp);
		}
		
		selectedIdp = idps.iterator().next();
		
		messageLabel = new Label();
		messageLabel.setContentMode(ContentMode.HTML);
		messageLabel.addStyleName(Styles.error.toString());
		errorDetailLabel = new Label();
		errorDetailLabel.setContentMode(ContentMode.HTML);
		errorDetailLabel.addStyleName(Styles.italic.toString());
		errorDetailLabel.setVisible(false);
		ret.addComponents(messageLabel, errorDetailLabel);

		return ret;
	}

	private void installRequestHandler()
	{
		VaadinSession session = VaadinSession.getCurrent();
		Collection<RequestHandler> requestHandlers = session.getRequestHandlers();
		boolean redirectInstalled = false;
		for (RequestHandler rh: requestHandlers)
		{
			if (rh instanceof RedirectRequestHandler)
			{
				redirectInstalled = true;
				break;
			}
			
		}
		if (!redirectInstalled)
			session.addRequestHandler(new RedirectRequestHandler());
	}
	
	private void breakLogin(boolean invokeCancel)
	{
		WrappedSession session = VaadinSession.getCurrent().getSession();
		RemoteAuthnContext context = (RemoteAuthnContext) session.getAttribute(
				SAMLRetrieval.REMOTE_AUTHN_CONTEXT);
		if (context != null)
		{
			session.removeAttribute(SAMLRetrieval.REMOTE_AUTHN_CONTEXT);
			samlContextManagement.removeAuthnContext(context.getRelayState());
		}
		if (invokeCancel)
			this.callback.cancelAuthentication();
	}
	
	private void showError(String message)
	{
		if (message == null)
		{
			messageLabel.setValue("");
			showErrorDetail(null);
			return;
		}
		messageLabel.setValue(message);
	}

	private void showErrorDetail(String message)
	{
		if (message == null)
		{
			errorDetailLabel.setVisible(false);
			errorDetailLabel.setValue("");
			return;
		}
		errorDetailLabel.setVisible(true);
		errorDetailLabel.setValue(message);
	}
	
	private void startLogin(String idpKey)
	{
		WrappedSession session = VaadinSession.getCurrent().getSession();
		RemoteAuthnContext context = (RemoteAuthnContext) session.getAttribute(
				SAMLRetrieval.REMOTE_AUTHN_CONTEXT);
		if (context != null)
		{
			ErrorPopup.showError(msg, msg.getMessage("error"), 
					msg.getMessage("WebSAMLRetrieval.loginInProgressError"));
			return;
		}
		context = new RemoteAuthnContext();
		session.setAttribute(SAMLRetrieval.REMOTE_AUTHN_CONTEXT, context);
		samlContextManagement.addAuthnContext(context);
		
		SAMLSPProperties samlProperties = credentialExchange.getSamlValidatorSettings();
		AuthnRequestDocument request;
		try
		{
			request = credentialExchange.createSAMLRequest(idpKey);
		} catch (Exception e)
		{
			ErrorPopup.showError(msg, msg.getMessage("WebSAMLRetrieval.configurationError"), e);
			log.error("Can not create SAML request", e);
			breakLogin(true);
			return;
		}
		Binding requestBinding = samlProperties.getEnumValue(idpKey + SAMLSPProperties.IDP_BINDING, 
				Binding.class);
		String servletPath = VaadinServlet.getCurrent().getServletContext().getContextPath() + 
				VaadinServletService.getCurrentServletRequest().getServletPath();
		String identityProviderURL = samlProperties.getValue(idpKey + SAMLSPProperties.IDP_ADDRESS);
		String groupAttribute = samlProperties.getValue(
				idpKey + SAMLSPProperties.IDP_GROUP_MEMBERSHIP_ATTRIBUTE);
		String registrationFormForUnknown = samlProperties.getValue(
				idpKey + SAMLSPProperties.IDP_REGISTRATION_FORM);
		String translationProfile = samlProperties.getValue(
				idpKey + SAMLSPProperties.IDP_TRANSLATION_PROFILE);
		context.setRequest(request.xmlText(), request.getAuthnRequest().getID(), 
				requestBinding, identityProviderURL, servletPath, groupAttribute, 
				registrationFormForUnknown, translationProfile);
		
		
		Page.getCurrent().open(servletPath + RedirectRequestHandler.PATH, null);
	}

	/**
	 * Called when a SAML response is received.
	 * @param authnContext
	 */
	private void onSamlAnswer(RemoteAuthnContext authnContext)
	{
		AuthenticationResult authnResult;
		showError(null);
		String reason = null;
		Exception savedException = null;
		try
		{
			authnResult = credentialExchange.verifySAMLResponse(authnContext);
		} catch (AuthenticationException e)
		{
			savedException = e;
			reason = ErrorPopup.getHumanMessage(e, "<br>");
			authnResult = e.getResult();
		} catch (Exception e)
		{
			log.error("Runtime error during SAML response processing or principal mapping", e);
			authnResult = new AuthenticationResult(Status.deny, null);
		}

		if (authnResult.getStatus() == Status.success)
		{
			showError(null);
			breakLogin(false);
		} else if (authnResult.getStatus() == Status.unknownRemotePrincipal && 
				authnContext.getRegistrationFormForUnknown() != null) 
		{
			log.debug("There is a registration form to show for the unknown user: " + 
					authnContext.getRegistrationFormForUnknown());
			authnResult.setFormForUnknownPrincipal(authnContext.getRegistrationFormForUnknown());
			showError(null);
			breakLogin(false);
		} else
		{
			if (savedException != null)
				log.warn("SAML response verification or processing failed", savedException);
			else
				log.warn("SAML response verification or processing failed");
			if (reason != null)
				showErrorDetail(msg.getMessage("WebSAMLRetrieval.authnFailedDetailInfo", reason));
			showError(msg.getMessage("WebSAMLRetrieval.authnFailedError"));
			breakLogin(false);
		}

		callback.setAuthenticationResult(authnResult);
	}
	
	@Override
	public void setUsernameCallback(UsernameProvider usernameCallback)
	{
	}

	@Override
	public void setAuthenticationResultCallback(AuthenticationResultCallback callback)
	{
		this.callback = callback;
	}

	@Override
	public void triggerAuthentication()
	{
		startLogin(selectedIdp);
	}

	@Override
	public void cancelAuthentication()
	{
		breakLogin(false);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void refresh(VaadinRequest request) 
	{
		WrappedSession session = request.getWrappedSession();
		RemoteAuthnContext context = (RemoteAuthnContext) session.getAttribute(
				SAMLRetrieval.REMOTE_AUTHN_CONTEXT);
		if (context == null)
		{
			log.trace("Either user refreshes page, or different authN arrived");
		} else if (context.getResponse() == null)
		{
			log.debug("Authentication started but SAML response not arrived (user back button)");
		} else 
		{
			onSamlAnswer(context);
		}
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getLabel()
	{	
		return credentialExchange.getSamlValidatorSettings().getValue(SAMLSPProperties.DISPLAY_NAME);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Resource getImage()
	{
		return null;
	}

	@Override
	public void clear()
	{
		//nop
	}
}