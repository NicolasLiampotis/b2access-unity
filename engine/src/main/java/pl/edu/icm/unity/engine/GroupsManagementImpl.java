/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine;

import org.apache.ibatis.session.SqlSession;
import org.springframework.beans.factory.annotation.Autowired;

import pl.edu.icm.unity.db.DB;
import pl.edu.icm.unity.db.DBGroups;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.server.api.GroupsManagement;
import pl.edu.icm.unity.types.EntityParam;
import pl.edu.icm.unity.types.Group;
import pl.edu.icm.unity.types.GroupContents;

/**
 * Implementation of groups management. Responsible for top level transaction handling,
 * proper error logging and authorization.
 * 
 * @author K. Benedyczak
 */
public class GroupsManagementImpl implements GroupsManagement
{
	private DB db;
	private DBGroups dbGroups;
	
	@Autowired
	public GroupsManagementImpl(DB db, DBGroups dbGroups)
	{
		this.db = db;
		this.dbGroups = dbGroups;
	}

	@Override
	public void addGroup(Group toAdd) throws EngineException
	{
		SqlSession sql = db.getSqlSession(true);
		try
		{
			dbGroups.addGroup(toAdd, sql);
		} finally
		{
			sql.commit();
			db.releaseSqlSession(sql);
		}
	}

	@Override
	public void linkGroup(String targetPath, String sourcePath) throws EngineException
	{
		throw new RuntimeException("NOT implemented"); // TODO Auto-generated method stub
	}

	@Override
	public void unlinkGroup(String targetPath, String sourcePath) throws EngineException
	{
		throw new RuntimeException("NOT implemented"); // TODO Auto-generated method stub
	}

	@Override
	public void removeGroup(String path, boolean recursive) throws EngineException
	{
		throw new RuntimeException("NOT implemented"); // TODO Auto-generated method stub
	}

	@Override
	public void addSelfManagedGroup(Group toAdd) throws EngineException
	{
		throw new RuntimeException("NOT implemented"); // TODO Auto-generated method stub
	}

	@Override
	public void addMemberFromParent(String path, EntityParam entity) throws EngineException
	{
		throw new RuntimeException("NOT implemented"); // TODO Auto-generated method stub
	}

	@Override
	public void removeMember(String path, EntityParam entity) throws EngineException
	{
		throw new RuntimeException("NOT implemented"); // TODO Auto-generated method stub
	}

	@Override
	public GroupContents getContents(String path, int filter) throws EngineException
	{
		SqlSession sql = db.getSqlSession(true);
		try 
		{
			return dbGroups.getContents(path, filter, sql);
		} finally
		{
			sql.commit();
			db.releaseSqlSession(sql);
		}
	}

	@Override
	public void updateGroup(String path, Group group) throws EngineException
	{
		throw new RuntimeException("NOT implemented"); // TODO Auto-generated method stub
	}
	
}
