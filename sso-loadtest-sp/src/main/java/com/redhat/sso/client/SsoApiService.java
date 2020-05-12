package com.redhat.sso.client;

import java.util.List;
import java.util.concurrent.CompletionStage;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.keycloak.representations.info.ServerInfoRepresentation;

import com.redhat.sso.client.model.SsoInitialAccessToken;
import com.redhat.sso.client.model.SsoInitialAccessTokenCreate;
import com.redhat.sso.client.model.SsoUser;

@Path("/auth")
@RegisterRestClient(configKey = "sso")
public interface SsoApiService {
	
	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@Path("/admin/realms/loadtest/users")
	CompletionStage<String> createUserAsync(SsoUser user, @HeaderParam("Authorization") String authorizationHeader);
	
	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@Path("/admin/realms/loadtest/users")
	String createUser(SsoUser user, @HeaderParam("Authorization") String authorizationHeader);

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@Path("/admin/realms/loadtest/users")
	List<SsoUser> getUsers(@QueryParam("briefRepresentation") boolean briefRepresentation, @QueryParam("max") long maxResults, @HeaderParam("Authorization") String authorizationHeader);

	@POST
	@Produces(MediaType.APPLICATION_JSON)
	@Path("/admin/realms/loadtest/clients-initial-access")
	SsoInitialAccessToken createInitialAccessToken(SsoInitialAccessTokenCreate config, @HeaderParam("Authorization") String authorizationHeader);

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@Path("/admin/serverinfo")
	ServerInfoRepresentation getServerInfo(@HeaderParam("Authorization") String authorizationHeader);
}
