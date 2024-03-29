/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.internal;

import java.util.List;

import org.apache.ibatis.session.SqlSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.db.DBSessionManager;
import pl.edu.icm.unity.db.generic.authn.AuthenticatorInstanceDB;
import pl.edu.icm.unity.engine.authn.AuthenticatorLoader;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.server.api.internal.AuthenticatorsManagement;
import pl.edu.icm.unity.server.authn.AuthenticationOption;
import pl.edu.icm.unity.types.authn.AuthenticationOptionDescription;

/**
 * Implementation of {@link AuthenticatorsManagement}
 * 
 * @author K. Benedyczak
 */
@Component
public class AuthenticatorsManagementImpl implements AuthenticatorsManagement
{
	private DBSessionManager db;
	private AuthenticatorLoader authnLoader;
	private AuthenticatorInstanceDB authenticatorDB;
	
	@Autowired
	public AuthenticatorsManagementImpl(DBSessionManager db, AuthenticatorLoader authnLoader,
			AuthenticatorInstanceDB authenticatorDB)
	{
		super();
		this.db = db;
		this.authnLoader = authnLoader;
		this.authenticatorDB = authenticatorDB;
	}


	@Override
	public List<AuthenticationOption> getAuthenticatorUIs(List<AuthenticationOptionDescription> authnList) 
			throws EngineException
	{
		SqlSession sql = db.getSqlSession(false);
		List<AuthenticationOption> authenticators;
		try 
		{
			authenticators = authnLoader.getAuthenticators(authnList, sql);
			sql.commit();
		} finally
		{
			db.releaseSqlSession(sql);
		}
		return authenticators;
	}
	
	@Override
	public void removeAllPersistedAuthenticators() throws EngineException
	{
		SqlSession sql = db.getSqlSession(false);
		try 
		{
			authenticatorDB.removeAllNoCheck(sql);
			sql.commit();
		} finally
		{
			db.releaseSqlSession(sql);
		}
	}
}
