package com.redhat.sso.client;

import java.util.concurrent.CompletionStage;

import javax.ws.rs.Consumes;
import javax.ws.rs.CookieParam;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.MediaType;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.jboss.resteasy.annotations.Form;

import com.redhat.sso.client.model.AuthnRequestForm;
import com.redhat.sso.client.model.LoginForm;

@Path("/auth/realms/loadtest")
@RegisterRestClient(configKey = "sso")
public interface SsoSamlService {
	
	@POST
	@Consumes(MediaType.APPLICATION_XML)
	@Path("/clients-registrations/saml2-entity-descriptor")
	String registerClient(String samlConfigXml, @HeaderParam("Authorization") String initialAccessToken);

	@POST
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	@Produces(MediaType.TEXT_PLAIN)
	@Path("/protocol/saml")
	CompletionStage<String> sendAuthnRequest(@Form AuthnRequestForm data);

	@GET
	@Consumes(MediaType.TEXT_PLAIN)
	@Produces(MediaType.TEXT_HTML)
	@Path("/login-actions/authenticate")
	CompletionStage<String> requestLoginPage(@QueryParam("client_id") String clientId,
			@QueryParam("tab_id") String tabId,
			@CookieParam("Set-Cookie") Cookie setCookie
			);
	
	
	@POST
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	@Produces(MediaType.TEXT_PLAIN)
	@Path("/login-actions/authenticate")
	CompletionStage<String> sendLoginRequest(
			@QueryParam("client_id") String clientId,
			@QueryParam("tab_id") String tabId, 
			@QueryParam("session_code") String sessionCode,
			@QueryParam("execution") String execution,
			@CookieParam("Set-Cookie") Cookie setCookie,
			@Form LoginForm data);
	
	
}
 