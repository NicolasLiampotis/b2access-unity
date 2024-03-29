/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.stdext.tactions.in;

import java.io.Serializable;
import java.util.Collection;

import org.apache.log4j.Logger;
import org.mvel2.MVEL;

import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.server.authn.remote.RemotelyAuthenticatedInput;
import pl.edu.icm.unity.server.translation.TranslationActionDescription;
import pl.edu.icm.unity.server.translation.in.AbstractInputTranslationAction;
import pl.edu.icm.unity.server.translation.in.GroupEffectMode;
import pl.edu.icm.unity.server.translation.in.MappedGroup;
import pl.edu.icm.unity.server.translation.in.MappingResult;
import pl.edu.icm.unity.server.utils.Log;

/**
 * Create group mappings.
 *   
 * @author K. Benedyczak
 */
public class MapGroupAction extends AbstractInputTranslationAction
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_TRANSLATION, MapGroupAction.class);
	private Serializable expressionCompiled;
	private GroupEffectMode groupEffect = GroupEffectMode.REQUIRE_EXISTING_GROUP;

	public MapGroupAction(String[] params, TranslationActionDescription desc)
	{
		super(desc, params);
		setParameters(params);
	}
	
	@Override
	protected MappingResult invokeWrapped(RemotelyAuthenticatedInput input, Object mvelCtx, String currentProfile) 
			throws EngineException
	{
		MappingResult ret = new MappingResult();
		Object result = MVEL.executeExpression(expressionCompiled, mvelCtx);
		if (result == null)
		{
			log.debug("Group evaluated to null, skipping");
			return ret;
		}
		if (result instanceof Collection<?>)
		{
			Collection<?> mgs = (Collection<?>) result;
			for (Object mg: mgs)
			{
				log.debug("Mapped group: " + mg.toString());
				ret.addGroup(new MappedGroup(mg.toString(), groupEffect, input.getIdpName(),
						currentProfile));
			}
		} else
		{
			log.debug("Mapped group: " + result.toString());
			ret.addGroup(new MappedGroup(result.toString(), groupEffect, input.getIdpName(), 
					currentProfile));
		}
		return ret;
	}

	private void setParameters(String[] parameters)
	{
		if (parameters.length < 1 || parameters.length > 2)
			throw new IllegalArgumentException("Action requires 2 parameters");
		expressionCompiled = MVEL.compileExpression(parameters[0]);
		if (parameters.length > 1)
			groupEffect = GroupEffectMode.valueOf(parameters[1]);
		else
			groupEffect = GroupEffectMode.REQUIRE_EXISTING_GROUP;
	}
}
