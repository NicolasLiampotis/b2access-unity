/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.client;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.log4j.Logger;

import com.nimbusds.jwt.ReadOnlyJWTClaimsSet;
import com.nimbusds.oauth2.sdk.AuthorizationCode;
import com.nimbusds.oauth2.sdk.AuthorizationCodeGrant;
import com.nimbusds.oauth2.sdk.ParseException;
import com.nimbusds.oauth2.sdk.ResponseType;
import com.nimbusds.oauth2.sdk.Scope;
import com.nimbusds.oauth2.sdk.SerializeException;
import com.nimbusds.oauth2.sdk.TokenRequest;
import com.nimbusds.oauth2.sdk.auth.ClientAuthentication;
import com.nimbusds.oauth2.sdk.auth.ClientSecretPost;
import com.nimbusds.oauth2.sdk.auth.Secret;
import com.nimbusds.oauth2.sdk.http.HTTPRequest;
import com.nimbusds.oauth2.sdk.http.HTTPResponse;
import com.nimbusds.oauth2.sdk.id.ClientID;
import com.nimbusds.oauth2.sdk.id.State;
import com.nimbusds.oauth2.sdk.token.AccessToken;
import com.nimbusds.oauth2.sdk.token.BearerAccessToken;
import com.nimbusds.openid.connect.sdk.AuthenticationRequest;
import com.nimbusds.openid.connect.sdk.OIDCAccessTokenResponse;
import com.nimbusds.openid.connect.sdk.UserInfoErrorResponse;
import com.nimbusds.openid.connect.sdk.UserInfoRequest;
import com.nimbusds.openid.connect.sdk.UserInfoResponse;
import com.nimbusds.openid.connect.sdk.UserInfoSuccessResponse;
import com.nimbusds.openid.connect.sdk.op.OIDCProviderMetadata;

import eu.unicore.util.configuration.ConfigurationException;
import pl.edu.icm.unity.exceptions.InternalException;
import pl.edu.icm.unity.server.api.AttributesManagement;
import pl.edu.icm.unity.server.api.TranslationProfileManagement;
import pl.edu.icm.unity.server.authn.AuthenticationException;
import pl.edu.icm.unity.server.authn.AuthenticationResult;
import pl.edu.icm.unity.server.authn.remote.AbstractRemoteVerificator;
import pl.edu.icm.unity.server.authn.remote.RemoteAttribute;
import pl.edu.icm.unity.server.authn.remote.RemotelyAuthenticatedInput;
import pl.edu.icm.unity.server.utils.Log;


/**
 * Binding independent OAuth 2 logic. Creates authZ requests, validates response (OAuth authorization grant)
 * performs subsequent call to AS to get resource owner's (authenticated user) information.
 *   
 * @author K. Benedyczak
 */
