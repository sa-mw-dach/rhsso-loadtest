package com.redhat.sso;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Scanner;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.servlet.ServletContext;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.RedirectionException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jsoup.Jsoup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.redhat.sso.client.SamlProtectedService;
import com.redhat.sso.client.SsoApiService;
import com.redhat.sso.client.SsoSamlService;
import com.redhat.sso.client.model.AuthnRequestForm;
import com.redhat.sso.client.model.LoginForm;
import com.redhat.sso.testdata.UserDataFactory;

/**
 * Resource to execute test requests for SAML2.
 * 
 *
 */
@Path("/api/saml/test")
public class SamlTestResource {

	private static final String EXECUTION = "execution";
	private static final String SESSION_CODE = "session_code";
	private static final String TAB_ID = "tab_id";
	private static final String CLIENT_ID = "client_id";
	private static final String AUTH_SESSION_ID = "AUTH_SESSION_ID";
	private static final Logger LOGGER = LoggerFactory.getLogger(SamlTestResource.class);
	
	@Inject
    @RestClient
    SamlProtectedService protectedResource;
	
	@Inject
    @RestClient
    SsoSamlService samlResource;
	
	@Inject
    @RestClient
    SsoApiService apiResource;
	
	@Inject
	TestDataResource testdataResource;
	
	@Inject
	UserDataFactory factory;
	
	@ConfigProperty(name = "saml.test.async")
	boolean async;
	
