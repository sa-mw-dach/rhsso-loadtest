package com.redhat.sso.client.model;

import javax.json.bind.annotation.JsonbProperty;

public class TokenResponse {
	
	@JsonbProperty("access_token")
	private String accessToken;
	
	private String scope;

	public TokenResponse(String accessToken, String scope) {
		super();
		this.accessToken = accessToken;
		this.scope = scope;
	}

	public TokenResponse() {
		super();
	}
	
	
	public String getAccessToken() {
		return accessToken;
	}

	public void setAccessToken(String accessToken) {
		this.accessToken = accessToken;
	}

	public String getScope() {
		return scope;
	}

	public void setScope(String scope) {
		this.scope = scope;
	}
	
	
}
