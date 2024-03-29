/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.saml.ecp;

import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.jetty.servlet.ServletHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.saml.metadata.MultiMetadataServlet;
import pl.edu.icm.unity.saml.metadata.cfg.MetaDownloadManager;
import pl.edu.icm.unity.saml.metadata.cfg.RemoteMetaManager;
import pl.edu.icm.unity.server.api.IdentitiesManagement;
import pl.edu.icm.unity.server.api.PKIManagement;
import pl.edu.icm.unity.server.api.TranslationProfileManagement;
import pl.edu.icm.unity.server.api.internal.IdentityResolver;
import pl.edu.icm.unity.server.api.internal.NetworkServer;
import pl.edu.icm.unity.server.api.internal.SessionManagement;
import pl.edu.icm.unity.server.api.internal.SharedEndpointManagement;
import pl.edu.icm.unity.server.api.internal.TokensManagement;
import pl.edu.icm.unity.server.authn.remote.InputTranslationEngine;
import pl.edu.icm.unity.server.endpoint.EndpointFactory;
import pl.edu.icm.unity.server.endpoint.EndpointInstance;
import pl.edu.icm.unity.server.utils.ExecutorsService;
import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.server.utils.UnityServerConfiguration;
import pl.edu.icm.unity.types.endpoint.EndpointTypeDescription;
import eu.unicore.samly2.validators.ReplayAttackChecker;

/**
 * Factory of {@link ECPEndpoint}s.
 * @author K. Benedyczak
 */
@Component
public class ECPEndpointFactory implements EndpointFactory
{
	public static final String SERVLET_PATH = "/saml2-ecp";
	public static final String METADATA_SERVLET_PATH = "/metadata";
	public static final String NAME = "SAML-ECP";
	
	private EndpointTypeDescription description;
	private PKIManagement pkiManagement;
	private URL baseAddress;
	private ECPContextManagement samlContextManagement;
	private ReplayAttackChecker replayAttackChecker;
	private IdentityResolver identityResolver;
	private TranslationProfileManagement profileManagement;
	private InputTranslationEngine trEngine;
	private TokensManagement tokensMan;
	private IdentitiesManagement identitiesMan;
	private SessionManagement sessionMan;
	private String baseContext;
	
	private MultiMetadataServlet metadataServlet;
	private UnityServerConfiguration mainCfg;
	private ExecutorsService executorsService;
	private Map<String, RemoteMetaManager> remoteMetadataManagers;
	private MetaDownloadManager downloadManager;
	private UnityMessageSource msg;
	private NetworkServer server;
	
	@Autowired
	public ECPEndpointFactory(PKIManagement pkiManagement, NetworkServer jettyServer,
			ECPContextManagement samlContextManagement,
			ReplayAttackChecker replayAttackChecker, IdentityResolver identityResolver,
			@Qualifier("insecure")
			TranslationProfileManagement profileManagement,
			InputTranslationEngine trEngine, TokensManagement tokensMan,
			IdentitiesManagement identitiesMan, SessionManagement sessionMan,
			UnityServerConfiguration mainCfg, MetaDownloadManager downloadManager,
			ExecutorsService executorsService, SharedEndpointManagement sharedEndpointManagement,
			UnityMessageSource msg, NetworkServer server) 
					throws EngineException
	{
		this.pkiManagement = pkiManagement;
		this.msg = msg;
		this.server = server;
		this.baseAddress = jettyServer.getAdvertisedAddress();
		this.samlContextManagement = samlContextManagement;
		this.replayAttackChecker = replayAttackChecker;
		this.identityResolver = identityResolver;
		this.profileManagement = profileManagement;
		this.trEngine = trEngine;
		this.tokensMan = tokensMan;
		this.identitiesMan = identitiesMan;
		this.sessionMan = sessionMan;
		this.mainCfg = mainCfg;
		this.executorsService = executorsService;
		this.baseContext = sharedEndpointManagement.getBaseContextPath();
		Set<String> supportedAuthn = new HashSet<String>();
		Map<String,String> paths = new HashMap<String, String>();
		paths.put(SERVLET_PATH, "SAML 2 ECP authentication endpoint");
		paths.put(METADATA_SERVLET_PATH, "Metadata of the SAML ECP endpoint");
		description = new EndpointTypeDescription(NAME, 
				"SAML 2 ECP authentication endpoint", supportedAuthn, paths);
		
		metadataServlet = new MultiMetadataServlet(METADATA_SERVLET_PATH);
		sharedEndpointManagement.deployInternalEndpointServlet(METADATA_SERVLET_PATH, 
				new ServletHolder(metadataServlet), false);
		this.remoteMetadataManagers = Collections.synchronizedMap(new HashMap<String, RemoteMetaManager>());
		this.downloadManager = downloadManager;
	}
	
	@Override
	public EndpointTypeDescription getDescription()
	{
		return description;
	}

	@Override
	public EndpointInstance newInstance()
	{
		return new ECPEndpoint(server, SERVLET_PATH, pkiManagement, samlContextManagement, baseAddress,
				baseContext, replayAttackChecker, identityResolver, profileManagement, trEngine, 
				tokensMan, identitiesMan, sessionMan, remoteMetadataManagers, 
				mainCfg, downloadManager, executorsService, metadataServlet, msg);
	}

}
