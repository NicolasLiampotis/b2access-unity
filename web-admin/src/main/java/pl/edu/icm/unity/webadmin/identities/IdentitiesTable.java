/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.identities;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.exceptions.AuthorizationException;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.home.iddetails.EntityDetailsDialog;
import pl.edu.icm.unity.home.iddetails.EntityDetailsPanel;
import pl.edu.icm.unity.server.api.AttributesManagement;
import pl.edu.icm.unity.server.api.AuthenticationManagement;
import pl.edu.icm.unity.server.api.GroupsManagement;
import pl.edu.icm.unity.server.api.IdentitiesManagement;
import pl.edu.icm.unity.server.api.PreferencesManagement;
import pl.edu.icm.unity.server.api.internal.AttributesInternalProcessing;
import pl.edu.icm.unity.server.api.internal.LoginSession;
import pl.edu.icm.unity.server.authn.InvocationContext;
import pl.edu.icm.unity.server.utils.Log;
import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.stdext.utils.EntityNameMetadataProvider;
import pl.edu.icm.unity.types.EntityInformation;
import pl.edu.icm.unity.types.EntityState;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.AttributeExt;
import pl.edu.icm.unity.types.basic.AttributeType;
import pl.edu.icm.unity.types.basic.Entity;
import pl.edu.icm.unity.types.basic.EntityParam;
import pl.edu.icm.unity.types.basic.GroupMembership;
import pl.edu.icm.unity.types.basic.Identity;
import pl.edu.icm.unity.webadmin.groupbrowser.GroupChangedEvent;
import pl.edu.icm.unity.webadmin.identities.CredentialRequirementDialog.Callback;
import pl.edu.icm.unity.webadmin.utils.MessageUtils;
import pl.edu.icm.unity.webui.WebSession;
import pl.edu.icm.unity.webui.bus.EventsBus;
import pl.edu.icm.unity.webui.common.ConfirmDialog;
import pl.edu.icm.unity.webui.common.EntityWithLabel;
import pl.edu.icm.unity.webui.common.NotificationPopup;
import pl.edu.icm.unity.webui.common.Images;
import pl.edu.icm.unity.webui.common.SingleActionHandler;
import pl.edu.icm.unity.webui.common.Styles;
import pl.edu.icm.unity.webui.common.attributes.AttributeHandlerRegistry;
import pl.edu.icm.unity.webui.common.credentials.CredentialEditorRegistry;
import pl.edu.icm.unity.webui.common.credentials.CredentialsChangeDialog;
import pl.edu.icm.unity.webui.common.identities.IdentityEditorRegistry;

import com.vaadin.data.Container;
import com.vaadin.data.Item;
import com.vaadin.data.Property;
import com.vaadin.event.Action;
import com.vaadin.ui.TreeTable;

