package com.redhat.sso;

import java.io.InputStream;
import java.util.Scanner;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.keycloak.adapters.servlet.OIDCFilterSessionStore.SerializableKeycloakAccount;
import org.keycloak.adapters.spi.KeycloakAccount;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.redhat.sso.client.SsoOidcService;

/**
 * Resource to execute test requests for OIDC/OAuth2.
 * 
 */
@Path("/api/oidc/test")
public class OidcTestResource {

	private static final Logger LOGGER = LoggerFactory.getLogger(OidcTestResource.class);
	
	@Inject
    @RestClient
    SsoOidcService oidcResource;
	
	/**
	 * 
	 * @param initialAccessToken Can be obtained from the load-test realm admin console tab client registration.
	 * @return
	 */
	@GET
	@Path("/register/client")
    @Produces(MediaType.TEXT_PLAIN)
    public String registerClient(@Context HttpServletRequest request) {
		SerializableKeycloakAccount account = (SerializableKeycloakAccount) request.getSession().getAttribute(KeycloakAccount.class.getName());
		InputStream resourceAsStream = SamlTestResource.class.getClassLoader().getResourceAsStream("/META-INF/resources/WEB-INF/client-oidc-config.json");
		try(Scanner scanner = new Scanner(resourceAsStream, "UTF-8")) {
			String oidcConfigJson = scanner.useDelimiter("\\A").next();
			LOGGER.debug("Register oidc client with json config {}", oidcConfigJson);
			return oidcResource.registerClient(oidcConfigJson, "bearer " + account.getKeycloakSecurityContext().getTokenString());
		}
	}
	
}
