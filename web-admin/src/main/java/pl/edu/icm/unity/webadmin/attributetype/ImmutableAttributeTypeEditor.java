/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.attributetype;

import pl.edu.icm.unity.exceptions.IllegalAttributeTypeException;
import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.types.I18nString;
import pl.edu.icm.unity.types.basic.AttributeType;
import pl.edu.icm.unity.types.basic.AttributeVisibility;
import pl.edu.icm.unity.webui.common.EnumComboBox;
import pl.edu.icm.unity.webui.common.i18n.I18nTextArea;
import pl.edu.icm.unity.webui.common.i18n.I18nTextField;

import com.vaadin.ui.CheckBox;
import com.vaadin.ui.Component;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.Label;

/**
 * Allows to edit an attribute type which has immutable type. For such attributes only displayed name
 * and description can be edited. Creation of an attribute type is not possible. 
 * 
 * @author K. Benedyczak
 */
public class ImmutableAttributeTypeEditor extends FormLayout implements AttributeTypeEditor
{
	private UnityMessageSource msg;
	
	private AttributeType original;
	private Label name;
	private I18nTextField displayedName;
	private I18nTextArea typeDescription;
	private CheckBox selfModificable;
	private EnumComboBox<AttributeVisibility> visibility;
	
	public ImmutableAttributeTypeEditor(UnityMessageSource msg, AttributeType toEdit)
	{
		super();
		this.msg = msg;
		original = toEdit;
		
		initUI(toEdit);
	}

	private void initUI(AttributeType toEdit)
	{
		setWidth(100, Unit.PERCENTAGE);

		name = new Label(toEdit.getName());
		name.setCaption(msg.getMessage("AttributeType.name"));
		addComponent(name);
		
		displayedName = new I18nTextField(msg, msg.getMessage("AttributeType.displayedName"));
		addComponent(displayedName);
		
		typeDescription = new I18nTextArea(msg, msg.getMessage("AttributeType.description"));
		addComponent(typeDescription);
		
		selfModificable = new CheckBox(msg.getMessage("AttributeType.selfModificableCheck"));
		addComponent(selfModificable);
		
		visibility = new EnumComboBox<AttributeVisibility>(msg, "AttributeType.visibility.", 
				AttributeVisibility.class, AttributeVisibility.full);
		visibility.setCaption(msg.getMessage("AttributeType.visibility"));
		visibility.setSizeUndefined();
		addComponent(visibility);
		
		setInitialValues(toEdit);
	}
	
	private void setInitialValues(AttributeType aType)
	{
		typeDescription.setValue(aType.getDescription());
		displayedName.setValue(aType.getDisplayedName());
		selfModificable.setValue(aType.isSelfModificable());
		visibility.setEnumValue(aType.getVisibility());
	}
	
	@Override
	public AttributeType getAttributeType() throws IllegalAttributeTypeException
	{
		AttributeType ret = new AttributeType();
		ret.setDescription(typeDescription.getValue());
		ret.setName(name.getValue());
		I18nString displayedNameS = displayedName.getValue();
		displayedNameS.setDefaultValue(ret.getName());
		ret.setDisplayedName(displayedNameS);
		ret.setValueType(original.getValueType());
		ret.setSelfModificable(selfModificable.getValue());
		ret.setVisibility(visibility.getSelectedValue());
		return ret;
	}

	@Override
	public Component getComponent()
	{
		return this;
	}
}
