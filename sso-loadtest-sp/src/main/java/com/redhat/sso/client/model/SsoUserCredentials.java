package com.redhat.sso.client.model;

import java.util.Objects;

public class SsoUserCredentials {
	private String value;
	private String type;

	public SsoUserCredentials(String value) {
		super();
		this.value = value;
		this.type = "password";
	}

	public SsoUserCredentials(String value, String type) {
		super();
		this.value = value;
		this.type = type;
	}

	public SsoUserCredentials() {
		super();
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	@Override
	public int hashCode() {
		return Objects.hash(type, value);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		SsoUserCredentials other = (SsoUserCredentials) obj;
		return Objects.equals(type, other.type) && Objects.equals(value, other.value);
	}

	@Override
	public String toString() {
		return String.format("SsoUserCredentials [value=%s, type=%s]", "*******", type);
	}
	
}
