/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.tprofile.dryrun;

import org.vaadin.teemu.wizards.Wizard;
import org.vaadin.teemu.wizards.event.WizardProgressListener;

import pl.edu.icm.unity.sandbox.SandboxAuthnResultEvent;
import pl.edu.icm.unity.server.utils.UnityMessageSource;

import com.vaadin.annotations.AutoGenerated;
import com.vaadin.ui.Button;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.VerticalLayout;

/**
 * Component that displays the dryrun - used in {@link DryRunDialog}.
 * 
 * @author Roman Krysinski
 */
public class DryRunComponent extends CustomComponent 
{

	/*- VaadinEditorProperties={"grid":"RegularGrid,20","showGrid":true,"snapToGrid":true,"snapToObject":true,"movingGuides":false,"snappingDistance":10} */

	@AutoGenerated
	private Wizard wizard;
	private DryRunStep dryrunStep;
	
	/**
	 * The constructor should first build the main layout, set the
	 * composition root and then do any custom initialization.
	 *
	 * The constructor will not be automatically regenerated by the
	 * visual editor.
	 * @param sandboxURL 
	 * @param updateCallback 
	 */
	public DryRunComponent(UnityMessageSource msg, String sandboxURL) 
	{
		setCompositionRoot(buildMainLayout());
		
		dryrunStep = new DryRunStep(msg, sandboxURL);
		wizard.addStep(new IntroStep(msg));
		wizard.addStep(dryrunStep);
	}

	public void addWizardListener(WizardProgressListener listener) 
	{
		wizard.addListener(listener);
	}
	
	public Button getNextButton()
	{
		return wizard.getNextButton();
	}

	public void handle(SandboxAuthnResultEvent event) 
	{
		dryrunStep.handle(event);
	}
	
	@AutoGenerated
	private VerticalLayout buildMainLayout() {
		// common part: create layout
		VerticalLayout mainLayout = new VerticalLayout();
		mainLayout.setSizeFull();
		mainLayout.setMargin(true);
		
		// wizard
		wizard = new Wizard();
		wizard.setSizeFull();
		mainLayout.addComponent(wizard);

		setSizeFull();

		return mainLayout;
	}
}
