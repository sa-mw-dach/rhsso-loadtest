package com.redhat.sso;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.Scanner;
import java.util.stream.IntStream;

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
import org.jboss.resteasy.annotations.SseElementType;
import org.keycloak.adapters.KeycloakDeployment;
import org.keycloak.adapters.KeycloakDeploymentBuilder;
import org.keycloak.adapters.servlet.KeycloakOIDCFilter;
import org.keycloak.adapters.servlet.OIDCFilterSessionStore.SerializableKeycloakAccount;
import org.keycloak.adapters.spi.KeycloakAccount;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.redhat.sso.client.SsoApiService;
import com.redhat.sso.client.SsoOidcService;
import com.redhat.sso.client.model.RequestTokenForm;
import com.redhat.sso.client.model.SsoInitialAccessTokenCreate;
import com.redhat.sso.client.model.SsoUser;
import com.redhat.sso.client.model.TokenResponse;
import com.redhat.sso.metrics.TestDataMetrics;
import com.redhat.sso.testdata.UserDataFactory;

import io.quarkus.runtime.StartupEvent;
import io.smallrye.mutiny.Multi;

@Path("/api/testdata")
public class TestDataResource {
	
	private static final String GRANT_TYPE = "password";
	private static final Logger LOGGER = LoggerFactory.getLogger(TestDataResource.class);
	private List<SsoUser> userMap = new ArrayList<>();
	private Random random = new Random();
	private TestDataMetrics metrics = new TestDataMetrics();
	
	@ConfigProperty(name = "sso.service.username")
	Optional<String> username;
	@ConfigProperty(name = "sso.service.password")
	Optional<String> password;
	@ConfigProperty(name = "sso.loadusers.onstartup")
	Optional<Boolean> loadUsersOnStartup;
	@ConfigProperty(name = "quarkus.application.version")
	Optional<String> appVersion;
	
	
	@Inject
    @RestClient
    SsoApiService apiResource;
	@Inject
    @RestClient
    SsoOidcService oidcResource;
	
	@Inject
	UserDataFactory factory;
	
	void onStart(@Observes StartupEvent ev) throws IOException {
		if (loadUsersOnStartup.map(b -> b).orElse(true)) {
			LOGGER.info("Loading users if service account data is provided...");
			Optional<TokenResponse> tokenResponse = obtainAccessTokenWithServiceAccount();
			tokenResponse.ifPresent(resp -> loadUsers(resp.getAccessToken()));
		}
	}


