package com.redhat.sso;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.keycloak.adapters.servlet.OIDCFilterSessionStore.SerializableKeycloakAccount;
import org.keycloak.adapters.spi.KeycloakAccount;

import com.redhat.sso.client.SsoApiService;
import com.redhat.sso.client.model.SsoUser;
import com.redhat.sso.testdata.UserDataFactory;

@Path("/api/testdata")
public class TestDataResource {
	
	private List<SsoUser> userMap = new ArrayList<>();
	private Random random = new Random();
	
	
	@Inject
    @RestClient
    SsoApiService apiResource;
	
	@Inject
	UserDataFactory factory;
	
	
	
	/**
	 * 
	 * @param initialAccessToken Can be obtained from the load-test realm admin console tab client registration.
	 * @return
	 */
	@GET
	@Path("/users/{users}")
    @Produces(MediaType.TEXT_PLAIN)
    public String registerUsers(@PathParam("users") Long users, @Context HttpServletRequest request) {
		SerializableKeycloakAccount account = (SerializableKeycloakAccount) request.getSession().getAttribute(KeycloakAccount.class.getName());
		String autHeaderValue = "bearer " + account.getKeycloakSecurityContext().getTokenString();
		for (int i = 0; i < users; i++) {
			apiResource.createUser(factory.newUser(), autHeaderValue);
			//TODO Get User and add to map
		}
		return "ok";
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
		SerializableKeycloakAccount account = (SerializableKeycloakAccount) request.getSession().getAttribute(KeycloakAccount.class.getName());
		String autHeaderValue = "bearer " + account.getKeycloakSecurityContext().getTokenString();
		userMap = new ArrayList<>(apiResource.getUsers(true, 1000000, autHeaderValue));
		return "ok";
	}
	
	public Optional<SsoUser> getRandomUser() {
		if (userMap.size() - 1 >= 0) {
			return Optional.of(userMap.get(random.nextInt(userMap.size() - 1)));
		} else {
			return Optional.empty();
		}
		
	}
	
}
