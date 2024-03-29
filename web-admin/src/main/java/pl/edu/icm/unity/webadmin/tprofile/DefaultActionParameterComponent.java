/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.tprofile;

import pl.edu.icm.unity.server.translation.ActionParameterDesc;
import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.webui.common.RequiredTextField;

import com.vaadin.ui.TextField;

/**
 * Trivial, {@link TextField} based implementation of {@link ActionParameterComponent}. 
 * @author K. Benedyczak
 */
public class DefaultActionParameterComponent extends RequiredTextField implements ActionParameterComponent
{
	public DefaultActionParameterComponent(ActionParameterDesc desc, UnityMessageSource msg)
	{
		super(desc.getName() + ":", msg);
		setDescription(msg.getMessage(desc.getDescriptionKey()));
		setColumns(40);
	}
	
	@Override
	public String getActionValue()
	{
		return getValue();
	}

	@Override
	public void setActionValue(String value)
	{
		setValue(value);
	}
}
