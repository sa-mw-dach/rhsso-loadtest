package com.redhat.sso;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Path("/api/saml/protected")
public class SamlProtectedResource {

	@GET
    @Produces(MediaType.TEXT_PLAIN)
    public String hello() {
        return "Hello I am protected";
    }
}