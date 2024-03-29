/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.common.attributes;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.vaadin.shared.ui.MarginInfo;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;

import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.AttributeValueSyntax;
import pl.edu.icm.unity.webui.common.Styles;
import pl.edu.icm.unity.webui.common.attributes.WebAttributeHandler.RepresentationSize;

/**
 * Gives access to web attribute handlers for given syntax types.
 * Additionally a methods are provided to easily get a simplified attribute representation for the given 
 * attribute.
 * 
 * @author K. Benedyczak
 */
@Component
public class AttributeHandlerRegistry
{
	private UnityMessageSource msg;
	private Map<String, WebAttributeHandlerFactory> factoriesByType = new HashMap<>();
	public static final int DEFAULT_MAX_LEN = 16;
	
	@Autowired
	public AttributeHandlerRegistry(List<WebAttributeHandlerFactory> factories, UnityMessageSource msg)
	{
		this.msg = msg;
		for (WebAttributeHandlerFactory factory: factories)
			factoriesByType.put(factory.getSupportedSyntaxId(), factory);
	}
	
	public WebAttributeHandler<?> getHandler(String syntaxId)
	{
		WebAttributeHandlerFactory factory = factoriesByType.get(syntaxId);
		if (factory == null)
			throw new IllegalArgumentException("SyntaxId " + syntaxId + " has no handler factory registered");
		return factory.createInstance();
	}
	
	@SuppressWarnings("unchecked")
	public com.vaadin.ui.Component getRepresentation(Attribute<?> attribute, RepresentationSize size)
	{
		VerticalLayout vl = new VerticalLayout();
		vl.addStyleName(Styles.smallSpacing.toString());
		AttributeValueSyntax<?> syntax = attribute.getAttributeSyntax();
		StringBuilder main = new StringBuilder(attribute.getName());
		if (attribute.getRemoteIdp() != null)
		{
			String idpInfo = msg.getMessage("IdentityFormatter.remoteInfo", attribute.getRemoteIdp());
			main.append(" [").append(idpInfo).append("]");
		}
		vl.addComponent(new Label(main.toString()));
		VerticalLayout indentedValues = new VerticalLayout();
		indentedValues.setMargin(new MarginInfo(false, false, false, true));
		@SuppressWarnings("rawtypes")
		WebAttributeHandler handler = getHandler(syntax.getValueSyntaxId());
		for (Object value: attribute.getValues())
			indentedValues.addComponent(handler.getRepresentation(value, syntax, size));
		vl.addComponent(indentedValues);
		return vl;
	}
	
	public Set<String> getSupportedSyntaxes()
	{
		return new HashSet<>(factoriesByType.keySet());
	}
	
	public String getSimplifiedAttributeRepresentation(Attribute<?> attribute, int maxValuesLen)
	{
		return getSimplifiedAttributeRepresentation(attribute, maxValuesLen, attribute.getName());
	}
	
	/**
	 * Returns a string representing the attribute. The returned format contains the attribute name
	 * and the values. If the values can not be put in the remaining text len, then are shortened.
	 * @param attribute
	 * @param maxValuesLen max values length, not less then 16
	 * @return
	 */
	public String getSimplifiedAttributeRepresentation(Attribute<?> attribute, int maxValuesLen, 
			String displayedName)
	{
		StringBuilder sb = new StringBuilder();
		sb.append(displayedName);
		List<?> values = attribute.getValues();
		if (values.size() > 0)
		{
			sb.append(": ");
			sb.append(getSimplifiedAttributeValuesRepresentation(attribute, maxValuesLen));
		}
		return sb.toString();
	}
	
	/**
	 * Returns a string representing the attributes values. The length of the values
	 * string is limited by the argument. When some of the values can not be displayed, then 
	 * ... is appended.
	 * @param attribute
	 * @return
	 */
	public String getSimplifiedAttributeValuesRepresentation(Attribute<?> attribute, int maxValuesLen)
	{
		if (maxValuesLen < 16)
			throw new IllegalArgumentException("The max length must be lager then 16");
		StringBuilder sb = new StringBuilder();
		List<?> values = attribute.getValues();
		AttributeValueSyntax<?> syntax = attribute.getAttributeSyntax();
		@SuppressWarnings("rawtypes")
		WebAttributeHandler handler = getHandler(syntax.getValueSyntaxId());
		int remainingLen = maxValuesLen;
		final String MORE_VALS = ", ...";
		final int moreValsLen = MORE_VALS.length();
		
		for (int i=0; i<values.size(); i++)
		{
			int allowedLen = i == (values.size()-1) ? remainingLen : remainingLen-moreValsLen;
			if (allowedLen < WebAttributeHandler.MIN_VALUE_TEXT_LEN)
			{
				sb.append(", ...");
				break;
			}
			@SuppressWarnings("unchecked")
			String val = handler.getValueAsString(values.get(i), syntax, allowedLen);
			remainingLen -= val.length(); 
			if (i > 0)
			{
				sb.append(", ");
				remainingLen -= 2;
			}
			sb.append(val);
		}
		return sb.toString();
	}
}



