/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.stdext.credential;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Date;
import java.util.Deque;
import java.util.List;
import java.util.Random;

import org.apache.log4j.Logger;
import org.bouncycastle.util.Arrays;

import pl.edu.icm.unity.Constants;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.IllegalAttributeTypeException;
import pl.edu.icm.unity.exceptions.IllegalAttributeValueException;
import pl.edu.icm.unity.exceptions.IllegalCredentialException;
import pl.edu.icm.unity.exceptions.IllegalGroupValueException;
import pl.edu.icm.unity.exceptions.IllegalPreviousCredentialException;
import pl.edu.icm.unity.exceptions.IllegalTypeException;
import pl.edu.icm.unity.exceptions.InternalException;
import pl.edu.icm.unity.notifications.NotificationProducer;
import pl.edu.icm.unity.server.authn.AbstractLocalVerificator;
import pl.edu.icm.unity.server.authn.AuthenticatedEntity;
import pl.edu.icm.unity.server.authn.AuthenticationException;
import pl.edu.icm.unity.server.authn.AuthenticationResult;
import pl.edu.icm.unity.server.authn.AuthenticationResult.Status;
import pl.edu.icm.unity.server.authn.CredentialHelper;
import pl.edu.icm.unity.server.authn.CredentialReset;
import pl.edu.icm.unity.server.authn.EntityWithCredential;
import pl.edu.icm.unity.server.authn.LocalSandboxAuthnContext;
import pl.edu.icm.unity.server.authn.remote.SandboxAuthnResultCallback;
import pl.edu.icm.unity.server.utils.Log;
import pl.edu.icm.unity.stdext.identity.EmailIdentity;
import pl.edu.icm.unity.stdext.identity.UsernameIdentity;
import pl.edu.icm.unity.stdext.utils.CryptoUtils;
import pl.edu.icm.unity.types.authn.CredentialPublicInformation;
import pl.edu.icm.unity.types.authn.LocalCredentialState;

import com.fasterxml.jackson.databind.node.ObjectNode;

import edu.vt.middleware.password.AlphabeticalSequenceRule;
import edu.vt.middleware.password.CharacterCharacteristicsRule;
import edu.vt.middleware.password.DigitCharacterRule;
import edu.vt.middleware.password.LengthRule;
import edu.vt.middleware.password.LowercaseCharacterRule;
import edu.vt.middleware.password.NonAlphanumericCharacterRule;
import edu.vt.middleware.password.NumericalSequenceRule;
import edu.vt.middleware.password.Password;
import edu.vt.middleware.password.PasswordData;
import edu.vt.middleware.password.PasswordValidator;
import edu.vt.middleware.password.QwertySequenceRule;
import edu.vt.middleware.password.RepeatCharacterRegexRule;
import edu.vt.middleware.password.Rule;
import edu.vt.middleware.password.RuleResult;
import edu.vt.middleware.password.UppercaseCharacterRule;

/**
 * Ordinary password credential verificator. Highly configurable: it is possible to set minimal length,
 * what character classes are required, minimum number of character classes, how many previous passwords 
 * should be stored and not repeated after change, how often the password must be changed.
 * <p>
 * Additionally configuration of the credential may allow for password reset feature. Then are stored
 * email verification settings, security question settings and confirmation code length.
 * 
 * @see PasswordCredential
 * @author K. Benedyczak
 */
public class PasswordVerificator extends AbstractLocalVerificator implements PasswordExchange
{ 	
	private static final Logger log = Log.getLogger(Log.U_SERVER, PasswordVerificator.class);
	static final String[] IDENTITY_TYPES = {UsernameIdentity.ID, EmailIdentity.ID};

	private NotificationProducer notificationProducer;
	private CredentialHelper credentialHelper;
	private Random random = new SecureRandom();
	
	private PasswordCredential credential = new PasswordCredential();

	public PasswordVerificator(String name, String description,
			NotificationProducer notificationProducer, CredentialHelper credentialHelper)
	{
		super(name, description, PasswordExchange.ID, true);
		this.notificationProducer = notificationProducer;
		this.credentialHelper = credentialHelper;
	}

	@Override
	public String getSerializedConfiguration() throws InternalException
	{
		return credential.getSerializedConfiguration();
	}

	@Override
	public void setSerializedConfiguration(String json) throws InternalException
	{
		credential.setSerializedConfiguration(json);
	}

