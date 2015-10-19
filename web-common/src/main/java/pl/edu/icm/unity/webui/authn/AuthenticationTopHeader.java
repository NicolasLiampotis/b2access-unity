/*
 * Copyright (c) 2015 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.authn;

import com.vaadin.server.ExternalResource;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Link;

import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.webui.common.TopHeaderLight;

/**
 * Top bar of the authentication screen. Includes language selection.
 * @author K. Benedyczak
 */
public class AuthenticationTopHeader extends TopHeaderLight
{

	public AuthenticationTopHeader(String title, LocaleChoiceComponent localeChoice, UnityMessageSource msg)
	{
		super(title, msg);
		
                Link[] links = new Link[] {   
                    new Link("What is B2ACCESS", new ExternalResource("https://b2access.eudat.eu/files/b2access-about.html")),
                    new Link("User Guide", new ExternalResource("https://b2access.eudat.eu/files/b2access-guide.html")),
                    new Link("FAQs", new ExternalResource("https://b2access.eudat.eu/files/b2access-faq.html")),
                    new Link("Contact", new ExternalResource("http://eudat.eu/support-request?Service=B2ACCESS"))
                };
                
                HorizontalLayout buttons = new HorizontalLayout();
                buttons.setId("top-header-buttons");
                for(Link l : links) {
                    buttons.addComponent(l);
                    l.setSizeUndefined();
                    l.setStyleName("top-header-button");
                }
                
                addComponent(buttons);
                setComponentAlignment(buttons, Alignment.TOP_CENTER);
		addComponent(localeChoice);
		setComponentAlignment(localeChoice, Alignment.MIDDLE_RIGHT);
                localeChoice.setStyleName("locale-choice");
	}

}
