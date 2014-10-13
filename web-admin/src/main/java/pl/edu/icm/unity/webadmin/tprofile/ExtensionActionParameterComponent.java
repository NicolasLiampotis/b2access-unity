/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.tprofile;

import pl.edu.icm.unity.server.translation.ActionParameterDesc;
import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.webadmin.tprofile.wizard.DragDropBean;

import com.vaadin.data.Validator.InvalidValueException;
import com.vaadin.data.util.BeanItem;
import com.vaadin.event.DataBoundTransferable;
import com.vaadin.event.FieldEvents.BlurEvent;
import com.vaadin.event.FieldEvents.BlurListener;
import com.vaadin.event.dd.DragAndDropEvent;
import com.vaadin.event.dd.DropHandler;
import com.vaadin.event.dd.acceptcriteria.AcceptAll;
import com.vaadin.event.dd.acceptcriteria.AcceptCriterion;
import com.vaadin.ui.Component;
import com.vaadin.ui.CustomField;
import com.vaadin.ui.DragAndDropWrapper;

/**
 * 
 * In high level view, this class implements the {@link DefaultActionParameterComponent} with
 * drag'n'drop support.
 *  
 * @author Roman Krysinski
 *
 */
public class ExtensionActionParameterComponent extends CustomField<String> implements ActionParameterComponent
{
	DefaultActionParameterComponent parameter;

	public ExtensionActionParameterComponent(ActionParameterDesc desc, UnityMessageSource msg) 
	{
		parameter = new DefaultActionParameterComponent(desc, msg);
		setCaption(parameter.getCaption());
		setDescription(parameter.getDescription());
		setRequired(parameter.isRequired());
		setRequiredError(parameter.getRequiredError());
		parameter.addBlurListener(new BlurListener() 
		{
			@Override
			public void blur(BlurEvent event) 
			{
				markAsDirtyRecursive();
			}
		});
	}

	@Override
	public String getActionValue() 
	{
		return getInternalValue();
	}

	@Override
	public void setActionValue(String value) 
	{
		setInternalValue(value);
	}

	@Override
	public void validate() throws InvalidValueException 
	{
		parameter.validate();
	}

	@Override
	public boolean isValid() 
	{
		return parameter.isValid();
	}

	@Override
	protected String getInternalValue() 
	{
		return parameter.getValue();
	}

	@Override
	protected void setInternalValue(String newValue) 
	{
		parameter.setValue(newValue);
	}
	
	@Override
	protected Component initContent() 
	{
		DragAndDropWrapper wrapper = new DragAndDropWrapper(parameter);
		
		wrapper.setDropHandler(new DropHandler() 
		{
			@Override
			public AcceptCriterion getAcceptCriterion() 
			{
				return AcceptAll.get();
			}
			
			@Override
			public void drop(DragAndDropEvent event) 
			{
				DataBoundTransferable t = (DataBoundTransferable) event.getTransferable();
				Object sourceItemId = t.getData("itemId");
				
				String source = "";
				if (sourceItemId instanceof BeanItem<?>)
				{
					Object bean = ((BeanItem<?>) sourceItemId).getBean();
					source = ((DragDropBean) bean).getExpression();
				} else if (sourceItemId instanceof DragDropBean)
				{
					source = ((DragDropBean) sourceItemId).getExpression();
				}
				String newValue = parameter.getValue() + source;
				parameter.setValue(newValue);
				parameter.focus();
			}
		});
		return wrapper;
	}


	@Override
	public Class<String> getType() 
	{
		return String.class;
	}
	
	@Override
	public void setStyleName(String style) 
	{
		super.setStyleName(style);
		parameter.setStyleName(style);
	}
	
	@Override
	public void removeStyleName(String style) 
	{
		super.removeStyleName(style);
		parameter.removeStyleName(style);
	}
}