	Optional<TokenResponse> obtainAccessTokenWithServiceAccount() throws IOException {
		Optional<TokenResponse> tokenResponse = username.flatMap(
				usr -> password.flatMap(pw -> 
				{
					try {
						return Optional.of(oidcResource.obtainAccessToken(new RequestTokenForm("admin-cli", "", usr, pw, GRANT_TYPE)));
					} catch (WebApplicationException e) {
						logWebApplicationException(e);
						return Optional.empty();
					}
				}));
		if (!tokenResponse.isPresent()) {
			LOGGER.warn("For API access with service account you must provide sso.service.username and sso.service.password.");
		} else {
			LOGGER.debug("Token received");
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
		
	
	@SuppressWarnings("unchecked")
	Optional<KeycloakDeployment> loadKeycloakJson(Optional<ServletContext> optCtx) {
		return (Optional<KeycloakDeployment>) optCtx.map(ctx -> {
			try(InputStream is = ctx.getResourceAsStream("/WEB-INF/keycloak.json")) {
	      	  return Optional.of(KeycloakDeploymentBuilder.build(is));
			} catch(IOException e) {
				LOGGER.error("Error loading deployment via servlet context", e);
				return Optional.empty();
			}
		}).orElseGet(() -> {
			try(InputStream is = TestDataResource.class.getClassLoader().getResourceAsStream("/META-INF/resources/WEB-INF/keycloak.json")) {
	      	  return Optional.of(KeycloakDeploymentBuilder.build(is));
			} catch(IOException e) {
				LOGGER.error("Error loading deployment via classpath", e);
				return Optional.empty();
			}
		});
    }
	
	/**
	 * Reactive / async is not supported by {@link KeycloakOIDCFilter}, hence this method uses the service account.
	 * @param users
	 * @param request
	 * @return
	 * @throws IOException 
	 */
	@GET
	@Produces(MediaType.SERVER_SENT_EVENTS)
	@SseElementType(MediaType.TEXT_PLAIN)
	@Path("/reactive/users/{users}")
	public  Multi<String> registerUsersReactive(@PathParam("users") Integer users) throws IOException {
		LOGGER.debug("Registering {} new users", users);
		Optional<TokenResponse> tokenResponse = obtainAccessTokenWithServiceAccount();
		return tokenResponse.map(resp -> 
			Multi.createFrom().items(IntStream.range(0, users).mapToObj(i -> {
				SsoUser newUser = factory.newUser();
				apiResource.createUser(newUser, "bearer " + resp.getAccessToken());
				userMap.add(newUser);
				refreshUserCountMetrics();
				if(LOGGER.isDebugEnabled()) {
					LOGGER.debug("{}: New user '{}' registered", i + 1, newUser.getUsername());
				}
				return String.format("%d - %s", i + 1, newUser.getUsername());
			})).onItem().apply(s -> s)
		).orElseGet(() -> { 
			LOGGER.error("Could not obtain access token");
			return null;
		});
	}
	
	@GET
	@Produces(MediaType.TEXT_PLAIN)
	@Path("/initalaccesstoken")
	public String createInitialAccessToken() throws IOException {
		LOGGER.debug("Creating initial access token");
		Optional<TokenResponse> tokenResponse = obtainAccessTokenWithServiceAccount();
		return tokenResponse.map(resp -> 
			apiResource.createInitialAccessToken(new SsoInitialAccessTokenCreate(), "bearer " + resp.getAccessToken()).getToken()
		).orElseGet(() -> { 
			LOGGER.error("Could not create initial access token");
			return null;
		});
	}
	
	/**
	 * 
	 * @param initialAccessToken Can be obtained from the load-test realm admin console tab client registration.
	 * @return
	 */
	@GET
	@Path("/users/{users}")
    @Produces(MediaType.TEXT_PLAIN)
    public String registerUsers(@PathParam("users") Integer users, @Context HttpServletRequest request) {
		LOGGER.debug("Registering {} new users", users);
		SerializableKeycloakAccount account = (SerializableKeycloakAccount) request.getSession().getAttribute(KeycloakAccount.class.getName());
		String autHeaderValue = "bearer " + account.getKeycloakSecurityContext().getTokenString();
		for (int i = 0; i < users; i++) {
			SsoUser newUser = factory.newUser();
			apiResource.createUser(newUser, autHeaderValue);
			userMap.add(newUser);
			refreshUserCountMetrics();
			if(LOGGER.isDebugEnabled()) {
				LOGGER.debug("{}: New user '{}' registered", i + 1, newUser.getUsername());
			}
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


	String loadUsers(String accessTokenString) {
		String autHeaderValue = "bearer " + accessTokenString;
		userMap = new ArrayList<>(apiResource.getUsers(true, 1000000, autHeaderValue));
		int users = refreshUserCountMetrics();
		LOGGER.debug("Loaded {} users", users);
		return String.format("Loaded %d users", users);
	}


	private int refreshUserCountMetrics() {
		int users = userMap.size();
		metrics.setUserCount(users);
		return users;
	}
	
	public Optional<SsoUser> getRandomUser() {
		if (!userMap.isEmpty()) {
			return Optional.of(userMap.get(random.nextInt(userMap.size())));
		} else {
			return Optional.empty();
		}
		
	}
	
	@Gauge(name = "test.data.user.count", unit = MetricUnits.NONE, description = "Number of successful SAML logins")
    public int userCount() {
        return metrics.getUserCount();
    }
	
	@Gauge(name = "test.data.sso.version", unit = MetricUnits.NONE, description = "Version of RHSSSO")
	public int getSsoVersion() throws IOException {
		return obtainAccessTokenWithServiceAccount().map(r -> 
		{
			String verStr = apiResource.getServerInfo("bearer " + r.getAccessToken()).getSystemInfo().getVersion();
			return versionStringToInteger(verStr);
		}).orElse(-1);
	}


	private int versionStringToInteger(String verStr) {
		return Integer.parseInt(String.valueOf(verStr.charAt(0)) + String.valueOf(verStr.charAt(2)) + String.valueOf(verStr.charAt(4)));
	}
	
	@Gauge(name = "app.version", unit = MetricUnits.NONE, description = "Version of RHSSSO")
	public int getAppVersion() {
		return appVersion.map(this::versionStringToInteger).orElse(-1);
	}
	
}
