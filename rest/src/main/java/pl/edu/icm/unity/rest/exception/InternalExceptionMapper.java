/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.rest.exception;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import org.apache.log4j.Logger;

import pl.edu.icm.unity.exceptions.InternalException;
import pl.edu.icm.unity.server.utils.Log;

/**
 * Maps InternalException to HTTP error responses. No details are exposed to the client.
 * @author K. Benedyczak
 */
@Provider
public class InternalExceptionMapper implements ExceptionMapper<InternalException>
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_REST, InternalExceptionMapper.class);
	
	public Response toResponse(InternalException ex)
	{
		log.error("InternalException error during RESTful API invocation", ex);
		return Response.status(Status.INTERNAL_SERVER_ERROR).build();
	}
}
