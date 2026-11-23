package com.delivery.webshop.catalog.config;

import java.net.http.HttpClient;
import java.time.Duration;

import com.delivery.webshop.catalog.client.BackofficeClient;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.oauth2.client.AuthorizedClientServiceOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.InMemoryOAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProviderBuilder;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.client.OAuth2ClientHttpRequestInterceptor;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

/**
 * Service-to-service HTTP: the webshop calls the back office with ITS OWN identity (OAuth2 client
 * credentials against the Keycloak realm "staff"), never by forwarding the customer's token.
 * The interceptor fetches a token, caches it and renews it when it expires.
 */
@Configuration(proxyBeanMethods = false)
public class BackofficeClientConfig {

    static final String REGISTRATION_ID = "backoffice";

    /**
     * Tokens are cached per (registration, principal). Client credentials have no end user, so a fixed
     * application principal is used: one token shared by all requests, not one per logged-in customer.
     */
    private static final Authentication SERVICE_PRINCIPAL = new AnonymousAuthenticationToken(
            "webshop-service", "webshop-service", AuthorityUtils.createAuthorityList("ROLE_SERVICE"));

    @Bean
    OAuth2AuthorizedClientService authorizedClientService(ClientRegistrationRepository registrations) {
        return new InMemoryOAuth2AuthorizedClientService(registrations);
    }

    /** Works outside an HTTP request too (e.g. from a RabbitMQ listener or a scheduled job). */
    @Bean
    OAuth2AuthorizedClientManager authorizedClientManager(ClientRegistrationRepository registrations,
                                                          OAuth2AuthorizedClientService clientService) {
        AuthorizedClientServiceOAuth2AuthorizedClientManager manager =
                new AuthorizedClientServiceOAuth2AuthorizedClientManager(registrations, clientService);
        manager.setAuthorizedClientProvider(OAuth2AuthorizedClientProviderBuilder.builder().clientCredentials().build());
        return manager;
    }

    @Bean
    BackofficeClient backofficeClient(OAuth2AuthorizedClientManager authorizedClientManager,
                                      OAuth2AuthorizedClientService clientService,
                                      @Value("${app.backoffice.base-url}") String baseUrl,
                                      @Value("${app.backoffice.connect-timeout:2s}") Duration connectTimeout,
                                      @Value("${app.backoffice.read-timeout:5s}") Duration readTimeout) {
        OAuth2ClientHttpRequestInterceptor oauth2 = new OAuth2ClientHttpRequestInterceptor(authorizedClientManager);
        oauth2.setClientRegistrationIdResolver(request -> REGISTRATION_ID);
        oauth2.setPrincipalResolver(request -> SERVICE_PRINCIPAL);
        // On 401/403 from the back office, drop the cached token so the next call fetches a fresh one.
        oauth2.setAuthorizationFailureHandler(OAuth2ClientHttpRequestInterceptor.authorizationFailureHandler(clientService));

        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(
                HttpClient.newBuilder().connectTimeout(connectTimeout).build());
        requestFactory.setReadTimeout(readTimeout);

        RestClient restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(requestFactory)
                .requestInterceptor(oauth2)
                .build();
        return HttpServiceProxyFactory.builderFor(RestClientAdapter.create(restClient))
                .build()
                .createClient(BackofficeClient.class);
    }
}
