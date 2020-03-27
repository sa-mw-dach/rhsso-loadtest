package com.redhat.sso.client.model;

import java.util.Arrays;
import java.util.Objects;

public class SsoUser {
	
	private String username;
	private String firstName;
	private String lastName;
	private boolean enabled = true;
	private SsoUserCredentials[] credentials;
	
	public SsoUser(String username, String firstName, String lastName, SsoUserCredentials credentials) {
		super();
		this.username = username;
		this.firstName = firstName;
		this.lastName = lastName;
		this.credentials = new SsoUserCredentials[] {credentials};
	}
	
	

	public SsoUser(String username, String firstName, String lastName, SsoUserCredentials[] credentials) {
		super();
		this.username = username;
		this.firstName = firstName;
		this.lastName = lastName;
		this.credentials = credentials;
	}

	public SsoUser() {
		super();
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getFirstName() {
		return firstName;
	}

	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}

	public String getLastName() {
		return lastName;
	}

	public void setLastName(String lastName) {
		this.lastName = lastName;
	}

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public SsoUserCredentials[] getCredentials() {
		return credentials;
	}

	public void setCredentials(SsoUserCredentials[] credentials) {
		this.credentials = credentials;
	}

	@Override
	public int hashCode() {
		return Objects.hash(username);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		SsoUser other = (SsoUser) obj;
		return Objects.equals(username, other.username);
	}

	@Override
	public String toString() {
		return String.format("SsoUser [username=%s, firstName=%s, lastName=%s, enabled=%s, credentials=%s]", username,
				firstName, lastName, enabled, Arrays.toString(credentials));
	}
	
	
}


