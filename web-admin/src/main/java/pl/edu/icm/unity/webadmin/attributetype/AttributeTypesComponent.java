/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.attributetype;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.server.api.AttributesManagement;
import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.types.basic.AttributeType;
import pl.edu.icm.unity.webadmin.attributetype.AttributeTypeEditDialog.Callback;
import pl.edu.icm.unity.webadmin.attributetype.AttributeTypesTable.AttributeTypeItem;
import pl.edu.icm.unity.webui.WebSession;
import pl.edu.icm.unity.webui.bus.EventsBus;
import pl.edu.icm.unity.webui.common.ConfirmWithOptionDialog;
import pl.edu.icm.unity.webui.common.ErrorComponent;
import pl.edu.icm.unity.webui.common.ErrorPopup;
import pl.edu.icm.unity.webui.common.Images;
import pl.edu.icm.unity.webui.common.SingleActionHandler;
import pl.edu.icm.unity.webui.common.attributes.AttributeHandlerRegistry;
import pl.edu.icm.unity.webui.common.attributes.WebAttributeHandler;

import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.event.Action;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Panel;

/**
 * Responsible for attribute types management.
 * @author K. Benedyczak
 */
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class AttributeTypesComponent extends Panel
{
	private UnityMessageSource msg;
	private AttributesManagement attrManagement;
	private AttributeHandlerRegistry attrHandlerRegistry;
	
	private AttributeTypesTable table;
	private AttributeTypeViewer viewer;
	private com.vaadin.ui.Component main;
	private EventsBus bus;
	
	
	@Autowired
	public AttributeTypesComponent(UnityMessageSource msg, AttributesManagement attrManagement, 
			AttributeHandlerRegistry attrHandlerRegistry)
	{
		this.msg = msg;
		this.attrManagement = attrManagement;
		this.attrHandlerRegistry = attrHandlerRegistry;
		this.bus = WebSession.getCurrent().getEventBus();
		HorizontalLayout hl = new HorizontalLayout();
		
		setCaption(msg.getMessage("AttributeTypes.caption"));
		table = new AttributeTypesTable(msg, attrManagement);
		hl.addComponent(table);
		viewer = new AttributeTypeViewer(msg);
		hl.addComponent(viewer);
		table.addValueChangeListener(new ValueChangeListener()
		{
			@Override
			public void valueChange(ValueChangeEvent event)
			{
				AttributeTypeItem item = (AttributeTypeItem)table.getValue();
				if (item != null)
				{
					AttributeType at = item.getAttributeType();
					WebAttributeHandler<?> handler = AttributeTypesComponent.this.attrHandlerRegistry.getHandler(
							at.getValueType().getValueSyntaxId());
					viewer.setInput(at, handler);
				} else
					viewer.setInput(null, null);
			}
		});
		table.addActionHandler(new RefreshActionHandler());
		table.addActionHandler(new AddActionHandler());
		table.addActionHandler(new EditActionHandler());
		table.addActionHandler(new DeleteActionHandler());
		hl.setSizeFull();
		hl.setMargin(true);
		hl.setSpacing(true);
		setContent(hl);
		main = hl;
		refresh();
	}
	
	public void refresh()
	{
		try
		{
			List<AttributeType> types = attrManagement.getAttributeTypes();
			table.setInput(types);
			setContent(main);
			bus.fireEvent(new AttributeTypesUpdatedEvent(types));
		} catch (Exception e)
		{
			ErrorComponent error = new ErrorComponent(e);
			setContent(error);
		}
		
	}
	
	private boolean updateType(AttributeType type)
	{
		try
		{
			attrManagement.updateAttributeType(type);
			refresh();
			return true;
		} catch (Exception e)
		{
			ErrorPopup.showError(msg.getMessage("AttributeTypes.errorUpdate"), e);
			return false;
		}
	}

	private boolean addType(AttributeType type)
	{
		try
		{
			attrManagement.addAttributeType(type);
			refresh();
			return true;
		} catch (Exception e)
		{
			ErrorPopup.showError(msg.getMessage("AttributeTypes.errorAdd"), e);
			return false;
		}
	}

	private boolean removeType(String name, boolean withInstances)
	{
		try
		{
			attrManagement.removeAttributeType(name, withInstances);
			refresh();
			return true;
		} catch (Exception e)
		{
			ErrorPopup.showError(msg.getMessage("AttributeTypes.errorRemove"), e);
			return false;
		}
	}
	
	private class RefreshActionHandler extends SingleActionHandler
	{
		public RefreshActionHandler()
		{
			super(msg.getMessage("AttributeTypes.refreshAction"), Images.refresh.getResource());
			setNeedsTarget(false);
		}

		@Override
		public void handleAction(Object sender, final Object target)
		{
			refresh();
		}
	}

	private class AddActionHandler extends SingleActionHandler
	{
		public AddActionHandler()
		{
			super(msg.getMessage("AttributeTypes.addAction"), Images.add.getResource());
			setNeedsTarget(false);
		}

		@Override
		public void handleAction(Object sender, final Object target)
		{
			AttributeTypeEditor editor = new AttributeTypeEditor(msg, attrHandlerRegistry);
			AttributeTypeEditDialog dialog = new AttributeTypeEditDialog(msg, 
					msg.getMessage("AttributeTypes.addAction"), new Callback()
					{
						@Override
						public boolean newAttribute(AttributeType newAttributeType)
						{
							return addType(newAttributeType);
						}
					}, editor);
			dialog.show();
		}
	}
	
	private class EditActionHandler extends SingleActionHandler
	{
		public EditActionHandler()
		{
			super(msg.getMessage("AttributeTypes.editAction"), Images.edit.getResource());
		}

		@Override
		public Action[] getActions(Object target, Object sender)
		{
			if (target == null || !(target instanceof AttributeTypeItem))
				return EMPTY;
			AttributeTypeItem item = (AttributeTypeItem)target;
			final AttributeType at = item.getAttributeType();
			if (at.isTypeImmutable())
				return EMPTY;
			return super.getActions(target, sender);
		}

		@Override
		public void handleAction(Object sender, final Object target)
		{
			AttributeTypeItem item = (AttributeTypeItem)target;
			AttributeType at = item.getAttributeType();
			AttributeTypeEditor editor = new AttributeTypeEditor(msg, attrHandlerRegistry, at);
			AttributeTypeEditDialog dialog = new AttributeTypeEditDialog(msg, 
					msg.getMessage("AttributeTypes.editAction"), new Callback()
					{
						@Override
						public boolean newAttribute(AttributeType newAttributeType)
						{
							return updateType(newAttributeType);
						}
					}, editor);
			dialog.show();
		}
	}
	
	private class DeleteActionHandler extends SingleActionHandler
	{
		public DeleteActionHandler()
		{
			super(msg.getMessage("AttributeTypes.deleteAction"), 
					Images.delete.getResource());
		}
		
		@Override
		public Action[] getActions(Object target, Object sender)
		{
			if (target == null || !(target instanceof AttributeTypeItem))
				return EMPTY;
			AttributeTypeItem item = (AttributeTypeItem)target;
			final AttributeType at = item.getAttributeType();
			if (at.isTypeImmutable())
				return EMPTY;
			return super.getActions(target, sender);
		}
		
		@Override
		public void handleAction(Object sender, Object target)
		{
			AttributeTypeItem item = (AttributeTypeItem)target;
			final AttributeType at = item.getAttributeType();
			new ConfirmWithOptionDialog(msg, msg.getMessage("AttributeTypes.confirmDelete", at.getName()),
					msg.getMessage("AttributeTypes.withInstances"),
					new ConfirmWithOptionDialog.Callback()
			{
				@Override
				public void onConfirm(boolean withInstances)
				{
					removeType(at.getName(), withInstances);
				}
			}).show();
		}
	}
}