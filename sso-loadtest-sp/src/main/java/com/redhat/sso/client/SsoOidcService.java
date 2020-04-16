package com.redhat.sso.client;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.jboss.resteasy.annotations.Form;

import com.redhat.sso.client.model.RequestTokenForm;
import com.redhat.sso.client.model.TokenResponse;

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
	
	@POST
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	@Produces(MediaType.APPLICATION_JSON)
	@Path("protocol/openid-connect/token")
	TokenResponse obtainAccessToken(@Form RequestTokenForm data);
	
}