	/**
	 * 
	 * @param initialAccessToken Can be obtained from the load-test realm admin console tab client registration.
	 * @return
	 */
	@GET
	@Path("/register/client/{token}")
    @Produces(MediaType.TEXT_PLAIN)
    public String registerClient(@PathParam("token") String initialAccessToken, @Context ServletContext ctx) {
		InputStream resourceAsStream = ctx.getResourceAsStream("WEB-INF/client-entity-descriptor.xml");
		try(Scanner scanner = new Scanner(resourceAsStream, "UTF-8")) {
			String samlConfigXml = scanner.useDelimiter("\\A").next();
			LOGGER.debug("Register saml client with xml config {}", samlConfigXml);
			return samlResource.registerClient(samlConfigXml, "bearer " + initialAccessToken);
		}
	}
	
	
	
	
	/**
	 * Sends a request to the protected resource. Login with saml will be done automatically.
	 * @return
	 * @throws SAXException
	 * @throws IOException
	 * @throws ParserConfigurationException
	 */
	@GET
	@Path("/request/{username}")
    @Produces(MediaType.TEXT_PLAIN)
    public Response request(@PathParam("username") String username) throws SAXException, IOException, ParserConfigurationException {
		String htmlWithAuthnRequest = protectedResource.hello();
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("Sending SAML AuthnRequest");
			LOGGER.debug("AuthnRequest is {}", htmlWithAuthnRequest);
		}
		Response response = extractSamlRequest(htmlWithAuthnRequest)
			.map(req -> sendAuthnRequest(req, username))
			.orElseGet(() -> {
				LOGGER.debug("No users stored please load users first");
				return Response.status(Status.INTERNAL_SERVER_ERROR).build();
			});
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("SAML AuthnRequest successful");
		}
		return response;
	}
	
	/**
	 * Sends a request to the protected resource. Login with saml will be done automatically.
	 * @return
	 * @throws SAXException
	 * @throws IOException
	 * @throws ParserConfigurationException
	 */
	@GET
	@Path("/request")
    @Produces(MediaType.TEXT_PLAIN)
    public Response requestRandomUser() {
		Response response = testdataResource.getRandomUser().map(user -> {
			String username = user.getUsername();
			String htmlWithAuthnRequest = protectedResource.hello();
			LOGGER.debug("AuthnRequest is {}", htmlWithAuthnRequest);
			try {
				return extractSamlRequest(htmlWithAuthnRequest)
						.map(req -> sendAuthnRequest(req, username))
						.orElseGet(() -> {
							LOGGER.debug("No users stored please load users first");
							return Response.status(Status.INTERNAL_SERVER_ERROR).build();
						});
			} catch (SAXException | IOException | ParserConfigurationException e) {
				LOGGER.error("Error extracting saml request", e);
				return Response.status(Status.INTERNAL_SERVER_ERROR).build();
			}
		}).orElseGet(() -> Response.status(Status.INTERNAL_SERVER_ERROR.getStatusCode(), "No test user provided call /api/testdata/loadusers first").build());
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("SAML AuthnRequest successful");
		}
		return response;
	}
	
	private Response sendAuthnRequest(String samlRequest, String username) {
		CompletionStage<String> completionStage = samlResource.sendAuthnRequest(new AuthnRequestForm(samlRequest))
			.whenComplete((res, ex) -> whenAuthnRequestResponse(ex, username));
		waitForCompletionWhenSync(completionStage);
		return Response.ok("OK").build();
	}

	private void waitForCompletionWhenSync(CompletionStage<String> completionStage) {
		if (!async) {
			try {
				completionStage.toCompletableFuture().join();
			} catch (CompletionException e) {
				// Expected because of redirect (code != 2xx)
			}
		}
	}
	
	private void whenAuthnRequestResponse(Throwable e, String username) {
		if (e instanceof CompletionException) {
			if (e.getCause() instanceof RedirectionException) {
				RedirectionException redirectEx = (RedirectionException) e.getCause();
				URI location = redirectEx.getResponse().getLocation();
				LOGGER.debug("Redirecting to {}", location);
				requestLoginPage(getQueryMap(location), redirectEx.getResponse().getCookies().get(AUTH_SESSION_ID).toCookie(), username);
			} else {
				LOGGER.error("Expected redirect but was error", e.getCause());
			}
		} else {
			LOGGER.error("Expected redirect but was 2xx response");
		}
	}
	
	private void requestLoginPage(Map<String, List<String>> query, Cookie authSession, String username) {
		LOGGER.debug("Requesting login page");
		CompletionStage<String> completionStage = samlResource.requestLoginPage(query.get(CLIENT_ID).get(0), query.get(TAB_ID).get(0), authSession)
			.whenComplete((resp, ex) -> whenLoginPageResponse(resp, ex, authSession, username));
		waitForCompletionWhenSync(completionStage);
	}
	
	private void whenLoginPageResponse(String response, Throwable e, Cookie authSession, String username) {
		if (e == null) {
			LOGGER.debug("Login page response received");
			org.jsoup.nodes.Document loginForm = Jsoup.parse(new String(response.getBytes()));
			try {
				URI location = new URI(loginForm.getElementsByTag("FORM").attr("ACTION"));
				LOGGER.debug("Submitting form");
				sendLoginRequest(getQueryMap(location), authSession, username);
			} catch (URISyntaxException e1) {
				LOGGER.error("Error parsing URI from form action", e1);
			}
		} else {
			LOGGER.error("Login page request error", e);
		}
	}
	
	private void sendLoginRequest(Map<String, List<String>> query2, Cookie authSession, String username) {
		CompletionStage<String> completionStage = samlResource.sendLoginRequest(query2.get(CLIENT_ID).get(0), query2.get(TAB_ID).get(0),
				query2.get(SESSION_CODE).get(0), query2.get(EXECUTION).get(0), authSession,
				new LoginForm(username, factory.derivePasswordFromUsername(username))).whenComplete(this::whenLoginResponse);
		waitForCompletionWhenSync(completionStage);
	}
	
	private void whenLoginResponse(String response, Throwable e) {
		if (e != null) {
			LOGGER.error("Login error", e);
		} else {
			LOGGER.debug("Login successful");
		}
	}
	
	private Optional<String> extractSamlRequest(String xhtml) throws SAXException, IOException, ParserConfigurationException {
		DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
		builderFactory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, ""); 
		builderFactory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, ""); 
		DocumentBuilder builder = builderFactory.newDocumentBuilder();
		Document document = builder.parse(new ByteArrayInputStream(xhtml.getBytes()));
		NodeList inputElements = document.getElementsByTagName("INPUT");
		return extractSamlRequest(inputElements);
	}
	
	private Map<String, List<String>> getQueryMap(URI uri) {
		return Arrays.stream(uri.getQuery().split("&"))
				.collect(groupingBy(t -> t.split("=")[0], mapping( t ->
					t.substring(t.indexOf('=') + 1)
				, Collectors.toList())));
	}
	
	private Optional<String> extractSamlRequest(NodeList inputs) {
		for (int i = 0; i < inputs.getLength(); i++) {
			Node input = inputs.item(i);
			if (input.getAttributes().getNamedItem("NAME") != null && input.getAttributes().getNamedItem("NAME").getFirstChild().getNodeValue().equals("SAMLRequest")) {
				return Optional.ofNullable(input.getAttributes().getNamedItem("VALUE").getFirstChild().getNodeValue());
			}
		}
		return Optional.empty();
	}
	
}
