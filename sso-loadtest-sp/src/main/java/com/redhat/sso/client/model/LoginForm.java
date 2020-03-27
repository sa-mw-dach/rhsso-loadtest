package com.redhat.sso.client.model;

import javax.ws.rs.FormParam;
import javax.ws.rs.core.MediaType;

import org.jboss.resteasy.annotations.providers.multipart.PartType;

public class LoginForm {

	@FormParam("username")
    @PartType(MediaType.TEXT_PLAIN)
	public String username;
	
	@FormParam("password")
    @PartType(MediaType.TEXT_PLAIN)
	public String password;

	public LoginForm() {
		super();
	}

	public LoginForm(String username, String password) {
		super();
		this.username = username;
		this.password = password;
	}
	
}
