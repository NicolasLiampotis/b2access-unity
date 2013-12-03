/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.ldap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.server.api.AttributesManagement;
import pl.edu.icm.unity.server.api.TranslationProfileManagement;
import pl.edu.icm.unity.server.authn.CredentialVerificator;
import pl.edu.icm.unity.server.authn.CredentialVerificatorFactory;

/**
 * Produces verificators of passwords using remote LDAP server.
 * 
 * @author K. Benedyczak
 */
@Component
public class LdapVerificatorFactory implements CredentialVerificatorFactory
{
	public static final String NAME = "ldap";
	
	private TranslationProfileManagement profileManagement;
	private AttributesManagement attrMan;

	@Autowired
	public LdapVerificatorFactory(@Qualifier("insecure") TranslationProfileManagement profileManagement, 
			@Qualifier("insecure") AttributesManagement attrMan)
	{
		this.profileManagement = profileManagement;
		this.attrMan = attrMan;
	}

	@Override
	public String getName()
	{
		return NAME;
	}

	@Override
	public String getDescription()
	{
		return "Verifies password using LDAPv3 protocol";
	}

	@Override
	public CredentialVerificator newInstance()
	{
		return new LdapVerificator(getName(), getDescription(), profileManagement, attrMan);
	}
}
