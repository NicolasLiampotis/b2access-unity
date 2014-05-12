/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.saml.ecp;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Set;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.xmlbeans.XmlCursor;

import eu.emi.security.authn.x509.X509Credential;
import eu.unicore.samly2.SAMLConstants;
import pl.edu.icm.unity.saml.SAMLHelper;
import pl.edu.icm.unity.saml.sp.RemoteAuthnContext;
import pl.edu.icm.unity.saml.sp.SAMLSPProperties;
import pl.edu.icm.unity.saml.sp.SamlContextManagement;
import pl.edu.icm.unity.saml.xmlbeans.ecp.RelayStateDocument;
import pl.edu.icm.unity.saml.xmlbeans.ecp.RelayStateType;
import pl.edu.icm.unity.saml.xmlbeans.ecp.RequestDocument;
import pl.edu.icm.unity.saml.xmlbeans.ecp.RequestType;
import pl.edu.icm.unity.saml.xmlbeans.soap.Body;
import pl.edu.icm.unity.saml.xmlbeans.soap.Envelope;
import pl.edu.icm.unity.saml.xmlbeans.soap.EnvelopeDocument;
import pl.edu.icm.unity.saml.xmlbeans.soap.Header;
import xmlbeans.org.oasis.saml2.assertion.NameIDType;
import xmlbeans.org.oasis.saml2.protocol.AuthnRequestDocument;
import xmlbeans.org.oasis.saml2.protocol.IDPEntryType;
import xmlbeans.org.oasis.saml2.protocol.IDPListType;

/**
 * ECP servlet which performs the actual ECP profile processing over PAOS binding.
 * <p>
 * The GET request is used to ask for SAML request. The POST request is used to provide SAML response 
 * and obtain a JWT token which can be subsequently used with other Unity endpoints.
 * 
 * @author K. Benedyczak
 */
public class ECPServlet extends HttpServlet
{
	private SAMLSPProperties samlProperties;
	private String myAddress;
	private SamlContextManagement samlContextManagement;

	public ECPServlet(SAMLSPProperties samlProperties, SamlContextManagement samlContextManagement, 
			String myAddress)
	{
		this.samlProperties = samlProperties;
		this.myAddress = myAddress;
		this.samlContextManagement = samlContextManagement;
	}
	
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException
	{
		verifyRequestHeaders(req);
		
		RemoteAuthnContext context = new RemoteAuthnContext();
		EnvelopeDocument envDoc = generateECPEnvelope(context);
		samlContextManagement.addAuthnContext(context);
		
		resp.setContentType(ECPConstants.ECP_CONTENT_TYPE);
		resp.addHeader("Cache-Control", "no-cache, no-store, must-revalidate, private");
		resp.addHeader("Pragma", "no-cache");

		PrintWriter writer = resp.getWriter();
		envDoc.save(writer);
		writer.flush();
	}

	private EnvelopeDocument generateECPEnvelope(RemoteAuthnContext context)
	{
		EnvelopeDocument envDoc = EnvelopeDocument.Factory.newInstance();
		Envelope env = envDoc.addNewEnvelope();
		
		Header header = env.addNewHeader();
		XmlCursor curH = header.newCursor();
		generateEcpHeaders(curH, context);
		generatePaosHeader(curH);
		curH.dispose();
		
		Body body = env.addNewBody();
		XmlCursor curBody = body.newCursor();
		generateSamlRequest(curBody);
		curBody.dispose();
		
		return envDoc;
	}

	private void generateSamlRequest(XmlCursor curBody)
	{
		boolean sign = samlProperties.getBooleanValue(SAMLSPProperties.ECP_KEY + 
				SAMLSPProperties.IDP_SIGN_REQUEST);
		String requesterId = samlProperties.getValue(SAMLSPProperties.REQUESTER_ID);
		String requestedNameFormat = samlProperties.getValue(SAMLSPProperties.ECP_KEY + 
				SAMLSPProperties.IDP_REQUESTED_NAME_FORMAT);
		X509Credential credential = sign ? samlProperties.getRequesterCredential() : null;
		
		AuthnRequestDocument authnRequestDoc = SAMLHelper.createSAMLRequest(myAddress, sign, requesterId, null,
				requestedNameFormat, credential);
		
		XmlCursor curR = authnRequestDoc.newCursor();
		curR.copyXml(curBody);
		curR.dispose();
	}
	