	/**
	 * The rawCredential must be in JSON format, see {@link PasswordToken} for details.
	 */
	@Override
	public String prepareCredential(String rawCredential, String previousCredential, 
			String currentCredential)
			throws IllegalCredentialException, InternalException
	{
		Deque<PasswordInfo> currentPasswords = PasswordCredentialDBState.fromJson(currentCredential).
				getPasswords();
		
		PasswordToken pToken = PasswordToken.loadFromJson(rawCredential);
		
		//verify that existing password was correctly provided 
		if (previousCredential!= null && !currentPasswords.isEmpty())
		{
			PasswordToken checkedToken = PasswordToken.loadFromJson(previousCredential);
			PasswordInfo current = currentPasswords.getFirst();
			if (!checkPasswordInternal(checkedToken.getPassword(), current))
				throw new IllegalPreviousCredentialException("The current credential is incorrect");
		}
		
		verifyNewPassword(pToken.getExistingPassword(), pToken.getPassword(), currentPasswords);
		
		if (credential.getPasswordResetSettings().isEnabled() && 
				credential.getPasswordResetSettings().isRequireSecurityQuestion())
		{
			if (pToken.getAnswer() == null || pToken.getQuestion() == -1)
				throw new IllegalCredentialException("The credential must select a security question " +
						"and provide an answer for it");
			if (pToken.getQuestion() < 0 || pToken.getQuestion() >= 
					credential.getPasswordResetSettings().getQuestions().size())
				throw new IllegalCredentialException("The chosen answer for security question is invalid");
		}
		
		
		String salt = random.nextLong() + "";
		byte[] hashed = CryptoUtils.hash(pToken.getPassword(), salt, credential.getRehashNumber());

		PasswordInfo currentPassword = new PasswordInfo(hashed, salt, credential.getRehashNumber());
		if (credential.getHistorySize() <= currentPasswords.size() && !currentPasswords.isEmpty())
			currentPasswords.removeLast();
		currentPasswords.addFirst(currentPassword);

		return PasswordCredentialDBState.toJson(credential, currentPasswords, pToken);
	}


	@Override
	public String invalidate(String currentCredential)
	{
		ObjectNode root;
		try
		{
			root = (ObjectNode) Constants.MAPPER.readTree(currentCredential);
			root.put("outdated", true);
			return Constants.MAPPER.writeValueAsString(root);
		} catch (Exception e)
		{
			throw new InternalException("Can't deserialize password credential from JSON", e);
		}
	}

	@Override
	public CredentialPublicInformation checkCredentialState(String currentCredential) throws InternalException
	{
		PasswordCredentialDBState parsedCred = PasswordCredentialDBState.fromJson(currentCredential);
		Deque<PasswordInfo> currentPasswords = parsedCred.getPasswords();
		if (currentPasswords.isEmpty())
			return new CredentialPublicInformation(LocalCredentialState.notSet, "");
		
		PasswordInfo currentPassword = currentPasswords.getFirst();
		PasswordExtraInfo pei = new PasswordExtraInfo(currentPassword.getTime(), 
				parsedCred.getSecurityQuestion());
		String extraInfo = pei.toJson();
		
		if (isCurrentCredentialOutdated(parsedCred))
			return new CredentialPublicInformation(LocalCredentialState.outdated, extraInfo);
		return new CredentialPublicInformation(LocalCredentialState.correct, extraInfo);
	}

	/**
	 * Checks if the provided password is valid. If it is then it is checked if it is still 
	 * fulfilling all actual rules of the credential's configuration. If not then it is returned that 
	 * credential state is outdated. 
	 */
	@Override
	public AuthenticationResult checkPassword(String username, String password, 
			SandboxAuthnResultCallback sandboxCallback)
	{
		AuthenticationResult authenticationResult = checkPasswordInternal(username, password);
		if (sandboxCallback != null)
			sandboxCallback.sandboxedAuthenticationDone(new LocalSandboxAuthnContext(authenticationResult));
		return authenticationResult;
	}

	public AuthenticationResult checkPasswordInternal(String username, String password)
	{
		EntityWithCredential resolved;
		try
		{
			resolved = identityResolver.resolveIdentity(username, 
					IDENTITY_TYPES, credentialName);
		} catch (Exception e)
		{
			log.debug("The user for password authN can not be found: " + username, e);
			return new AuthenticationResult(Status.deny, null);
		}
		
		try
		{
			String dbCredential = resolved.getCredentialValue();
			PasswordCredentialDBState credState = PasswordCredentialDBState.fromJson(dbCredential);
			Deque<PasswordInfo> credentials = credState.getPasswords();
			if (credentials.isEmpty())
			{
				log.debug("The user has no password set: " + username);
				return new AuthenticationResult(Status.deny, null);
			}
			PasswordInfo current = credentials.getFirst();
			if (!checkPasswordInternal(password, current))
			{
				log.debug("Password provided by " + username + " is invalid");
				return new AuthenticationResult(Status.deny, null);
			}
			boolean isOutdated = isCurrentPasswordOutdated(password, credState, resolved);
			AuthenticatedEntity ae = new AuthenticatedEntity(resolved.getEntityId(), username, isOutdated);
			return new AuthenticationResult(Status.success, ae);
		} catch (Exception e)
		{
			log.debug("Error during password verification for " + username, e);
			return new AuthenticationResult(Status.deny, null);
		}
	}

