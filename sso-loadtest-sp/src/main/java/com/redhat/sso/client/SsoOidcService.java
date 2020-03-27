package com.redhat.sso.client;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.MediaType;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@Path("/auth/realms/loadtest")
@RegisterRestClient(configKey = "sso")
public interface SsoOidcService {
	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Path("/clients-registrations/default")
	String registerClient(String samlConfigXml, @HeaderParam("Authorization") String initialAccessToken);
	
	@GET
	@Consumes(MediaType.APPLICATION_JSON)
	@Path("/clients-registrations/default/loadtest-oidc")
	String getDefaultClientRepresentation(@HeaderParam("Authorization") String initialAccessToken);
}
