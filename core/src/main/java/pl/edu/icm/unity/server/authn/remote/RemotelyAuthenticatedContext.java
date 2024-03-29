/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.server.authn.remote;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import pl.edu.icm.unity.exceptions.InternalException;
import pl.edu.icm.unity.server.api.internal.SessionParticipant;
import pl.edu.icm.unity.server.authn.InvocationContext;
import pl.edu.icm.unity.server.translation.in.MappingResult;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.EntityParam;
import pl.edu.icm.unity.types.basic.IdentityTaV;

/**
 * Holds information about a user which was obtained and verified by a remote upstream IdP.
 * The information in this class is in Unity format but need not to have counterparts in the local database.
 * 
 * @author K. Benedyczak
 */
public class RemotelyAuthenticatedContext
{
	private String remoteIdPName;
	private String inputTranslationProfile;
	private Set<SessionParticipant> sessionParticipants;
	private Collection<IdentityTaV> identities = new ArrayList<>();
	private EntityParam localMappedPrincipal;
	private Collection<Attribute<?>> attributes = new ArrayList<>();
	private Collection<String> groups = new ArrayList<>();
	private MappingResult mappingResult;
	private RemotelyAuthenticatedInput input;

	public RemotelyAuthenticatedContext(String remoteIdPName, String inputTranslationProfile)
	{
		this.remoteIdPName = remoteIdPName;
		this.inputTranslationProfile = inputTranslationProfile;
		try
		{
			InvocationContext ctx = InvocationContext.getCurrent();
			if (ctx.getTlsIdentity() != null)
				identities.add(ctx.getTlsIdentity());
		} catch (InternalException e)
		{
			//OK
		}
	}
	public Collection<IdentityTaV> getIdentities()
	{
		return identities;
	}
	public void addIdentities(Collection<IdentityTaV> identities)
	{
		this.identities.addAll(identities);
	}
	public Collection<Attribute<?>> getAttributes()
	{
		return attributes;
	}
	public void addAttributes(Collection<Attribute<?>> attributes)
	{
		this.attributes.addAll(attributes);
	}
	public Collection<String> getGroups()
	{
		return groups;
	}
	public void addGroups(Collection<String> groups)
	{
		this.groups.addAll(groups);
	}
	public EntityParam getLocalMappedPrincipal()
	{
		return localMappedPrincipal;
	}
	public void setLocalMappedPrincipal(EntityParam localMappedPrincipal)
	{
		this.localMappedPrincipal = localMappedPrincipal;
	}
	public String getRemoteIdPName()
	{
		return remoteIdPName;
	}
	public String getInputTranslationProfile()
	{
		return inputTranslationProfile;
	}
	public MappingResult getMappingResult() 
	{
		return mappingResult;
	}
	public void setMappingResult(MappingResult mappingResult) 
	{
		this.mappingResult = mappingResult;
	}
	public void setAuthnInput(RemotelyAuthenticatedInput input) 
	{
		this.input = input;
	}
	public RemotelyAuthenticatedInput getAuthnInput()
	{
		return input;
	}
	public Set<SessionParticipant> getSessionParticipants()
	{
		return sessionParticipants;
	}
	public void setSessionParticipants(Set<SessionParticipant> sessionParticipants)
	{
		this.sessionParticipants = new HashSet<SessionParticipant>();
		this.sessionParticipants.addAll(sessionParticipants);
	}
}