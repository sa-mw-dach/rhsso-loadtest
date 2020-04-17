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

import com.redhat.sso.client.model.SsoUser;

@Path("/auth/admin/realms/loadtest")
@RegisterRestClient(configKey = "sso")
public interface SsoApiService {
	
	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@Path("/users")
	CompletionStage<String> createUser(SsoUser user, @HeaderParam("Authorization") String authorizationHeader);

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@Path("/users")
	List<SsoUser> getUsers(@QueryParam("briefRepresentation") boolean briefRepresentation, @QueryParam("max") long maxResults, @HeaderParam("Authorization") String authorizationHeader);
}
