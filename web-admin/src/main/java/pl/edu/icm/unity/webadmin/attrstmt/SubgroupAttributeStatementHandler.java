/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.attrstmt;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;

import pl.edu.icm.unity.server.api.GroupsManagement;
import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.AttributeStatement;
import pl.edu.icm.unity.types.basic.AttributeType;
import pl.edu.icm.unity.types.basic.attrstmnt.HasSubgroupAttributeStatement;
import pl.edu.icm.unity.webadmin.groupbrowser.GroupComboBox;
import pl.edu.icm.unity.webui.common.FormValidationException;
import pl.edu.icm.unity.webui.common.attributes.AttributeHandlerRegistry;

/**
 * Factory for Web UI code supporting {@link HasSubgroupAttributeStatement}.  
 * @author K. Benedyczak
 */
@org.springframework.stereotype.Component
public class SubgroupAttributeStatementHandler implements AttributeStatementWebHandlerFactory
{
	private UnityMessageSource msg;
	private AttributeHandlerRegistry handlersReg;
	private GroupsManagement groupsMan;
	
	
	@Autowired
	public SubgroupAttributeStatementHandler(UnityMessageSource msg,
			AttributeHandlerRegistry handlersReg, GroupsManagement groupsMan)
	{
		this.msg = msg;
		this.handlersReg = handlersReg;
		this.groupsMan = groupsMan;
	}

	@Override
	public AttributeStatementComponent getEditorComponent(List<AttributeType> attributeTypes, String group)
	{
		return new SubgroupAttributeStatementComponent(msg, handlersReg, attributeTypes, group);
	}

	@Override
	public String getTextRepresentation(AttributeStatement as)
	{
		StringBuilder sb = EverybodyStatementHandler.getAssignedAttributeText(msg, handlersReg, as);

		Attribute<?> a = as.getConditionAttribute();
		String condAttrStr = handlersReg.getSimplifiedAttributeRepresentation(a,
				EverybodyStatementHandler.ATTR_LEN);
		sb.append(msg.getMessage("AttributeStatements.hasSubgroupAttribute")).append(" ").append(condAttrStr);
		sb.append(" ").append(msg.getMessage("AttributeStatements.inGroup")).append(" ");
		sb.append(a.getGroupPath());
		return sb.toString();
	}

	@Override
	public String getSupportedAttributeStatementName()
	{
		return HasSubgroupAttributeStatement.NAME;
	}
	
	public class SubgroupAttributeStatementComponent extends AbstractAttributeStatementComponent
	{
		private GroupComboBox conditionGroup;
		
		public SubgroupAttributeStatementComponent(UnityMessageSource msg,
				AttributeHandlerRegistry attrHandlerRegistry,
				List<AttributeType> attributeTypes, String group)
		{
			super(msg, attrHandlerRegistry, attributeTypes, group,
					new HasSubgroupAttributeStatement().getDescription());
			addAssignedAttributeField();
			addConditionAttributeField();
			conditionGroup = new GroupComboBox(msg.getMessage("AttributeStatementEditDialog.inGroup"), 
					groupsMan);
			conditionGroup.setInput(group, false);
			main.addComponent(conditionGroup);
		}

		@Override
		public AttributeStatement getStatementFromComponent()
				throws FormValidationException
		{
			HasSubgroupAttributeStatement ret = new HasSubgroupAttributeStatement();
			ret.setAssignedAttribute(getAssignedAttribute());
			Attribute<?> condAttr = getConditionAttribute();
			condAttr.setGroupPath((String) conditionGroup.getValue());
			ret.setConditionAttribute(condAttr);
			return ret;
		}

		@Override
		public void setInitialData(AttributeStatement initial)
		{
			setAssignedAttribute(initial.getAssignedAttribute());
			Attribute<?> condAttr = initial.getConditionAttribute();
			if (condAttr != null)
			{
				conditionGroup.setValue(condAttr.getGroupPath());
				setConditionAttribute(condAttr);
			}
			
		}
	}
}