public class OAuth2Verificator extends AbstractRemoteVerificator implements OAuthExchange
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_OAUTH, OAuth2Verificator.class);
	private OAuthClientProperties config;
	private String responseConsumerAddress;
	private OAuthContextsManagement contextManagement;
	private OpenIdProviderMetadataManager metadataManager;
	
	public OAuth2Verificator(String name, String description, OAuthContextsManagement contextManagement,
			TranslationProfileManagement profileManagement, AttributesManagement attrMan,
			URL baseAddress, String baseContext)
	{
		super(name, description, OAuthExchange.ID, profileManagement, attrMan);
		this.responseConsumerAddress = baseAddress + baseContext + ResponseConsumerServlet.PATH;
		this.contextManagement = contextManagement;
	}

	@Override
	public String getSerializedConfiguration() throws InternalException
	{
		StringWriter sbw = new StringWriter();
		try
		{
			config.getProperties().store(sbw, "");
		} catch (IOException e)
		{
			throw new InternalException("Can't serialize OAuth2 verificator configuration", e);
		}
		return sbw.toString();	
	}

	@Override
	public void setSerializedConfiguration(String source) throws InternalException
	{
		try
		{
			Properties properties = new Properties();
			properties.load(new StringReader(source));
			config = new OAuthClientProperties(properties);
			metadataManager = new OpenIdProviderMetadataManager();
			Set<String> keys = config.getStructuredListKeys(OAuthClientProperties.PROVIDERS);
			for (String key: keys)
			{
				if (config.getBooleanValue(key + OAuthClientProperties.OPENID_CONNECT))
				{
					metadataManager.addProvider(config.getValue(key + 
							OAuthClientProperties.OPENID_DISCOVERY));
				}
			}
		} catch(ConfigurationException e)
		{
			throw new InternalException("Invalid configuration of the OAuth2 verificator", e);
		} catch (IOException e)
		{
			throw new InternalException("Invalid configuration of the OAuth2 verificator(?)", e);
		}
	}

	@Override
	public OAuthClientProperties getSettings()
	{
		return config;
	}

	@Override
	public OAuthContext createRequest(String providerKey) throws URISyntaxException, SerializeException
	{
		String clientId = config.getValue(providerKey + OAuthClientProperties.CLIENT_ID);
		String authzEndpoint = config.getValue(providerKey + OAuthClientProperties.PROVIDER_LOCATION);
		String scopes = config.getValue(providerKey + OAuthClientProperties.SCOPES);

		OAuthContext context = new OAuthContext();
		
		AuthenticationRequest req = new AuthenticationRequest(
				new URI(authzEndpoint),
				new ResponseType(ResponseType.Value.CODE),
				Scope.parse(scopes),
				new ClientID(clientId),
				new URI(responseConsumerAddress),
				new State(context.getRelayState()),
				null);

		
		context.setRequest(req, req.toURI(), providerKey);
		contextManagement.addAuthnContext(context);
		return context;
	}

	/**
	 * The real OAuth workhorse. The authz code response verification needs not to be done: the state is 
	 * correct as otherwise there would be no match with the {@link OAuthContext}. However we need to
	 * use the authz code to retrieve access token. The access code may include everything we need. But it 
	 * may also happen that we need to perform one more query to obtain additional profile information.
	 * @throws AuthenticationException 
	 *   
	 */
	@Override
	public AuthenticationResult verifyOAuthAuthzResponse(OAuthContext context) throws AuthenticationException
	{
		String error = context.getErrorCode();
		if (error != null)
		{
			throw new AuthenticationException("OAuth provider returned an error: " + 
					error + (context.getErrorDescription() != null ? 
							" " + context.getErrorDescription() : ""));
		}
		
		boolean openIdConnectMode = config.getBooleanValue(context.getProviderConfigKey() + 
				OAuthClientProperties.OPENID_CONNECT);
		
		Map<String, String> attributes;
		try
		{
			attributes = openIdConnectMode ? getUserInfoWithOpenIdConnect(context) :
				getUserInfoWithPlainOAuth2(context);
		} catch (SerializeException | ParseException | IOException | URISyntaxException
				| java.text.ParseException e)
		{
			throw new AuthenticationException("Problem during user information retrieval", e);
		}

		RemotelyAuthenticatedInput input = convertInput(context, attributes);

		String translationProfile = config.getValue(context.getProviderConfigKey() + 
				OAuthClientProperties.TRANSLATION_PROFILE);
		
		return getResult(input, translationProfile);
	}
	
	private HTTPResponse retrieveAccessTokenGeneric(OAuthContext context) 
			throws SerializeException, IOException, URISyntaxException
	{
		String clientId = config.getValue(context.getProviderConfigKey() + OAuthClientProperties.CLIENT_ID);
		String clientSecret = config.getValue(context.getProviderConfigKey() + 
				OAuthClientProperties.CLIENT_SECRET);
		String tokenEndpoint = config.getValue(context.getProviderConfigKey() + 
				OAuthClientProperties.ACCESS_TOKEN_ENDPOINT);
		//TODO - should be configurable
		ClientAuthentication clientAuthn = new ClientSecretPost(
				new ClientID(clientId), 
				new Secret(clientSecret));
		AuthorizationCodeGrant authzCodeGrant = new AuthorizationCodeGrant(
				new AuthorizationCode(context.getAuthzCode()), 
				new URI(responseConsumerAddress)); 
		TokenRequest request = new TokenRequest(
				new URI(tokenEndpoint),
				clientAuthn,
				authzCodeGrant);
		HTTPRequest httpRequest = request.toHTTPRequest(); 
		log.trace("Exchanging authorization code for access token with request: " + httpRequest.getURL() + 
				"?" + httpRequest.getQuery());
		HTTPResponse response = httpRequest.send();
		
		log.debug("Received answer: " + response.getStatusCode());
		if (response.getStatusCode() != 200)
			log.debug("Error received. Contents: " + response.getContent());
		else
			log.trace("Received token: " + response.getContent());
		return response;
	}
	
	private Map<String, String> getUserInfoWithOpenIdConnect(OAuthContext context) 
			throws AuthenticationException, SerializeException, IOException, URISyntaxException, 
			ParseException, java.text.ParseException 
	{
		HTTPResponse response = retrieveAccessTokenGeneric(context);
		OIDCAccessTokenResponse acResponse = OIDCAccessTokenResponse.parse(response);

		AccessToken accessTokenGeneric = acResponse.getAccessToken();
		if (!(accessTokenGeneric instanceof BearerAccessToken))
		{
			throw new AuthenticationException("OAuth provider returned an access token which is not "
					+ "the bearer token, it is unsupported and most probably a problem on "
					+ "the provider side. The received token type is: " + 
					accessTokenGeneric.getType().toString());
		}
		BearerAccessToken accessToken = (BearerAccessToken) accessTokenGeneric;
		
		Map<String, String> ret = new HashMap<String, String>();
		toAttributes(acResponse.getIDToken().getJWTClaimsSet(), ret);
		
		String discoveryEndpoint = config.getValue(context.getProviderConfigKey() + 
				OAuthClientProperties.OPENID_DISCOVERY);
		
		OIDCProviderMetadata providerMeta = metadataManager.getMetadata(discoveryEndpoint);
		
		URI userInfoEndpoint = providerMeta.getUserInfoEndpointURI();
		if (userInfoEndpoint != null)
		{
			fetchOpenIdUserInfo(accessToken, userInfoEndpoint, ret);
		}
		
		return ret;
	}
	
	private void fetchOpenIdUserInfo(BearerAccessToken accessToken, URI userInfoEndpoint, Map<String, String> ret) 
			throws AuthenticationException, SerializeException, IOException, ParseException, java.text.ParseException
	{
		UserInfoRequest uiRequest = new UserInfoRequest(userInfoEndpoint, accessToken);
		HTTPResponse uiHttpResponse = uiRequest.toHTTPRequest().send();
		UserInfoResponse uiResponse = UserInfoResponse.parse(uiHttpResponse);
		if (uiResponse instanceof UserInfoErrorResponse)
		{
			UserInfoErrorResponse errorResp = (UserInfoErrorResponse) uiResponse;
			throw new AuthenticationException("Authentication was successful, but an error "
					+ "occurred during user information endpoint query: " + 
					errorResp.getErrorObject().getCode() +  
					(errorResp.getErrorObject().getDescription() != null ? 
							" " + errorResp.getErrorObject().getDescription() : ""));
		}
		UserInfoSuccessResponse uiResponseS = (UserInfoSuccessResponse) uiResponse;
		ReadOnlyJWTClaimsSet claimSet = uiResponseS.getUserInfoJWT().getJWTClaimsSet();
		toAttributes(claimSet, ret);
	}
	
	private void toAttributes(ReadOnlyJWTClaimsSet claimSet, Map<String, String> attributes)
	{
		Map<String, Object> claims = claimSet.getAllClaims();
		for (Map.Entry<String, Object> claim: claims.entrySet())
			attributes.put(claim.getKey(), claim.getValue().toString());
	}
	
	private Map<String, String> getUserInfoWithPlainOAuth2(OAuthContext context) 
	{
		throw new RuntimeException("not implemented");
	}
	
	private RemotelyAuthenticatedInput convertInput(OAuthContext context, Map<String, String> attributes)
	{
		String tokenEndpoint = config.getValue(context.getProviderConfigKey() + 
				OAuthClientProperties.ACCESS_TOKEN_ENDPOINT);
		RemotelyAuthenticatedInput input = new RemotelyAuthenticatedInput(tokenEndpoint);
		for (Map.Entry<String, String> attr: attributes.entrySet())
		{
			input.addAttribute(new RemoteAttribute(attr.getKey(), attr.getValue()));
		}
		return input;
	}
}







