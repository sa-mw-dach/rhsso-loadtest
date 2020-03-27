package com.redhat.sso.client.model;

import javax.ws.rs.FormParam;
import javax.ws.rs.core.MediaType;

import org.jboss.resteasy.annotations.providers.multipart.PartType;

public class AuthnRequestForm {
	
	@FormParam("SAMLRequest")
    @PartType(MediaType.TEXT_PLAIN)
	public String samlRequest;

	public AuthnRequestForm(String sAMLRequest) {
		super();
		this.samlRequest = sAMLRequest;
	}

	public AuthnRequestForm() {
		super();
	}
	
	
}

