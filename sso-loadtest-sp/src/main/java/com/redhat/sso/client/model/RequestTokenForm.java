package com.redhat.sso.client.model;

import javax.ws.rs.FormParam;
import javax.ws.rs.core.MediaType;

import org.jboss.resteasy.annotations.providers.multipart.PartType;

public class RequestTokenForm {
	
	@FormParam("client_id")
    @PartType(MediaType.TEXT_PLAIN)
	String clientId;
	
	@FormParam("client_secret")
    @PartType(MediaType.TEXT_PLAIN)
	String clientSecret;
	
	@FormParam("username")
    @PartType(MediaType.TEXT_PLAIN)
	String username;
	
	
	@FormParam("password")
    @PartType(MediaType.TEXT_PLAIN)
	String password;
	
	@FormParam("grant_type")
    @PartType(MediaType.TEXT_PLAIN)
	String gratType;

	public RequestTokenForm(String clientId, String clientSecret, String username, String password, String gratType) {
		super();
		this.clientId = clientId;
		this.clientSecret = clientSecret;
		this.username = username;
		this.password = password;
		this.gratType = gratType;
	}

	public RequestTokenForm() {
		super();
	}
	
	
}
