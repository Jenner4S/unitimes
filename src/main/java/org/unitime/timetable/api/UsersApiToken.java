/*
 * Licensed to The Apereo Foundation under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for
 * additional information regarding copyright ownership.
 *
 * The Apereo Foundation licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at:
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
*/
package org.unitime.timetable.api;

import java.math.BigInteger;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.stereotype.Service;
import org.unitime.timetable.defaults.ApplicationProperty;
import org.unitime.timetable.gwt.command.client.GwtRpcException;
import org.unitime.timetable.model.User;
import org.unitime.timetable.model.dao._RootDAO;
import org.unitime.timetable.security.UserContext;
import org.unitime.timetable.security.context.UniTimeUserContext;

/**
 * @author Tomas Muller
 */
@Service("apiToken")
public class UsersApiToken implements ApiToken {
	
	private static SecretKey secret() throws NoSuchAlgorithmException, InvalidKeySpecException {
		byte salt[] = new byte[] { (byte)0x33, (byte)0x7b, (byte)0x09, (byte)0x0e, (byte)0xcf, (byte)0x5a, (byte)0x58, (byte)0xd9 };
		SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
		KeySpec spec = new PBEKeySpec(ApplicationProperty.UrlEncoderSecret.value().toCharArray(), salt, 1024, 128);
		SecretKey key = factory.generateSecret(spec);
		return new SecretKeySpec(key.getEncoded(), "AES");
	}
	
	public static String encode(String text) {
		try {
			if (text == null || text.isEmpty()) return null;
			Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
			cipher.init(Cipher.ENCRYPT_MODE, secret());
			return new BigInteger(cipher.doFinal(text.getBytes())).toString(36);
		} catch (Exception e) {
			throw new GwtRpcException("Encoding failed: " + e.getMessage(), e);
		}
	}
	
	public static String decode(String text) {
		try {
			if (text == null || text.isEmpty()) return null;
			Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
			cipher.init(Cipher.DECRYPT_MODE, secret());
			return new String(cipher.doFinal(new BigInteger(text, 36).toByteArray()));
		} catch (Exception e) {
			throw new GwtRpcException("Decoding failed: " + e.getMessage(), e);
		}
	}	

	@Override
	public String getToken(String externalId, String secret) {
		try {
			return encode(externalId + "|" + secret);
		} catch (Exception e) {
			throw new IllegalArgumentException("Failed to encode API token: " + e.getMessage(), e);
		}
	}

	@Override
	public UserContext getContext(String token) {
		try {
			String decoded = decode(token);
			int split = decoded.indexOf('|');
			String externalId = decoded.substring(0, split);
			String secret = decoded.substring(split + 1);
			org.hibernate.Session hibSession = null;
			try {
				hibSession = new _RootDAO().createNewSession();
				User user = (User)hibSession.createQuery("select u from User u where u.externalUniqueId=:externalId")
						.setString("externalId", externalId)
						.setCacheable(true)
						.setMaxResults(1)
						.uniqueResult();
				if (user == null)
					throw new IllegalArgumentException("Failed to decode API token: user does not exist.");
				if (secret == null || !secret.equals(user.getPassword()))
					throw new IllegalArgumentException("Failed to decode API token: secret does not match.");
				return new UniTimeUserContext(user.getExternalUniqueId(), user.getUsername(), null, user.getPassword());
			} finally {
				hibSession.close();
			}
		} catch (IllegalArgumentException e) {
			throw e;
		} catch (Exception e) {
			throw new IllegalArgumentException("Failed to decode API token: " + e.getMessage(), e);
		}
	}
}
