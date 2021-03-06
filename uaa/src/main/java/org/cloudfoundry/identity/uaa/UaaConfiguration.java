package org.cloudfoundry.identity.uaa;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.validation.ConstraintViolation;
import javax.validation.Valid;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

import org.cloudfoundry.identity.uaa.UaaConfiguration.OAuth.Client;
import org.cloudfoundry.identity.uaa.config.CustomPropertyConstructor;
import org.hibernate.validator.constraints.URL;
import org.yaml.snakeyaml.TypeDescription;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.AbstractConstruct;
import org.yaml.snakeyaml.constructor.Construct;
import org.yaml.snakeyaml.nodes.Node;

/**
 * Java representation of the UAA YAML configuration for validation purposes.
 * 
 * @author Luke Taylor
 */
public class UaaConfiguration {
	public String name;
	@Pattern(regexp = "(default|postgresql|hsqldb|mysql|oracle)")
	public String platform;
	public String spring_profiles;
	@URL(message = "issuer.uri must be a valid URL")
	public String issuerUri;
	public boolean dump_requests;
	public boolean require_https;
	public boolean loginAddnew;
	@Valid
	public PasswordPolicy passwordPolicy;
	@Valid
	public Database database;
	@Valid
	public Logging logging;
	@Valid
	public Jwt jwt;
	@Valid
	public OAuth oauth;
	@Valid
	public Scim scim;
	@Valid
	public CloudController cloud_controller;

	public static class CloudController {
		@Valid
		public Database database;
	}

	public static class Database {
		public String driverClassName;
		@NotNull(message = "Database url is required")
		public String url;
		@NotNull(message = "Database username is required")
		public String username;
		@NotNull(message = "Database password is required")
		public String password;
	}

	public static class Logging {
		@NotNull(message = "'logging:' needs a 'config' file location")
		public String config;
	}

	public static class Jwt {
		@NotNull(message = "'jwt:' requires a 'token:' block")
		public Token token;

		public static class Token {
			@NotNull(message = "'token:' requires 'signing-key'")
			public String signingKey;
			public String verificationKey;
		}
	}

	public static class OAuth {
		@Valid
		public Client client;
		@Valid
		public Authorize authorize;
		@Valid
		public Map<String, OAuthClient> clients;

		public static class Client {
			public String override;
			public List<String> autoapprove;
		}

		public static class Authorize {
			@NotNull
			public boolean ssl;
		}
	}

	public static class OAuthClient {
		@NotNull(message = "Each oauth client requires an 'id'")
		public String id;
		public boolean override;
		public List<String> autoapprove;
		public String scope;
		public String secret;
		public String authorities;
		@NotNull
		public String grantTypes;
		public String accessTokenValidity;
		public String refreshTokenValidity;
		@URL(message = "'redirect-uri' must be a valid URL")
		public String redirectUri;
	}

	public static class Scim {
		public boolean userids_enabled;
		public boolean userOverride;
		public List<String> users;
		public String username_pattern;
	}

	public static class PasswordPolicy {
		public int requiredScore;
	}

	public static class UaaConfigConstructor extends CustomPropertyConstructor {

		public UaaConfigConstructor() {
			super(UaaConfiguration.class);
			TypeDescription oauthDesc = new TypeDescription(OAuth.class);
			oauthDesc.putMapPropertyType("clients", String.class, OAuthClient.class);
			addTypeDescription(oauthDesc);
			TypeDescription clientDesc = new TypeDescription(Client.class);
			clientDesc.putListPropertyType("autoapprove", String.class);
			addTypeDescription(clientDesc);
			TypeDescription oauthClientDesc = new TypeDescription(OAuthClient.class);
			oauthClientDesc.putListPropertyType("autoapprove", String.class);
			addTypeDescription(oauthClientDesc);
			addPropertyAlias("issuer.uri", UaaConfiguration.class, "issuerUri");
			addPropertyAlias("login.addnew", UaaConfiguration.class, "loginAddnew");
			addPropertyAlias("password-policy", UaaConfiguration.class, "passwordPolicy");
			addPropertyAlias("required-score", PasswordPolicy.class, "requiredScore");
			addPropertyAlias("signing-key", Jwt.Token.class, "signingKey");
			addPropertyAlias("verification-key", Jwt.Token.class, "verificationKey");
			addPropertyAlias("authorized-grant-types", OAuthClient.class, "grantTypes");
			addPropertyAlias("redirect-uri", OAuthClient.class, "redirectUri");
			addPropertyAlias("access-token-validity", OAuthClient.class, "accessTokenValidity");
			addPropertyAlias("refresh-token-validity", OAuthClient.class, "refreshTokenValidity");
			addPropertyAlias("user.override", Scim.class, "userOverride");
		}
		@Override
		protected Construct getConstructor(Node node) {
			if (List.class.isAssignableFrom(node.getType())) {
				return new AbstractConstruct() {
					@Override
					public Object construct(Node node) {
						return new ArrayList<Object>();
					}					
				};
			}
			return super.getConstructor(node);
		}
	}

	public static void main(String[] args) throws Exception {
		if (args.length != 1) {
			throw new IllegalArgumentException("YAML file required");
		}
		Yaml yaml = new Yaml(new UaaConfigConstructor());
		BufferedReader br = new BufferedReader(new FileReader(args[0]));
		UaaConfiguration config = (UaaConfiguration) yaml.load(br);
		ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
		Validator validator = factory.getValidator();
		Set<ConstraintViolation<UaaConfiguration>> errors = validator.validate(config);
		System.out.println(errors);
	}
}