	private boolean checkPasswordInternal(String password, PasswordInfo current) 
	{
		byte[] testedHash = CryptoUtils.hash(password, current.getSalt(), current.getRehashNumber());
		return Arrays.areEqual(testedHash, current.getHash());
	}
	
	
	@Override
	public CredentialReset getCredentialResetBackend()
	{
		return new CredentialResetImpl(notificationProducer, identityResolver, 
				this, credentialHelper,
				credentialName, getSerializedConfiguration(), 
				credential.getPasswordResetSettings());
	}

	/**
	 * As {@link #isCurrentCredentialOutdated(PasswordCredentialDBState)} but also
	 * checks if the provided password is fulfilling the actual rules of the credential
	 * (what can not be checked with the hashed in-db version of the credential).
	 * <p>
	 * Additionally, if it is detected that the credential is outdated by checking the password, 
	 * this newly received information is stored in DB: credential is updated to be manually outdated.
	 *   
	 * @param password
	 * @throws AuthenticationException 
	 * @throws IllegalGroupValueException 
	 * @throws IllegalAttributeTypeException 
	 * @throws IllegalTypeException 
	 * @throws IllegalAttributeValueException 
	 */
	private boolean isCurrentPasswordOutdated(String password, PasswordCredentialDBState credState, 
			EntityWithCredential resolved) throws AuthenticationException
	{
		if (isCurrentCredentialOutdated(credState))
			return true;

		PasswordValidator validator = getPasswordValidator();
		RuleResult result = validator.validate(new PasswordData(new Password(password)));
		if (!result.isValid())
		{
			String invalidated = invalidate(resolved.getCredentialValue());
			try
			{
				credentialHelper.updateCredential(resolved.getEntityId(), resolved.getCredentialName(), 
							invalidated);
			} catch (EngineException e)
			{
				throw new AuthenticationException("Problem invalidating outdated credential", e);
			}
			return true;
		}
		return false;
	}

	/**
	 * Checks if the provided credential state is fulfilling the rules of the credential,
	 * if it was expired manually and if password did expire on its own.  
	 *  
	 * @param password
	 * @return true if credential is invalid
	 */
	private boolean isCurrentCredentialOutdated(PasswordCredentialDBState credState)
	{
		if (credState.isOutdated())
			return true;
		if (credState.getSecurityQuestion() == null && 
				credential.getPasswordResetSettings().isEnabled() && 
				credential.getPasswordResetSettings().isRequireSecurityQuestion())
			return true;
		PasswordInfo current = credState.getPasswords().getFirst();
		Date validityEnd = new Date(current.getTime().getTime() + credential.getMaxAge());
		if (new Date().after(validityEnd))
			return true;
		if (credential.getRehashNumber() != current.getRehashNumber())
			return true;
		if (credential.getPasswordResetSettings().isEnabled() && 
				credential.getPasswordResetSettings().isRequireSecurityQuestion() &&
				credential.getRehashNumber() != credState.getAnswerRehashNumber())
			return true;
		return false;
	}
	
	private void verifyNewPassword(String existingPassword, String password, 
			Deque<PasswordInfo> currentCredentials) throws IllegalCredentialException
	{
		PasswordValidator validator = getPasswordValidator();
		
		RuleResult result = validator.validate(new PasswordData(new Password(password)));
		//TODO when i18n is added this should be extended to provide better information.
		if (!result.isValid())
			throw new IllegalCredentialException("Password is too weak");
		
		for (PasswordInfo pi: currentCredentials)
		{
			byte[] newHashed = CryptoUtils.hash(password, pi.getSalt(), pi.getRehashNumber());
			if (Arrays.areEqual(newHashed, pi.getHash()))
				throw new IllegalCredentialException("The same password was recently used");
		}
	}

	private PasswordValidator getPasswordValidator()
	{
		List<Rule> ruleList = new ArrayList<Rule>();
		ruleList.add(new LengthRule(credential.getMinLength(), 512));
		CharacterCharacteristicsRule charRule = new CharacterCharacteristicsRule();
		charRule.getRules().add(new DigitCharacterRule(1));
		charRule.getRules().add(new NonAlphanumericCharacterRule(1));
		charRule.getRules().add(new UppercaseCharacterRule(1));
		charRule.getRules().add(new LowercaseCharacterRule(1));
		charRule.setNumberOfCharacteristics(credential.getMinClassesNum());
		ruleList.add(charRule);
		if (credential.isDenySequences())
		{
			ruleList.add(new AlphabeticalSequenceRule());
			ruleList.add(new NumericalSequenceRule(3, true));
			ruleList.add(new QwertySequenceRule());
			ruleList.add(new RepeatCharacterRegexRule(4));
		}
		return new PasswordValidator(ruleList);
	}

	@Override
	public String prepareCredential(String rawCredential, String currentCredential)
			throws IllegalCredentialException, InternalException
	{
		return prepareCredential(rawCredential, null, currentCredential);
	}
}