/**
 * Displays a tree table with identities. Can present contents in two modes: 
 *  - flat, where each identity is a fully separate table row
 *  - grouped by entity, where each entity has all its entities as children
 * @author K. Benedyczak
 */
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class IdentitiesTable extends TreeTable
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_WEB, IdentitiesTable.class);
	
	enum BaseColumnId {entity, type, identity, status, local, dynamic, credReq, remoteIdP, profile, target, realm,
		scheduledOperation};
	public static final String ATTR_COL_PREFIX = "a::";
	public static final String ATTR_ROOT_COL_PREFIX = ATTR_COL_PREFIX + "root::";
	public static final String ATTR_CURRENT_COL_PREFIX = ATTR_COL_PREFIX + "current::";
	private IdentitiesManagement identitiesMan;
	private GroupsManagement groupsMan;
	private UnityMessageSource msg;
	private AuthenticationManagement authnMan;
	private AttributesManagement attrMan;
	private PreferencesManagement preferencesMan;
	private AttributesInternalProcessing attrProcessor;
	private IdentityEditorRegistry identityEditorReg;
	private AttributeHandlerRegistry attrHandlerRegistry;
	private CredentialEditorRegistry credEditorsRegistry;
	private EventsBus bus;
	private String group;
	private Map<Long, IdentitiesAndAttributes> data = new HashMap<>();
	private boolean groupByEntity;
	private boolean showTargeted;
	private Entity selected;
	private List<Filter> containerFilters;
	private String entityNameAttribute = null;
	private List<SingleActionHandler> actionHandlers;
	
	@Autowired
	public IdentitiesTable(IdentitiesManagement identitiesMan, GroupsManagement groupsMan, 
			AuthenticationManagement authnMan, AttributesManagement attrMan,PreferencesManagement preferencesMan,
			AttributesInternalProcessing attrProcessor,
			IdentityEditorRegistry identityEditorReg, CredentialEditorRegistry credEditorsRegistry,
			AttributeHandlerRegistry attrHandlerReg, UnityMessageSource msg)
	{
		this.preferencesMan = preferencesMan;
		this.identitiesMan = identitiesMan;
		this.attrProcessor = attrProcessor;
		this.groupsMan = groupsMan;
		this.identityEditorReg = identityEditorReg;
		this.authnMan = authnMan;
		this.msg = msg;
		this.attrHandlerRegistry = attrHandlerReg;
		this.attrMan = attrMan;
		this.bus = WebSession.getCurrent().getEventBus();
		this.containerFilters = new ArrayList<Container.Filter>();
		this.credEditorsRegistry = credEditorsRegistry;
		this.actionHandlers = new ArrayList<>();
		
		addContainerProperty(BaseColumnId.entity.toString(), String.class, null);
		addContainerProperty(BaseColumnId.type.toString(), String.class, "");
		addContainerProperty(BaseColumnId.identity.toString(), String.class, "");
		addContainerProperty(BaseColumnId.status.toString(), String.class, "");
		addContainerProperty(BaseColumnId.local.toString(), String.class, "");
		addContainerProperty(BaseColumnId.dynamic.toString(), String.class, "");
		addContainerProperty(BaseColumnId.credReq.toString(), String.class, "");
		addContainerProperty(BaseColumnId.target.toString(), String.class, "");
		addContainerProperty(BaseColumnId.realm.toString(), String.class, "");
		addContainerProperty(BaseColumnId.remoteIdP.toString(), String.class, "");
		addContainerProperty(BaseColumnId.profile.toString(), String.class, "");
		addContainerProperty(BaseColumnId.scheduledOperation.toString(), String.class, "");

		setColumnHeader(BaseColumnId.entity.toString(), msg.getMessage("Identities.entity"));
		setColumnHeader(BaseColumnId.type.toString(), msg.getMessage("Identities.type"));
		setColumnHeader(BaseColumnId.identity.toString(), msg.getMessage("Identities.identity"));
		setColumnHeader(BaseColumnId.status.toString(), msg.getMessage("Identities.status"));
		setColumnHeader(BaseColumnId.local.toString(), msg.getMessage("Identities.local"));
		setColumnHeader(BaseColumnId.dynamic.toString(), msg.getMessage("Identities.dynamic"));
		setColumnHeader(BaseColumnId.credReq.toString(), msg.getMessage("Identities.credReq"));
		setColumnHeader(BaseColumnId.target.toString(), msg.getMessage("Identities.target"));
		setColumnHeader(BaseColumnId.realm.toString(), msg.getMessage("Identities.realm"));
		setColumnHeader(BaseColumnId.remoteIdP.toString(), msg.getMessage("Identities.remoteIdP"));
		setColumnHeader(BaseColumnId.profile.toString(), msg.getMessage("Identities.profile"));
		setColumnHeader(BaseColumnId.scheduledOperation.toString(), 
				msg.getMessage("Identities.scheduledOperation"));
		
		setSelectable(true);
		setMultiSelect(true);	
		setColumnReorderingAllowed(true);
		setColumnCollapsingAllowed(true);
		setColumnCollapsible(BaseColumnId.entity.toString(), false);
		setColumnCollapsed(BaseColumnId.local.toString(), true);
		setColumnCollapsed(BaseColumnId.dynamic.toString(), true);
		setColumnCollapsed(BaseColumnId.credReq.toString(), true);
		setColumnCollapsed(BaseColumnId.target.toString(), true);
		setColumnCollapsed(BaseColumnId.realm.toString(), true);
		setColumnCollapsed(BaseColumnId.remoteIdP.toString(), true);
		setColumnCollapsed(BaseColumnId.profile.toString(), true);
		setColumnCollapsed(BaseColumnId.scheduledOperation.toString(), true);

		setColumnWidth(BaseColumnId.entity.toString(), 200);
		setColumnWidth(BaseColumnId.type.toString(), 100);
		setColumnWidth(BaseColumnId.status.toString(), 100);
		setColumnWidth(BaseColumnId.local.toString(), 100);
		setColumnWidth(BaseColumnId.dynamic.toString(), 100);
		setColumnWidth(BaseColumnId.credReq.toString(), 180);
		setColumnWidth(BaseColumnId.target.toString(), 180);
		setColumnWidth(BaseColumnId.realm.toString(), 100);
		setColumnWidth(BaseColumnId.remoteIdP.toString(), 200);
		setColumnWidth(BaseColumnId.profile.toString(), 100);
		setColumnWidth(BaseColumnId.scheduledOperation.toString(), 200);

		loadPreferences();

		addActionHandler(new RefreshHandler());
		addActionHandler(new ShowEntityDetailsHandler());
		addActionHandler(new RemoveFromGroupHandler());
		addActionHandler(new AddEntityActionHandler());
		addActionHandler(new AddIdentityActionHandler());
		addActionHandler(new DeleteEntityHandler());
		addActionHandler(new DeleteIdentityHandler());
		addActionHandler(new ChangeEntityStatusHandler());
		addActionHandler(new ChangeCredentialHandler());
		addActionHandler(new ChangeCredentialRequirementHandler());
		addActionHandler(new EntityAttributesClassesHandler());
		addActionHandler(new MergeEntitiesHandler());
		setDragMode(TableDragMode.ROW);

		setImmediate(true);
		setSizeFull();

		addValueChangeListener(new Property.ValueChangeListener()
		{
			@Override
			public void valueChange(com.vaadin.data.Property.ValueChangeEvent event)
			{
				Collection<?> nodes = (Collection<?>) getValue();
				Object selected = null;
				if (nodes != null && nodes.size() == 1)
				{
					selected = nodes.iterator().next();
				}
				if (selected == null)
				{
					IdentitiesTable.this.selected = null;
					bus.fireEvent(new EntityChangedEvent(null, group));
				} else if (selected instanceof EntityWithLabel)
				{
					if (selected.equals(IdentitiesTable.this.selected))
						return;
					IdentitiesTable.this.selected = ((EntityWithLabel)selected).getEntity();
					bus.fireEvent(new EntityChangedEvent((EntityWithLabel)selected, group));
				} else if (selected instanceof IdentityWithEntity)
				{
					IdentityWithEntity identity = (IdentityWithEntity) selected;
					if (identity.getEntityWithLabel().getEntity().equals(IdentitiesTable.this.selected))
						return;
					IdentitiesTable.this.selected = identity.getEntityWithLabel().getEntity();
					bus.fireEvent(new EntityChangedEvent(identity.getEntityWithLabel(), group));
				}
			}
		});

		addStyleName(Styles.vTableNoHorizontalLines.toString());
		addStyleName(Styles.vSmall.toString());
		
//		addColumnResizeListener(new ColumnResizeListener()
//		{
//			@Override
//			public void columnResize(ColumnResizeEvent event)
//			{
//				savePreferences();
//			}
//		});
//
//		addColumnReorderListener(new ColumnReorderListener()
//		{
//			@Override
//			public void columnReorder(ColumnReorderEvent event)
//			{
//				savePreferences();
//			}
//		});
		//For future: addColumnCollapseListener, expected for Vaadin 7.2
	}

	public void savePreferences()
	{
		Collection<?> props = getContainerPropertyIds();
		IdentitiesTablePreferences preferences = new IdentitiesTablePreferences();
		Object[] columns = getVisibleColumns(); //order of the columns

		for (Object prop : props)
		{
			if (!(prop instanceof String))
				continue;
			String property = (String) prop;
			IdentitiesTablePreferences.ColumnSettings settings = 
					new IdentitiesTablePreferences.ColumnSettings();
			settings.setCollapsed(isColumnCollapsed(property));

			settings.setWidth(getColumnWidth(property));

			for (int i = 0; i < columns.length; i++)
			{
				String c = (String) columns[i];
				if (c.equals(property))
				{
					settings.setOrder(i);
				}
			}

			preferences.addColumneSettings(property, settings);
		}

		preferences.setGroupByEntitiesSetting(groupByEntity);
		preferences.setShowTargetedSetting(showTargeted);
		try
		{
			IdentitiesTablePreferences.savePreferences(preferencesMan, preferences);
		} catch (EngineException e)
		{
			NotificationPopup.showError(msg, msg.getMessage("error"),
					msg.getMessage("Identities.cannotSavePrefernces"));
			return;

		}

	}

	private void loadPreferences()
	{

		IdentitiesTablePreferences preferences = null;
		try
		{
			preferences = IdentitiesTablePreferences.getPreferences(preferencesMan);
		} catch (EngineException e)
		{
			log.debug("Can not load preferences for identities table", e);
			return;
		}
		groupByEntity = preferences.getGroupByEntitiesSetting();
		showTargeted = preferences.getShowTargetedSetting();
		
		Set<String> props = new HashSet<String>();

		for (Object prop : getContainerPropertyIds())
		{
			if (!(prop instanceof String))
				continue;
			String property = (String) prop;
			props.add(property);
		}

		if (preferences != null && preferences.getColumnSettings().size() > 0)
		{
			Object[] scol = new String[preferences.getColumnSettings().size()];

			for (Map.Entry<String, IdentitiesTablePreferences.ColumnSettings> entry : 
				preferences.getColumnSettings().entrySet())
			{
				if (!props.contains(entry.getKey().toString()))
				{
					if (entry.getKey().startsWith(ATTR_ROOT_COL_PREFIX))
						addAttributeColumn(entry.getKey().substring(
								ATTR_ROOT_COL_PREFIX.length()), "/");
					if (entry.getKey().startsWith(ATTR_CURRENT_COL_PREFIX))
						addAttributeColumn(entry.getKey().substring(
								ATTR_CURRENT_COL_PREFIX.length()), null);

					setColumnCollapsed(entry.getKey(), entry.getValue().isCollapsed());
					setColumnWidth(entry.getKey(), entry.getValue().getWidth());

				} else
				{
					if (!entry.getKey().equals(BaseColumnId.entity.toString()))
					{
						setColumnCollapsed(entry.getKey(), entry.getValue()
								.isCollapsed());
					}
					setColumnWidth(entry.getKey(), entry.getValue().getWidth());

				}

				scol[entry.getValue().getOrder()] = entry.getKey();
			}
			
			//get all which are not in prefs and add them at the end. Important for prefs from older version.
			HashSet<String> missing = new HashSet<String>(props);
			missing.removeAll(preferences.getColumnSettings().keySet());
			Object[] scolComplete = new Object[scol.length + missing.size()];
			int i=0;
			for (; i<scol.length; i++)
				scolComplete[i] = scol[i];
			for (String miss: missing)
				scolComplete[i++] = miss;
			
			setVisibleColumns(scolComplete);
		}

	}

	@Override
	public void addActionHandler(Action.Handler actionHandler) {
		super.addActionHandler(actionHandler);
		if (actionHandler instanceof SingleActionHandler)
			actionHandlers.add((SingleActionHandler) actionHandler);
	}

	public List<SingleActionHandler> getActionHandlers()
	{
		return actionHandlers;
	}

	public void setMode(boolean groupByEntity)
	{
		this.groupByEntity = groupByEntity;
		updateContents();
	}
	
	public void setShowTargeted(boolean showTargeted) throws EngineException
	{
		this.showTargeted = showTargeted;
		ArrayList<Long> entities = new ArrayList<>();
		entities.addAll(data.keySet());
		setInput(group, entities);
		updateContents();
	}	

	private void refresh()
	{
		bus.fireEvent(new GroupChangedEvent(group));
	}

	public String getGroup()
	{
		return group;
	}

	public void setInput(String group, List<Long> entities) throws EngineException
	{
		this.group = group;
		AttributeType nameAt = attrProcessor.getAttributeTypeWithSingeltonMetadata(
				EntityNameMetadataProvider.NAME);
		this.entityNameAttribute = nameAt == null ? null : nameAt.getName();
		data.clear();
		for (Long entity : entities)
		{
			try
			{
				resolveEntity(entity);
			} catch (AuthorizationException e)
			{
				log.debug("Entity " + entity + " information can not be loaded, "
						+ "won't be in the identities table", e);
			}
		}
		updateContents();
	}

	/**
	 * Adds a new attribute column.
	 * 
	 * @param attribute
	 * @param group group from where the attribute's value should be displayed. If it is null then the current 
	 * group is used. Otherwise root group is assumed (in future other 'fixed' groups might be supported, 
	 * but it isn't implemented yet)
	 */
	public void addAttributeColumn(String attribute, String group)
	{
		String key = (group == null) ? ATTR_CURRENT_COL_PREFIX+attribute : ATTR_ROOT_COL_PREFIX+attribute;
		addContainerProperty(key, String.class, "");
		setColumnHeader(key, attribute + (group == null ? "@" + this.group : "@/"));
		refresh();
		// savePreferences();
	}

	public void removeAttributeColumn(String group, String... attributes)
	{
		for (String attribute: attributes)
		{
			if (group.equals("/"))
				removeContainerProperty(ATTR_ROOT_COL_PREFIX + attribute);
			if (group.equals(this.group))
				removeContainerProperty(ATTR_CURRENT_COL_PREFIX + attribute);
		}
		refresh();
		// savePreferences();
	}

	public Set<String> getAttributeColumns(boolean root)
	{
		Collection<?> props = getContainerPropertyIds();
		Set<String> ret = new HashSet<String>();
		for (Object prop: props)
		{
			if (!(prop instanceof String))
				continue;
			String property = (String) prop;
			if (root)
			{
				if (property.startsWith(ATTR_ROOT_COL_PREFIX))
					ret.add(property.substring(ATTR_ROOT_COL_PREFIX.length()));
			} else
			{
				if (property.startsWith(ATTR_CURRENT_COL_PREFIX))
					ret.add(property.substring(ATTR_CURRENT_COL_PREFIX.length()));
			}
		}
		return ret;
	}

	private void updateAttributeColumnHeaders()
	{
		Collection<?> props = getContainerPropertyIds();
		for (Object prop : props)
		{
			if (!(prop instanceof String))
				continue;
			String property = (String) prop;
			if (property.startsWith(ATTR_CURRENT_COL_PREFIX))
			{
				String attrName = property.substring(ATTR_CURRENT_COL_PREFIX.length());
				setColumnHeader(property, attrName + "@" + this.group);
			}
		}
	}

	public void addFilter(Filter filter)
	{
		Container.Filterable filterable = (Filterable) getContainerDataSource();
		filterable.addContainerFilter(filter);
		containerFilters.add(filter);
	}

	public void removeFilter(Filter filter)
	{
		Container.Filterable filterable = (Filterable) getContainerDataSource();
		filterable.removeContainerFilter(filter);
		containerFilters.remove(filter);
		refresh();
	}

	private void updateContents()
	{
		updateAttributeColumnHeaders();
		Object selected = getValue();
		removeAllItems();
		if (groupByEntity)
			setGroupedContents(selected);
		else
			setFlatContents(selected);
	}

	/*
	 * We use a hack here: filters are temporarly removed and readded after all data is set. 
	 * This is because Vaadin (tested at 7.0.4) seems to ignore parent elements when not matching filter
	 * during addition, but properly shows them afterwards.
	 */
	private void setGroupedContents(Object selected)
	{
		Container.Filterable filterable = (Filterable) getContainerDataSource();
		filterable.removeAllContainerFilters();
		for (IdentitiesAndAttributes entry: data.values())
		{
			Entity entity = entry.getEntity();
			Object parentKey = addRow(null, entity, entry.getRootAttributes(), entry.getCurrentAttributes());
			if (selected != null && selected.equals(parentKey))
				setValue(parentKey);
			for (Identity id: entry.getIdentities())
			{
				Object key = addRow(id, entity, entry.getRootAttributes(), entry.getCurrentAttributes());
				setParent(key, parentKey);
				setChildrenAllowed(key, false);
				if (selected != null && selected.equals(key))
					setValue(key);
			}
		}
		for (Filter filter: containerFilters)
			filterable.addContainerFilter(filter);
	}

	private void setFlatContents(Object selected)
	{
		for (IdentitiesAndAttributes entry: data.values())
		{
			for (Identity id: entry.getIdentities())
			{
				Object itemId = addRow(id, entry.getEntity(), entry.getRootAttributes(), 
						entry.getCurrentAttributes());
				setChildrenAllowed(itemId, false);
				if (selected != null && selected.equals(itemId))
					setValue(itemId);
			}
		}
	}

	@SuppressWarnings("unchecked")
	private Object addRow(Identity id, Entity ent, Map<String, Attribute<?>> rootAttributes,
			Map<String, Attribute<?>> curAttributes)
	{
		String label = null;
		if (entityNameAttribute != null && rootAttributes.containsKey(entityNameAttribute))
			label = rootAttributes.get(entityNameAttribute).getValues().get(0).toString() + " ";
		EntityWithLabel entWithLabel = new EntityWithLabel(ent, label);
		Object itemId = id == null ? entWithLabel
				: new IdentityWithEntity(id, entWithLabel);
		Item newItem = addItem(itemId);
		
		newItem.getItemProperty(BaseColumnId.entity.toString()).setValue(
				entWithLabel.toString());
		newItem.getItemProperty(BaseColumnId.credReq.toString()).setValue(
				ent.getCredentialInfo().getCredentialRequirementId());
		newItem.getItemProperty(BaseColumnId.status.toString()).setValue(
				msg.getMessage("EntityState."+ent.getState().name()));
		EntityInformation entInfo = ent.getEntityInformation();
		String scheduledOpTxt = entInfo.getScheduledOperation() == null ? "" : 
			msg.getMessage("EntityScheduledOperationWithDateShort."+
				entInfo.getScheduledOperation().name(),
				entInfo.getScheduledOperationTime());
		newItem.getItemProperty(BaseColumnId.scheduledOperation.toString()).setValue(scheduledOpTxt);
		
		if (id != null)
		{
			newItem.getItemProperty(BaseColumnId.type.toString()).setValue(
					id.getTypeId());
			newItem.getItemProperty(BaseColumnId.identity.toString()).setValue(
					id.toPrettyStringNoPrefix());
			newItem.getItemProperty(BaseColumnId.local.toString()).setValue(
					new Boolean(id.isLocal()).toString());
			newItem.getItemProperty(BaseColumnId.dynamic.toString()).setValue(
					new Boolean(id.getType().getIdentityTypeProvider().isDynamic()).toString());
			String ridp = id.getRemoteIdp() == null ? "" : id.getRemoteIdp();
			newItem.getItemProperty(BaseColumnId.remoteIdP.toString()).setValue(ridp);
			String prof = id.getTranslationProfile() == null ? "" : id.getTranslationProfile();
			newItem.getItemProperty(BaseColumnId.profile.toString()).setValue(prof);
			newItem.getItemProperty(BaseColumnId.target.toString()).setValue(id.getTarget());
			newItem.getItemProperty(BaseColumnId.realm.toString()).setValue(id.getRealm());
		} else
		{
			newItem.getItemProperty(BaseColumnId.type.toString()).setValue("");
			newItem.getItemProperty(BaseColumnId.identity.toString()).setValue("");
			newItem.getItemProperty(BaseColumnId.local.toString()).setValue("");
			newItem.getItemProperty(BaseColumnId.dynamic.toString()).setValue("");
			newItem.getItemProperty(BaseColumnId.target.toString()).setValue("");
			newItem.getItemProperty(BaseColumnId.realm.toString()).setValue("");
			newItem.getItemProperty(BaseColumnId.remoteIdP.toString()).setValue("");
			newItem.getItemProperty(BaseColumnId.profile.toString()).setValue("");
		}

		Collection<?> propertyIds = newItem.getItemPropertyIds();
		for (Object propertyId: propertyIds)
		{
			if (!(propertyId instanceof String))
				continue;
			String propId = (String) propertyId;
			if (!propId.startsWith(ATTR_COL_PREFIX))
				continue;
			Attribute<?> attribute = getAttributeForColumnProperty(propId, rootAttributes, curAttributes);
			String val;
			if (attribute == null)
				val = msg.getMessage("Identities.attributeUndefined");
			else
				val = attrHandlerRegistry.getSimplifiedAttributeValuesRepresentation(attribute,
						AttributeHandlerRegistry.DEFAULT_MAX_LEN*3);
				
			newItem.getItemProperty(propId).setValue(val);
		}
		return itemId;
	}
	
	private Attribute<?> getAttributeForColumnProperty(String propId, Map<String, Attribute<?>> rootAttributes, 
			Map<String, Attribute<?>> curAttributes)
	{
		if (propId.startsWith(ATTR_CURRENT_COL_PREFIX))
		{
			String attributeName = propId.substring(ATTR_CURRENT_COL_PREFIX.length());
			return curAttributes.get(attributeName);
		} else
		{
			String attributeName = propId.substring(ATTR_ROOT_COL_PREFIX.length());
			return rootAttributes.get(attributeName);
		}
	}

	private void resolveEntity(long entity) throws EngineException
	{		
		Entity resolvedEntity = showTargeted ? identitiesMan
				.getEntityNoContext(new EntityParam(entity), this.group) : identitiesMan
				.getEntity(new EntityParam(entity), null, false, this.group);
		Collection<AttributeExt<?>> rawCurAttrs = attrMan.getAllAttributes(new EntityParam(entity), 
				true, this.group, null, true);
		Collection<AttributeExt<?>> rawRootAttrs = new ArrayList<AttributeExt<?>>();
		try
		{
			rawRootAttrs = attrMan.getAllAttributes(new EntityParam(entity), 
				true, "/", null, true);
		} catch (AuthorizationException e)
		{
			log.debug("can not resolve attributes in '/' for entity, " + entity + 
					" only group's attributes will be available: " + e.toString());
		}
		Map<String, Attribute<?>> rootAttrs = new HashMap<String, Attribute<?>>(rawRootAttrs.size());
		Map<String, Attribute<?>> curAttrs = new HashMap<String, Attribute<?>>(rawRootAttrs.size());
		for (Attribute<?> a: rawRootAttrs)
			rootAttrs.put(a.getName(), a);
		for (Attribute<?> a: rawCurAttrs)
			curAttrs.put(a.getName(), a);
		IdentitiesAndAttributes resolved = new IdentitiesAndAttributes(resolvedEntity, 
				resolvedEntity.getIdentities(),	rootAttrs, curAttrs);
		data.put(resolvedEntity.getId(), resolved);
	}

	private void removeEntity(long entityId)
	{
		LoginSession entity = InvocationContext.getCurrent().getLoginSession();

		if (entityId == entity.getEntityId())
		{
			NotificationPopup.showError(msg, msg.getMessage("error"), msg.getMessage("Identities.notRemovingLoggedUser"));
			return;
		}
		try
		{
			identitiesMan.removeEntity(new EntityParam(entityId));
			refresh();
		} catch (Exception e)
		{
			NotificationPopup.showError(msg, msg.getMessage("Identities.removeEntityError"), e);
		}
	}

	private void removeIdentity(Identity identity)
	{
		try
		{
			identitiesMan.removeIdentity(identity);
		} catch (Exception e)
		{
			NotificationPopup.showError(msg, msg.getMessage("Identities.removeIdentityError"), e);
		}
	}

	private void resetIdentity(Identity identity)
	{
		try
		{
			identitiesMan.resetIdentity(new EntityParam(identity.getEntityId()), identity.getTypeId(), 
					identity.getRealm(), identity.getTarget());
		} catch (Exception e)
		{
			NotificationPopup.showError(msg, msg.getMessage("Identities.removeIdentityError"), e);
		}
	}

	private boolean setEntityStatus(long entityId, EntityInformation newState)
	{
		try
		{
			
			EntityParam entity = new EntityParam(entityId);
			
			if (newState.getState() != EntityState.onlyLoginPermitted)
				identitiesMan.setEntityStatus(entity, newState.getState());
			identitiesMan.scheduleEntityChange(entity, newState.getScheduledOperationTime(), 
						newState.getScheduledOperation());
			refresh();
			return true;
		} catch (Exception e)
		{
			NotificationPopup.showError(msg, msg.getMessage("Identities.changeEntityStatusError"), e);
			return false;
		}
	}

	private void removeFromGroup(long entityId)
	{
		try
		{
			groupsMan.removeMember(group, new EntityParam(entityId));
			refresh();
		} catch (Exception e)
		{
			NotificationPopup.showError(msg, msg.getMessage("Identities.removeFromGroupError"), e);
		}
	}
	
	private class RemoveFromGroupHandler extends SingleActionHandler
	{
		public RemoveFromGroupHandler()
		{
			super(msg.getMessage("Identities.removeFromGroupAction"), 
					Images.delete.getResource());
			setMultiTarget(true);
		}

		@Override
		public void handleAction(Object sender, Object target)
		{			
			Collection<?> nodes = (Collection<?>) target;	
			final HashMap<Long, EntityWithLabel> toRemove = new HashMap<Long, EntityWithLabel>();
			for (Object node : nodes)
			{
				EntityWithLabel entity = node instanceof IdentityWithEntity ? ((IdentityWithEntity) node)
						.getEntityWithLabel() : ((EntityWithLabel) node);
				toRemove.put(entity.getEntity().getId(), entity);
			}
			String confirmText = MessageUtils.createConfirmFromStrings(msg, toRemove.values());
			new ConfirmDialog(msg, msg.getMessage("Identities.confirmRemoveFromGroup",
					confirmText, group), new ConfirmDialog.Callback()
			{
				@Override
				public void onConfirm()
				{
					for (EntityWithLabel en : toRemove.values())
					{
						removeFromGroup(en.getEntity().getId());
					}
				}
			}).show();

		}
	}

	private class AddEntityActionHandler extends SingleActionHandler
	{
		public AddEntityActionHandler()
		{
			super(msg.getMessage("Identities.addEntityAction"), Images.addEntity.getResource());
			setNeedsTarget(false);
		}

		@Override
		public void handleAction(Object sender, Object target)
		{
			new EntityCreationDialog(msg, group, identitiesMan, groupsMan, 
					authnMan, attrHandlerRegistry,
					attrMan, identityEditorReg, 
					new EntityCreationDialog.Callback()
					{
						@Override
						public void onCreated(Identity newIdentity)
						{
							bus.fireEvent(new GroupChangedEvent(group));
						}
					}).show();
		}
	}

	private EntityWithLabel getSingleSelect(Object target)
	{	
		EntityWithLabel entity = target instanceof IdentityWithEntity ? ((IdentityWithEntity) target)
				.getEntityWithLabel() : ((EntityWithLabel) target);
		return entity;
	}

	private class AddIdentityActionHandler extends SingleActionHandler
	{
		public AddIdentityActionHandler()
		{
			super(msg.getMessage("Identities.addIdentityAction"), Images.addIdentity
					.getResource());
		}

		@Override
		public void handleAction(Object sender, Object target)
		{
			final EntityWithLabel entity = target instanceof IdentityWithEntity ? 
					((IdentityWithEntity) target).getEntityWithLabel() : ((EntityWithLabel)target);
			new IdentityCreationDialog(msg, entity.getEntity().getId(), identitiesMan,  
					identityEditorReg, new IdentityCreationDialog.Callback()
					{
						@Override
						public void onCreated(Identity newIdentity)
						{
							bus.fireEvent(new GroupChangedEvent(group));
						}
					}).show();
		}
	}
	
	private class DeleteEntityHandler extends SingleActionHandler
	{
		public DeleteEntityHandler()
		{
			super(msg.getMessage("Identities.deleteEntityAction"), 
					Images.deleteEntity.getResource());
			setMultiTarget(true);
		}

		@Override
		public void handleAction(Object sender, Object target)
		{       
			Collection<?> nodes = (Collection<?>) target;
			final HashMap<Long, EntityWithLabel> toRemove = new HashMap<Long, EntityWithLabel>();
			for (Object node : nodes)
			{
				EntityWithLabel entity = node instanceof IdentityWithEntity ? 
						((IdentityWithEntity) node).getEntityWithLabel() : 
						((EntityWithLabel) node);
				toRemove.put(entity.getEntity().getId(), entity);
			}
			String confirmText = MessageUtils.createConfirmFromStrings(msg, toRemove.values());
			new ConfirmDialog(msg, msg.getMessage("Identities.confirmEntityDelete",
					confirmText), new ConfirmDialog.Callback()
			{
				@Override
				public void onConfirm()
				{
					for (EntityWithLabel entity : toRemove.values())
					{
						removeEntity(entity.getEntity().getId());
					}
				}
			}).show();

		}
	}

	private class DeleteIdentityHandler extends SingleActionHandler
	{
		public DeleteIdentityHandler()
		{
			super(msg.getMessage("Identities.deleteIdentityAction"),
					Images.deleteIdentity.getResource());
			setMultiTarget(true);
		}

		@Override
		public Action[] getActions(Object target, Object sender)
		{
			if (target == null)
				return EMPTY;
			
			if (!(target instanceof Collection<?>))
				target = Collections.singleton(target);
			
			Collection<?> targets = (Collection<?>) target;
			for (Object ta : targets)
			{
				if (ta != null && !(ta instanceof IdentityWithEntity))
					return EMPTY;
			}
			
			return super.getActions(target, sender);
		}

		@Override
		public void handleAction(Object sender, Object target)
		{		
			Collection<?> nodes = (Collection<?>) target;
			final List<IdentityWithEntity> filteredNodes = new ArrayList<>();
			
			StringBuilder confirmText = new StringBuilder();
			int count = 0;
			for (Object o : nodes)
			{
				IdentityWithEntity node = (IdentityWithEntity) o;
				if (count < 4)
					confirmText.append(", " + node.identity);
				count++;
				filteredNodes.add(node);
			}
			if (count > 3)
				confirmText.append(msg.getMessage("MessageUtils.andMore", count-3));
				
			String confirmTextF = confirmText.substring(2);
			new ConfirmDialog(msg, msg.getMessage("Identities.confirmIdentityDelete",
					confirmTextF), new ConfirmDialog.Callback()
			{
				@Override
				public void onConfirm()
				{
					for (IdentityWithEntity o : filteredNodes)
					{
						if (o.getIdentity().getType().getIdentityTypeProvider().isRemovable())
							removeIdentity(o.identity);
						else
							resetIdentity(o.identity);
					}
					refresh();
				}
			}).show();
		}
	}
	
	private class ChangeEntityStatusHandler extends SingleActionHandler
	{
		public ChangeEntityStatusHandler()
		{
			super(msg.getMessage("Identities.changeEntityStatusAction"),
					Images.editUser.getResource());
		}

		@Override
		public void handleAction(Object sender, Object target)
		{
			final EntityWithLabel entity = getSingleSelect(target);
			new ChangeEntityStateDialog(msg, entity, 
					new ChangeEntityStateDialog.Callback()
					{
						@Override
						public boolean onChanged(EntityInformation newState)
						{
							return setEntityStatus(entity.getEntity()
									.getId(), newState);
						}
					}).show();
		}	
	}
	
	private class ChangeCredentialRequirementHandler extends SingleActionHandler
	{
		public ChangeCredentialRequirementHandler()
		{
			super(msg.getMessage("Identities.changeCredentialRequirementAction"),
					Images.key.getResource());
		}

		@Override
		public void handleAction(Object sender, Object target)
		{
			final EntityWithLabel entity = getSingleSelect(target);
			IdentitiesAndAttributes info = data.get(entity.getEntity().getId());
			String currentCredId = info.getEntity().getCredentialInfo()
					.getCredentialRequirementId();
			new CredentialRequirementDialog(msg, entity, currentCredId, identitiesMan,
					authnMan, new Callback()
					{
						@Override
						public void onChanged()
						{
							refresh();
						}
					}).show();
		}
	}

	private class ChangeCredentialHandler extends SingleActionHandler
	{
		public ChangeCredentialHandler()
		{
			super(msg.getMessage("Identities.changeCredentialAction"), Images.key
					.getResource());
		}

		@Override
		public void handleAction(Object sender, Object target)
		{
			final EntityWithLabel entity = getSingleSelect(target);
			new CredentialsChangeDialog(msg, entity.getEntity().getId(), authnMan, identitiesMan,
					credEditorsRegistry, false, new CredentialsChangeDialog.Callback()
					{
						@Override
						public void onClose(boolean changed)
						{
							if (changed)
								refresh();
						}
					}).show();
		}
	}

	private class RefreshHandler extends SingleActionHandler
	{
		public RefreshHandler()
		{
			super(msg.getMessage("Identities.refresh"), 
					Images.refresh.getResource());
			setNeedsTarget(false);
		}

		@Override
		public void handleAction(Object sender, Object target)
		{
			refresh();
		}
	}

	private void showEntityDetails(EntityWithLabel entity)
	{
		final EntityDetailsPanel identityDetailsPanel = new EntityDetailsPanel(msg);
		Collection<GroupMembership> groups;
		try
		{
			groups = identitiesMan.getGroups(new EntityParam(entity.getEntity().getId())).values();
		} catch (EngineException e)
		{
			NotificationPopup.showError(msg, msg.getMessage("error"), e);
			return;
		}
		identityDetailsPanel.setInput(entity, groups);
		new EntityDetailsDialog(msg, identityDetailsPanel).show();
	}

	private class ShowEntityDetailsHandler extends SingleActionHandler
	{
		public ShowEntityDetailsHandler()
		{
			super(msg.getMessage("Identities.showEntityDetails"), 
					Images.userMagnifier.getResource());
		}

		@Override
		public void handleAction(Object sender, Object target)
		{
			final EntityWithLabel entity = getSingleSelect(target);
			showEntityDetails(entity);
		}
	}

	private class EntityAttributesClassesHandler extends SingleActionHandler
	{
		public EntityAttributesClassesHandler()
		{
			super(msg.getMessage("Identities.editEntityACs"), 
					Images.attributes.getResource());
		}

		@Override
		public void handleAction(Object sender, Object target)
		{

			final EntityWithLabel entity = getSingleSelect(target);
			EntityAttributesClassesDialog dialog = new EntityAttributesClassesDialog(
					msg, group, entity, attrMan, groupsMan,
					new EntityAttributesClassesDialog.Callback()
					{
						@Override
						public void onChange()
						{
							refresh();
						}
					});
			dialog.show();
		}
	}

	private class MergeEntitiesHandler extends SingleActionHandler
	{
		public MergeEntitiesHandler()
		{
			super(msg.getMessage("Identities.mergeEntitiesAction"),
					Images.transfer.getResource());
			setMultiTarget(true);
		}

		@Override
		public Action[] getActions(Object target, Object sender)
		{
			if (target == null || !(target instanceof Collection<?>))
				return EMPTY;
			
			
			Collection<?> targets = (Collection<?>) target;
			if (targets.size() != 2)
				return EMPTY;

			Iterator<?> iterator = targets.iterator();
			EntityWithLabel e1 = getEntity(iterator.next());
			EntityWithLabel e2 = getEntity(iterator.next());
			if (e1.getEntity().getId().longValue() == e2.getEntity().getId().longValue())
				return EMPTY;
			
			return super.getActions(target, sender);
		}

		private EntityWithLabel getEntity(Object selection)
		{
			if (selection instanceof IdentityWithEntity)
				return ((IdentityWithEntity) selection).getEntityWithLabel();
			else
				return (EntityWithLabel) selection;
		}
		
		@Override
		public void handleAction(Object sender, Object target)
		{		
			Collection<?> targets = (Collection<?>) target;
			Iterator<?> iterator = targets.iterator();
			EntityWithLabel e1 = getEntity(iterator.next());
			EntityWithLabel e2 = getEntity(iterator.next());
			EntityMergeDialog dialog = new EntityMergeDialog(msg, e1, e2, group, identitiesMan);
			dialog.show();
		}
	}

	
	public boolean isGroupByEntity()
	{
		return groupByEntity;
	}
	
	public boolean isShowTargeted()
	{
		return showTargeted;
	}

	/**
	 * Complete info about entity: its identities and relevant attributes.
	 * Used to populate table.
	 */
	private static class IdentitiesAndAttributes
	{
		private Entity entity;
		private Identity[] identities;
		private Map<String, Attribute<?>> rootAttributes;
		private Map<String, Attribute<?>> currentAttributes;

		public IdentitiesAndAttributes(Entity entity, Identity[] identities, 
				Map<String, Attribute<?>> rootAttributes, Map<String, Attribute<?>> currentAttributes)
		{
			this.identities = Arrays.copyOf(identities, identities.length);
			this.rootAttributes = rootAttributes;
			this.currentAttributes = currentAttributes;
			this.entity = entity;
		}
		
		public Identity[] getIdentities()
		{
			return identities;
		}

		public Map<String, Attribute<?>> getRootAttributes()
		{
			return rootAttributes;
		}

		public Map<String, Attribute<?>> getCurrentAttributes()
		{
			return currentAttributes;
		}

		public Entity getEntity()
		{
			return entity;
		}
	}

	/**
	 * Identity with its Entity. Used as item id for the rows with
	 * particular identities.
	 * 
	 * @author K. Benedyczak
	 */
	public static class IdentityWithEntity
	{
		private Identity identity;
		private EntityWithLabel entity;
		public IdentityWithEntity(Identity identity, EntityWithLabel entity)
		{
			super();
			this.identity = identity;
			this.entity = entity;
		}

		public Identity getIdentity()
		{
			return identity;
		}

		public EntityWithLabel getEntityWithLabel()
		{
			return entity;
		}

		@Override
		public int hashCode()
		{
			final int prime = 31;
			int result = 1;
			result = prime * result + ((identity == null) ? 0 : identity.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj)
		{
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			IdentityWithEntity other = (IdentityWithEntity) obj;
			if (identity == null)
			{
				if (other.identity != null)
					return false;
			} else if (!identity.equals(other.identity))
				return false;
			return true;
		}

	}
}
