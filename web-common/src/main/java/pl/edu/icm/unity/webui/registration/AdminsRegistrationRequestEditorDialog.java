/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.registration;

import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.types.registration.RegistrationRequest;
import pl.edu.icm.unity.webui.common.AbstractDialog;
import pl.edu.icm.unity.webui.common.FormValidationException;
import pl.edu.icm.unity.webui.common.NotificationPopup;

import com.vaadin.ui.AbstractOrderedLayout;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Component;
import com.vaadin.ui.VerticalLayout;

/**
 * Dialog allowing to fill a registration form. Intended to be used from the AdminUI to fill registration form by admin.
 * It takes an editor component as argument. Dialog uses 3 buttons: submit request, submit and accept, cancel.
 * The submit and accept button distinguishes this dialog from its simpler brother 
 * {@link RegistrationRequestEditorDialog}.
 * 
 * @author K. Benedyczak
 */
public class AdminsRegistrationRequestEditorDialog extends AbstractDialog
{
	private RegistrationRequestEditor editor;
	private Callback callback;
	private Button submitAndAccept;
	
	public AdminsRegistrationRequestEditorDialog(UnityMessageSource msg, String caption, 
			RegistrationRequestEditor editor, Callback callback)
	{
		super(msg, caption, msg.getMessage("RegistrationRequestEditorDialog.submitRequest"), 
				msg.getMessage("cancel"));
		submitAndAccept = new Button(msg.getMessage("RegistrationRequestEditorDialog.submitAndAccept"), this);
		this.editor = editor;
		this.callback = callback;
		setSizeMode(SizeMode.LARGE);
	}

	@Override
	protected AbstractOrderedLayout getButtonsBar()
	{
		AbstractOrderedLayout ret = super.getButtonsBar();
		ret.addComponent(submitAndAccept, 0);
		return ret;
	}
	
	@Override
	protected Component getContents()
	{
		VerticalLayout vl = new VerticalLayout();
		vl.addComponent(editor);
		vl.setComponentAlignment(editor, Alignment.TOP_CENTER);
		vl.setHeight(100, Unit.PERCENTAGE);
		return vl;
	}

	@Override
	protected void onCancel()
	{
		callback.cancelled();
		super.onCancel();
	}
	
	@Override
	protected void onConfirm()
	{
		onConfirm(false);
	}
	
	private void onSubmitAndAccept()
	{
		onConfirm(true);
	}
	
	private void onConfirm(boolean autoAccept)
	{
		try
		{
			RegistrationRequest request = editor.getRequest();
			if (callback.newRequest(request, autoAccept))
				close();
		} catch (FormValidationException e) 
		{
			NotificationPopup.showError(msg, msg.getMessage("Generic.formError"), e);
			return;
		}
	}
	
	public interface Callback
	{
		boolean newRequest(RegistrationRequest request, boolean autoAccept);
		void cancelled();
	}
	
	public void buttonClick(ClickEvent event) {
		if (event.getSource() == submitAndAccept)
			onSubmitAndAccept();
		super.buttonClick(event);
	}
}
