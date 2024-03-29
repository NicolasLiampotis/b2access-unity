/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.events;

import java.lang.reflect.Proxy;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import pl.edu.icm.unity.server.api.NotificationsManagement;
import pl.edu.icm.unity.server.api.ServerManagement;

/**
 * Java dynamic proxy builder, decorating wrapped objects with event generation. This is the same as 
 * {@link EventProxyBuilder}, but for other classes - we can't have all in one builder as then we would easily 
 * get circular dependencies.
 *   
 * @author K. Benedyczak
 */
public class EventProxyBuilderSystem
{
	private static final ClassLoader classLoader = EventProxyBuilderSystem.class.getClassLoader();
	
	@Autowired @Qualifier("plain")
	private ServerManagement serverMan;
	@Autowired @Qualifier("plain")
	private NotificationsManagement notMan;
	
	@Autowired
	private EventProcessor eventProcessor;

	
	public ServerManagement getServerManagementInstance()
	{
		return (ServerManagement) Proxy.newProxyInstance(classLoader, 
				new Class[] {ServerManagement.class}, 
				new EventDecoratingHandler(serverMan, eventProcessor, 
						ServerManagement.class.getSimpleName()));
	}

	public NotificationsManagement getNotificationsManagementInstance()
	{
		return (NotificationsManagement) Proxy.newProxyInstance(classLoader, 
				new Class[] {NotificationsManagement.class}, 
				new EventDecoratingHandler(notMan, eventProcessor, 
						NotificationsManagement.class.getSimpleName()));
	}
}