	private void generatePaosHeader(XmlCursor curH)
	{
		pl.edu.icm.unity.saml.xmlbeans.paos.RequestDocument paosReqDoc = 
				pl.edu.icm.unity.saml.xmlbeans.paos.RequestDocument.Factory.newInstance();
		pl.edu.icm.unity.saml.xmlbeans.paos.RequestType paosReq = paosReqDoc.addNewRequest();
		
		paosReq.setMustUnderstand(true);
		paosReq.setActor("http://schemas.xmlsoap.org/soap/actor/next");
		paosReq.setService(ECPConstants.ECP_PROFILE);
		paosReq.setResponseConsumerURL(myAddress);
		
		XmlCursor curPa = paosReq.newCursor();
		curPa.copyXml(curH);
		curPa.dispose();
	}
	
	private void generateEcpHeaders(XmlCursor curH, RemoteAuthnContext context)
	{
		RequestDocument ecpRequestDoc = RequestDocument.Factory.newInstance();
		RequestType ecpReq = ecpRequestDoc.addNewRequest();
		ecpReq.setActor("http://schemas.xmlsoap.org/soap/actor/next");
		ecpReq.setMustUnderstand(true);
		
		NameIDType issuer = ecpReq.addNewIssuer();
		String requestrId = samlProperties.getValue(SAMLSPProperties.REQUESTER_ID);
		issuer.setFormat(SAMLConstants.NFORMAT_ENTITY);
		issuer.setStringValue(requestrId);
		
		IDPListType idps = ecpReq.addNewIDPList();
		Set<String> idpKeys = samlProperties.getStructuredListKeys(SAMLSPProperties.IDP_PREFIX);
		for (String idpKey: idpKeys)
		{
			String idpId = samlProperties.getValue(idpKey + SAMLSPProperties.IDP_ID);
			String idpName = samlProperties.getValue(idpKey + SAMLSPProperties.IDP_NAME);
			IDPEntryType idp = idps.addNewIDPEntry();
			idp.setProviderID(idpId);
			idp.setName(idpName);
		}
		
		RelayStateDocument relayStateDoc = RelayStateDocument.Factory.newInstance();
		RelayStateType relayState = relayStateDoc.addNewRelayState();
		relayState.setActor("http://schemas.xmlsoap.org/soap/actor/next");
		relayState.setMustUnderstand(true);
		relayState.setStringValue(context.getRelayState());
		XmlCursor curER = ecpReq.newCursor();
		curER.copyXml(curH);
		curER.dispose();
		
		XmlCursor curRS = relayState.newCursor();
		curRS.copyXml(curH);
		curRS.dispose();
	}
	
	private void verifyRequestHeaders(HttpServletRequest req) throws ServletException
	{
		String accept = req.getHeader("Accept");
		if (accept == null)
			throw new ServletException("No Accept header in request, what is mandatory for this service.");
		if (!accept.contains(ECPConstants.ECP_CONTENT_TYPE))
			throw new ServletException("Client must be able to accept " + ECPConstants.ECP_CONTENT_TYPE
					+ " what was not advertised in the Accept header.");
		
		String paos = req.getHeader("PAOS");
		if (paos == null)
			throw new ServletException("No PAOS header in request, what is mandatory for this service.");
		if (!paos.startsWith(ECPConstants.PAOS_VERSION))
			throw new ServletException("PAOS version incorrect, supported version is " + 
					ECPConstants.PAOS_VERSION);
		if (!paos.contains(ECPConstants.ECP_PROFILE))
			throw new ServletException("PAOS header must include support for the ECP profile, "
					+ "which is missing.");
		
	}
}
