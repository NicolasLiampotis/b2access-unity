/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.restadm;

import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;

import pl.edu.icm.unity.rest.RESTEndpoint;
import pl.edu.icm.unity.restadm.exception.EngineExceptionMapper;
import pl.edu.icm.unity.restadm.exception.InternalExceptionMapper;
import pl.edu.icm.unity.restadm.exception.NPEExceptionMapper;
import pl.edu.icm.unity.restadm.exception.JSONExceptionMapper;
import pl.edu.icm.unity.server.api.AttributesManagement;
import pl.edu.icm.unity.server.api.GroupsManagement;
import pl.edu.icm.unity.server.api.IdentitiesManagement;
import pl.edu.icm.unity.server.api.internal.SessionManagement;
import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.types.endpoint.EndpointTypeDescription;

/**
 * RESTful endpoint providing administration and query API.
 * 
 * @author K. Benedyczak
 */
public class RESTAdminEndpoint extends RESTEndpoint
{
	private IdentitiesManagement identitiesMan;
	private GroupsManagement groupsMan;
	private AttributesManagement attributesMan;
	
	public RESTAdminEndpoint(UnityMessageSource msg, SessionManagement sessionMan,
			EndpointTypeDescription type, String servletPath, IdentitiesManagement identitiesMan,
			GroupsManagement groupsMan, AttributesManagement attributesMan)
	{
		super(msg, sessionMan, type, servletPath);
		this.identitiesMan = identitiesMan;
		this.groupsMan = groupsMan;
		this.attributesMan = attributesMan;
	}

	@Override
	protected Application getApplication()
	{
		return new RESTAdminJAXRSApp();
	}

	@ApplicationPath("/")
	public class RESTAdminJAXRSApp extends Application
	{
		@Override 
		public Set<Object> getSingletons() 
		{
			HashSet<Object> ret = new HashSet<>();
			ret.add(new RESTAdmin(identitiesMan, groupsMan, attributesMan));
			ret.add(new EngineExceptionMapper());
			ret.add(new NPEExceptionMapper());
			ret.add(new InternalExceptionMapper());
			ret.add(new JSONExceptionMapper());
			return ret;
		}
	}
}