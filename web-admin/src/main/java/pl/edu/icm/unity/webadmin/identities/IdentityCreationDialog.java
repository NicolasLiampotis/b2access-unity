/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.identities;

import java.util.Set;

import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.Panel;

import pl.edu.icm.unity.exceptions.IllegalIdentityValueException;
import pl.edu.icm.unity.server.api.IdentitiesManagement;
import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.types.basic.EntityParam;
import pl.edu.icm.unity.types.basic.IdentityParam;
import pl.edu.icm.unity.webui.common.AbstractDialog;
import pl.edu.icm.unity.webui.common.ErrorPopup;
import pl.edu.icm.unity.webui.common.identities.IdentityEditor;
import pl.edu.icm.unity.webui.common.identities.IdentityEditorRegistry;

/**
 * Identity creation dialog. Adds the identity to an existing entity.
 * @author K. Benedyczak
 */
public class IdentityCreationDialog extends AbstractDialog
{
	private String entityId;
	protected IdentitiesManagement identitiesMan;
	protected IdentityEditorRegistry identityEditorReg;
	protected Callback callback;
	
	protected ComboBox identityType;
	protected Panel identityPanel;
	protected IdentityEditor identityEditor;
	protected CheckBox extractAttributes;
	protected CheckBox disable;
	
	public IdentityCreationDialog(UnityMessageSource msg, String entityId, IdentitiesManagement identitiesMan,
			IdentityEditorRegistry identityEditorReg, Callback callback)
	{
		this(msg.getMessage("IdentityCreation.caption"), msg, identitiesMan, identityEditorReg, callback);
		this.entityId = entityId;
	}

	protected IdentityCreationDialog(String caption, UnityMessageSource msg, IdentitiesManagement identitiesMan,
			IdentityEditorRegistry identityEditorReg, Callback callback)
	{
		super(msg, caption);
		this.identityEditorReg = identityEditorReg;
		this.identitiesMan = identitiesMan;
		this.callback = callback;
		this.defaultSizeUndfined = true;
	}

	
	@Override
	protected FormLayout getContents()
	{
		identityType = new ComboBox(msg.getMessage("IdentityCreation.idType"));
		Set<String> supportedTypes = identityEditorReg.getSupportedTypes();
		for (String type: supportedTypes)
			identityType.addItem(type);
		identityType.setNullSelectionAllowed(false);
		identityType.setImmediate(true);

		identityPanel = new Panel(msg.getMessage("IdentityCreation.idValue"));
		
		identityType.addValueChangeListener(new ValueChangeListener()
		{
			@Override
			public void valueChange(ValueChangeEvent event)
			{
				String type = (String) identityType.getValue();
				IdentityEditor editor = identityEditorReg.getEditor(type);
				identityPanel.setContent(editor.getEditor());
				IdentityCreationDialog.this.identityEditor = editor;
			}
		});
		identityType.select(supportedTypes.iterator().next());

		extractAttributes = new CheckBox(msg.getMessage("IdentityCreation.extractAttrs"), true);

		disable = new CheckBox(msg.getMessage("IdentityCreation.disable"));
		
		
		FormLayout main = new FormLayout();
		main.addComponents(identityType, identityPanel, extractAttributes, disable);
		main.setSizeFull();
		return main;
	}

	@Override
	protected void onConfirm()
	{
		String value;
		try
		{
			value = identityEditor.getValue();
		} catch (IllegalIdentityValueException e)
		{
			return;
		}
		String type = (String) identityType.getValue();
		IdentityParam toAdd = new IdentityParam(type, value, !disable.getValue(), true);
		try
		{
			identitiesMan.addIdentity(toAdd, new EntityParam(entityId), 
					extractAttributes.getValue());
		} catch (Exception e)
		{
			ErrorPopup.showError(msg.getMessage("IdentityCreation.entityCreateError"), e);
			return;
		}
		
		callback.onCreated();
		close();
	}
	
	public interface Callback 
	{
		public void onCreated();
	}
}