package com.redhat.sso;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.Scanner;

import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.metrics.MetricUnits;
import org.eclipse.microprofile.metrics.annotation.Gauge;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.keycloak.adapters.KeycloakDeployment;
import org.keycloak.adapters.KeycloakDeploymentBuilder;
import org.keycloak.adapters.servlet.OIDCFilterSessionStore.SerializableKeycloakAccount;
import org.keycloak.adapters.spi.KeycloakAccount;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.redhat.sso.client.SsoApiService;
import com.redhat.sso.client.SsoOidcService;
import com.redhat.sso.client.model.RequestTokenForm;
import com.redhat.sso.client.model.SsoUser;
import com.redhat.sso.client.model.TokenResponse;
import com.redhat.sso.metrics.TestDataMetrics;
import com.redhat.sso.testdata.UserDataFactory;

import io.quarkus.runtime.StartupEvent;

@Path("/api/testdata")
public class TestDataResource {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(TestDataResource.class);
	private List<SsoUser> userMap = new ArrayList<>();
	private Random random = new Random();
	private TestDataMetrics metrics = new TestDataMetrics();
	
	@ConfigProperty(name = "sso.service.username")
	Optional<String> username;
	@ConfigProperty(name = "sso.service.password")
	Optional<String> password;
	
	@Inject
    @RestClient
    SsoApiService apiResource;
	@Inject
    @RestClient
    SsoOidcService oidcResource;
	
	@Inject
	UserDataFactory factory;
	
	void onStart(@Observes StartupEvent ev, @Context ServletContext ctx) throws IOException {
		LOGGER.info("Loading users if service account data is provided...");
		Optional<TokenResponse> tokenResponse = obtainAccessTokenWithServiceAccount(ctx);
		tokenResponse.ifPresent(resp -> loadUsers(resp.getAccessToken()));
	}


	private Optional<TokenResponse> obtainAccessTokenWithServiceAccount(ServletContext ctx) throws IOException {
		KeycloakDeployment keycloakDeployment = loadKeycloakJson(ctx);
		Optional<TokenResponse> tokenResponse = username.flatMap(
				usr -> password.flatMap(pw -> 
				{
					try {
						return Optional.of(oidcResource.obtainAccessToken(new RequestTokenForm(keycloakDeployment.getResourceName(),
								(String) keycloakDeployment.getResourceCredentials().get("secret"), usr, pw, "password")));
					} catch (WebApplicationException e) {
						logWebApplicationException(e);
						return Optional.empty();
					}
				}));
		if (!tokenResponse.isPresent()) {
			LOGGER.warn("For automated user loading you must provide sso.service.username and sso.service.password.");
		}
		return tokenResponse;
	}

	

	private void logWebApplicationException(WebApplicationException e) {
		if (e.getResponse().getEntity() instanceof InputStream) {
			InputStream in = ((InputStream) e.getResponse().getEntity());
			try (Scanner scanner = new Scanner(in)) {
				scanner.useDelimiter("\\Z");
				String errorResponse = e.getResponse().getStatusInfo().getReasonPhrase();
				if (scanner.hasNext()) {
					errorResponse = scanner.next();
				}
				LOGGER.error("Error obtaining access token with service account {}", errorResponse);
			}
		} else {
			LOGGER.error("Error obtaining access token with service account {}", e);
		}
	}
		
	
	private KeycloakDeployment loadKeycloakJson(ServletContext ctx) throws IOException {
		String path = "/WEB-INF/keycloak.json";
        try(InputStream is = ctx.getResourceAsStream(path)) {
        	  return KeycloakDeploymentBuilder.build(is);
        }
    }

	
	
	/**
	 * 
	 * @param initialAccessToken Can be obtained from the load-test realm admin console tab client registration.
	 * @return
	 */
	@GET
	@Path("/users/{users}")
    @Produces(MediaType.TEXT_PLAIN)
    public String registerUsers(@PathParam("users") Long users, @Context HttpServletRequest request) {
		LOGGER.debug("Registering {} new users", users);
		SerializableKeycloakAccount account = (SerializableKeycloakAccount) request.getSession().getAttribute(KeycloakAccount.class.getName());
		String autHeaderValue = "bearer " + account.getKeycloakSecurityContext().getTokenString();
		for (int i = 0; i < users; i++) {
			SsoUser newUser = factory.newUser();
			apiResource.createUser(newUser, autHeaderValue);
			if(LOGGER.isDebugEnabled()) {
				LOGGER.debug("{}: New user '{}' registered", i + 1, newUser.getUsername());
			}
			//TODO Get User and add to map
		}
		LOGGER.debug("success");
		return String.format("Registered %d new users", users);
	}
	
	/**
	 * 
	 * @param initialAccessToken Can be obtained from the load-test realm admin console tab client registration.
	 * @return
	 */
	@GET
	@Path("/loadusers")
    @Produces(MediaType.TEXT_PLAIN)
    public String loadUsers(@Context HttpServletRequest request) {
		LOGGER.debug("Loading users");
		SerializableKeycloakAccount account = (SerializableKeycloakAccount) request.getSession().getAttribute(KeycloakAccount.class.getName());
		return loadUsers(account.getKeycloakSecurityContext().getTokenString());
	}


	private String loadUsers(String accessTokenString) {
		String autHeaderValue = "bearer " + accessTokenString;
		userMap = new ArrayList<>(apiResource.getUsers(true, 1000000, autHeaderValue));
		int users = userMap.size();
		LOGGER.debug("Loaded {} users", users);
		metrics.setUserCount(users);
		return String.format("Loaded %d users", users);
	}
	
	public Optional<SsoUser> getRandomUser() {
		if (userMap.size() - 1 >= 0) {
			return Optional.of(userMap.get(random.nextInt(userMap.size() - 1)));
		} else {
			return Optional.empty();
		}
		
	}
	
	@Gauge(name = "test.data.user.count", unit = MetricUnits.NONE, description = "Number of successful SAML logins")
    public int userCount() {
        return metrics.getUserCount();
    }
	
}
