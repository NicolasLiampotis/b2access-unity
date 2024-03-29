/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.stdext.tactions.out;

import org.apache.log4j.Logger;

import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.server.translation.ExecutionFailException;
import pl.edu.icm.unity.server.translation.TranslationActionDescription;
import pl.edu.icm.unity.server.translation.out.AbstractOutputTranslationAction;
import pl.edu.icm.unity.server.translation.out.TranslationInput;
import pl.edu.icm.unity.server.translation.out.TranslationResult;
import pl.edu.icm.unity.server.utils.Log;

/**
 * Fails the authentication. Allows for implementing poorman's authZ. 
 *   
 * @author K. Benedyczak
 */
public class FailAuthnAction extends AbstractOutputTranslationAction
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_TRANSLATION, FailAuthnAction.class);
	private String error;

	public FailAuthnAction(String[] params, TranslationActionDescription desc) 
			throws EngineException
	{
		super(desc, params);
		setParameters(params);
	}

	@Override
	protected void invokeWrapped(TranslationInput input, Object mvelCtx, String currentProfile,
			TranslationResult result) throws EngineException
	{
		log.debug("Authentication will be failed with message: " + error);
		throw new ExecutionFailException(error);
	}

	private void setParameters(String[] parameters)
	{
		if (parameters.length != 1)
			throw new IllegalArgumentException("Action requires exactly 1 parameter");
		error = parameters[0];
	}
}
