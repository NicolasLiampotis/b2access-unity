/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui;

import java.io.ByteArrayInputStream;
import java.io.CharArrayWriter;
import java.io.IOException;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.servlet.DispatcherType;
import javax.servlet.Servlet;

import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.springframework.context.ApplicationContext;

import com.vaadin.server.VaadinServlet;

import eu.unicore.util.configuration.ConfigurationException;
import pl.edu.icm.unity.server.api.internal.SessionManagement;
import pl.edu.icm.unity.server.authn.LoginToHttpSessionBinder;
import pl.edu.icm.unity.server.endpoint.AbstractEndpoint;
import pl.edu.icm.unity.server.endpoint.BindingAuthn;
import pl.edu.icm.unity.server.endpoint.EndpointFactory;
import pl.edu.icm.unity.server.endpoint.WebAppEndpointInstance;
import pl.edu.icm.unity.types.endpoint.EndpointTypeDescription;
import pl.edu.icm.unity.webui.authn.AuthenticationFilter;
import pl.edu.icm.unity.webui.authn.AuthenticationUI;

/**
 * Vaadin endpoint is used by all Vaadin based web endpoints. It is not a component:
 * concrete endpoint will define a custom {@link EndpointFactory} returning this class 
 * object initialized with the actual Vaadin application which should be exposed. 
 * @author K. Benedyczak
 */
public class VaadinEndpoint extends AbstractEndpoint implements WebAppEndpointInstance
{
	public static final String AUTHENTICATION_PATH = "/authentication";
	public static final String VAADIN_RESOURCES = "/VAADIN/*";
	public static final String SESSION_TIMEOUT_PARAM = "session-timeout";
	public static final String PRODUCTION_MODE_PARAM = "productionMode";
	protected ApplicationContext applicationContext;
	protected String uiBeanName;
	protected String servletPath;
	protected Properties properties;
	protected VaadinEndpointProperties genericEndpointProperties;

	protected ServletContextHandler context = null;
	protected UnityVaadinServlet theServlet;
	protected UnityVaadinServlet authenticationServlet;
	
	public VaadinEndpoint(EndpointTypeDescription type, ApplicationContext applicationContext,
			String uiBeanName, String servletPath)
	{
		super(type);
		this.applicationContext = applicationContext;
		this.uiBeanName = uiBeanName;
		this.servletPath = servletPath;
	}
	

	@Override
	public String getSerializedConfiguration()
	{
		CharArrayWriter writer = new CharArrayWriter();
		try
		{
			properties.store(writer, "");
		} catch (IOException e)
		{
			throw new IllegalStateException("Can not serialize endpoint's configuration", e);
		}
		return writer.toString();
	}

	@Override
	public void setSerializedConfiguration(String cfg)
	{
		properties = new Properties();
		try
		{
			properties.load(new ByteArrayInputStream(cfg.getBytes()));
			genericEndpointProperties = new VaadinEndpointProperties(properties);
		} catch (Exception e)
		{
			throw new ConfigurationException("Can't initialize the the generic IdP endpoint's configuration", e);
		}
	}

	@Override
	public synchronized ServletContextHandler getServletContextHandler()
	{
		if (context != null)
			return context;
	 	
		context = new ServletContextHandler(ServletContextHandler.SESSIONS);
		context.setContextPath(description.getContextAddress());
		
		SessionManagement sessionMan = applicationContext.getBean(SessionManagement.class);
		LoginToHttpSessionBinder sessionBinder = applicationContext.getBean(LoginToHttpSessionBinder.class);
		
		AuthenticationFilter authnFilter = new AuthenticationFilter(servletPath, 
				description.getContextAddress()+AUTHENTICATION_PATH, description.getRealm().getName(),
				sessionMan, sessionBinder);
		context.addFilter(new FilterHolder(authnFilter), "/*", EnumSet.of(DispatcherType.REQUEST));

		EndpointRegistrationConfiguration registrationConfiguration = getRegistrationConfiguration();

		authenticationServlet = new UnityVaadinServlet(applicationContext, 
				AuthenticationUI.class.getSimpleName(), description, authenticators, 
				registrationConfiguration);
		ServletHolder authnServletHolder = createVaadinServletHolder(authenticationServlet, true);
		authnServletHolder.setInitParameter("closeIdleSessions", "true");
		context.addServlet(authnServletHolder, AUTHENTICATION_PATH+"/*");
		context.addServlet(authnServletHolder, VAADIN_RESOURCES);
		
		theServlet = new UnityVaadinServlet(applicationContext, uiBeanName,
				description, authenticators, registrationConfiguration);
		context.addServlet(createVaadinServletHolder(theServlet, false), servletPath + "/*");

		return context;
	}

	protected int getHeartbeatInterval(int sessionTimeout)
	{
		return (sessionTimeout < 20) ? (sessionTimeout/2) : 10;
	}
	
	protected ServletHolder createServletHolder(Servlet servlet, boolean unrestrictedSessionTime)
	{
		ServletHolder holder = new ServletHolder(servlet);
		if (unrestrictedSessionTime)
		{
			holder.setInitParameter("closeIdleSessions", "false");
			holder.setInitParameter(SESSION_TIMEOUT_PARAM, String.valueOf(-1));
		} else
		{
			holder.setInitParameter("closeIdleSessions", "true");
			int sessionTimeout = description.getRealm().getMaxInactivity();
			int heartBeat = getHeartbeatInterval(sessionTimeout);
			if (sessionTimeout > heartBeat + 10)
			{
				sessionTimeout = sessionTimeout - heartBeat - 5;
			} else
			{
				sessionTimeout -= 5;
				if (sessionTimeout < 5)
					sessionTimeout = 5;
			}
			holder.setInitParameter(SESSION_TIMEOUT_PARAM, String.valueOf(sessionTimeout));
		}
		return holder;
	}
	
	protected ServletHolder createVaadinServletHolder(VaadinServlet servlet, boolean unrestrictedSessionTime)
	{
		ServletHolder holder = createServletHolder(servlet, unrestrictedSessionTime);
		int sessionTimeout = description.getRealm().getMaxInactivity();
		int heartBeat = getHeartbeatInterval(sessionTimeout);
			
		boolean productionMode = genericEndpointProperties.getBooleanValue(VaadinEndpointProperties.PRODUCTION_MODE);
		holder.setInitParameter("heartbeatInterval", String.valueOf(heartBeat));
		holder.setInitParameter(PRODUCTION_MODE_PARAM, String.valueOf(productionMode));
		return holder;
	}

	protected EndpointRegistrationConfiguration getRegistrationConfiguration()
	{
		return new EndpointRegistrationConfiguration(genericEndpointProperties.getListOfValues(
				VaadinEndpointProperties.ENABLED_REGISTRATION_FORMS),
				genericEndpointProperties.getBooleanValue(VaadinEndpointProperties.ENABLE_REGISTRATION));
	}
	
	@Override
	public synchronized void updateAuthenticators(List<Map<String, BindingAuthn>> authenticators)
	{
		setAuthenticators(authenticators);
		if (authenticationServlet != null)
		{
			authenticationServlet.updateAuthenticators(authenticators);
			theServlet.updateAuthenticators(authenticators);
		}
	}
}
