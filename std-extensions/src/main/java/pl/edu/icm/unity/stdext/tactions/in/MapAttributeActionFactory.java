/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.stdext.tactions.in;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.server.api.AttributesManagement;
import pl.edu.icm.unity.server.translation.ActionParameterDesc;
import pl.edu.icm.unity.server.translation.ProfileType;
import pl.edu.icm.unity.server.translation.ActionParameterDesc.Type;
import pl.edu.icm.unity.server.translation.TranslationActionFactory;
import pl.edu.icm.unity.server.translation.in.AttributeEffectMode;
import pl.edu.icm.unity.server.translation.in.InputTranslationAction;
import pl.edu.icm.unity.types.basic.AttributeVisibility;

/**
 * Factory for {@link MapAttributeAction}.
 *   
 * @author K. Benedyczak
 */
@Component
public class MapAttributeActionFactory implements TranslationActionFactory
{
	public static final String NAME = "mapAttribute";
	private AttributesManagement attrsMan;
	
	@Autowired
	public MapAttributeActionFactory(@Qualifier("insecure") AttributesManagement attrsMan)
	{
		this.attrsMan = attrsMan;
	}

	@Override
	public String getName()
	{
		return NAME;
	}

	@Override
	public String getDescriptionKey()
	{
		return "TranslationAction.mapAttribute.desc";
	}

	@Override
	public ActionParameterDesc[] getParameters()
	{
		return new ActionParameterDesc[] {
				new ActionParameterDesc(
						"unityAttribute",
						"TranslationAction.mapAttribute.paramDesc.unityAttribute",
						1, 1, Type.UNITY_ATTRIBUTE),
				new ActionParameterDesc(
						"group",
						"TranslationAction.mapAttribute.paramDesc.group",
						1, 1, Type.UNITY_GROUP),
				new ActionParameterDesc(
						"expression",
						"TranslationAction.mapAttribute.paramDesc.expression",
						1, 1, Type.EXPRESSION),
				new ActionParameterDesc(
						"visibility",
						"TranslationAction.mapAttribute.paramDesc.visibility",
						1, 1, AttributeVisibility.class),
				new ActionParameterDesc(
						"effect",
						"TranslationAction.mapAttribute.paramDesc.effect",
						1, 1, AttributeEffectMode.class)};
	}

	@Override
	public InputTranslationAction getInstance(String... parameters) throws EngineException
	{
		return new MapAttributeAction(parameters, this, attrsMan);
	}

	@Override
	public ProfileType getSupportedProfileType()
	{
		return ProfileType.INPUT;
	}
}
