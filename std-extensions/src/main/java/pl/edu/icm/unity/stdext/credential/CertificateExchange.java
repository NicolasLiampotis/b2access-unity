/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.stdext.credential;

import java.security.cert.X509Certificate;

import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.server.authn.AuthenticatedEntity;
import pl.edu.icm.unity.server.authn.CredentialExchange;

/**
 * Exchange for checking if the presented certificate is in the DB. It is assumed that the
 * certificate was actually authenticated by the transport layer - the verificator only checks 
 * if the certificate is present in the database.
 * @author K. Benedyczak
 */
public interface CertificateExchange extends CredentialExchange
{
	public static final String ID = "certificate exchange";
	
	public AuthenticatedEntity checkCertificate(X509Certificate[] chain) throws EngineException;

}