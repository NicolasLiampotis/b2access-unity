/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.db.export;

import java.io.IOException;
import java.util.List;

import org.apache.ibatis.session.SqlSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.ObjectMapper;

import pl.edu.icm.unity.db.DBIdentities;
import pl.edu.icm.unity.db.mapper.IdentitiesMapper;
import pl.edu.icm.unity.db.model.BaseBean;
import pl.edu.icm.unity.db.resolvers.IdentitiesResolver;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.types.basic.IdentityType;

/**
 * Handles import/export of identity types table.
 * @author K. Benedyczak
 */
@Component
public class IdentityTypesIE extends AbstractIE
{
	private final DBIdentities dbIdentities;
	private final IdentitiesResolver idResolver;
	
	@Autowired
	public IdentityTypesIE(ObjectMapper jsonMapper, DBIdentities dbIdentities,
			IdentitiesResolver idResolver)
	{
		super(jsonMapper);
		this.dbIdentities = dbIdentities;
		this.idResolver = idResolver;
	}

	public void serialize(SqlSession sql, JsonGenerator jg) throws JsonGenerationException, IOException
	{
		IdentitiesMapper mapper = sql.getMapper(IdentitiesMapper.class);
		List<BaseBean> beans = mapper.getIdentityTypes();
		jg.writeStartArray();
		for (BaseBean bean: beans)
		{
			jg.writeStartObject();
			serializeBaseBeanToJson(jg, bean);
			jg.writeEndObject();
		}
		jg.writeEndArray();
	}
	
	public void deserialize(SqlSession sql, JsonParser input) throws IOException, EngineException
	{
		JsonUtils.expect(input, JsonToken.START_ARRAY);
		while(input.nextToken() == JsonToken.START_OBJECT)
		{
			BaseBean bean = new BaseBean();
			deserializeBaseBeanFromJson(input, bean);
			JsonUtils.nextExpect(input, JsonToken.END_OBJECT);
			
			IdentityType idType = idResolver.resolveIdentityType(bean);
			dbIdentities.createIdentityType(sql, idType.getIdentityTypeProvider());
			dbIdentities.updateIdentityType(sql, idType);
		}
		JsonUtils.expect(input, JsonToken.END_ARRAY);
	}
}
