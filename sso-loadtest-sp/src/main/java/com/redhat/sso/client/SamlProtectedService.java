package com.redhat.sso.client;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@RegisterRestClient
@Path("/api/saml/protected")
public interface SamlProtectedService {

	@GET
    @Produces(MediaType.TEXT_PLAIN)
	String hello();

}
