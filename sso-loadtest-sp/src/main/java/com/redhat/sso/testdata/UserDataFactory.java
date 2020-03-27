package com.redhat.sso.testdata;

import javax.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import com.github.javafaker.Faker;
import com.redhat.sso.client.model.SsoUser;
import com.redhat.sso.client.model.SsoUserCredentials;

@ApplicationScoped
public class UserDataFactory {
	private Faker faker = new Faker();
	
	@ConfigProperty(name = "password.secret.postfix")
	String paswordPostFix;
	public SsoUser newUser() {
		String username = faker.name().username();
		return new SsoUser(username, faker.name().firstName(), faker.name().lastName(),
				new SsoUserCredentials(derivePasswordFromUsername(username)));
	}
	
	public String derivePasswordFromUsername(String username) {
		return username + paswordPostFix;
	}
}
