package com.delivery.backoffice.admin.keycloak;

import java.util.List;
import java.util.Map;

import com.delivery.backoffice.shared.error.BusinessRuleException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

/**
 * Thin wrapper around the Keycloak Admin REST API.
 * Authenticates via the master-realm admin-cli client (password grant) and operates on the staff realm.
 */
@Component
public class KeycloakAdminClient {

    private static final Logger log = LoggerFactory.getLogger(KeycloakAdminClient.class);

    private final RestClient http;
    private final String adminUrl;
    private final String realm;
    private final String adminUsername;
    private final String adminPassword;

    public KeycloakAdminClient(
            @Value("${app.keycloak.admin-url}") String adminUrl,
            @Value("${app.keycloak.staff-realm}") String realm,
            @Value("${app.keycloak.admin-username}") String adminUsername,
            @Value("${app.keycloak.admin-password}") String adminPassword) {
        this.adminUrl = adminUrl;
        this.realm = realm;
        this.adminUsername = adminUsername;
        this.adminPassword = adminPassword;
        this.http = RestClient.create();
    }

    /**
     * Creates a user in Keycloak, sets their password, assigns a realm role, and returns their UUID.
     */
    public String createUser(String username, String email, String firstName, String lastName,
                              String password, String roleName) {
        String token = fetchAdminToken();
        String userId = createKeycloakUser(token, username, email, firstName, lastName);
        setPassword(token, userId, password);
        assignRealmRole(token, userId, roleName);
        log.info("Created Keycloak user '{}' with role '{}'", username, roleName);
        return userId;
    }

    @SuppressWarnings("unchecked")
    private String fetchAdminToken() {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "password");
        form.add("client_id", "admin-cli");
        form.add("username", adminUsername);
        form.add("password", adminPassword);

        Map<String, Object> response = (Map<String, Object>) http.post()
                .uri(adminUrl + "/realms/master/protocol/openid-connect/token")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .body(Map.class);
        return (String) response.get("access_token");
    }

    private String createKeycloakUser(String token, String username, String email,
                                       String firstName, String lastName) {
        Map<String, Object> body = Map.of(
                "username", username,
                "email", email,
                "firstName", firstName,
                "lastName", lastName,
                "enabled", true,
                "emailVerified", true
        );

        var response = http.post()
                .uri(adminUrl + "/admin/realms/" + realm + "/users")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .onStatus(status -> status == HttpStatus.CONFLICT,
                        (req, res) -> {
                            throw new BusinessRuleException("USER_EXISTS",
                                    "A user with that username or email already exists.");
                        })
                .toBodilessEntity();

        String location = response.getHeaders().getFirst("Location");
        if (location == null) {
            throw new IllegalStateException("Keycloak did not return a Location header after user creation.");
        }
        return location.substring(location.lastIndexOf('/') + 1);
    }

    private void setPassword(String token, String userId, String password) {
        Map<String, Object> body = Map.of("type", "password", "value", password, "temporary", false);
        http.put()
                .uri(adminUrl + "/admin/realms/" + realm + "/users/" + userId + "/reset-password")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .toBodilessEntity();
    }

    @SuppressWarnings("unchecked")
    private void assignRealmRole(String token, String userId, String roleName) {
        Map<String, Object> role = (Map<String, Object>) http.get()
                .uri(adminUrl + "/admin/realms/" + realm + "/roles/" + roleName)
                .header("Authorization", "Bearer " + token)
                .retrieve()
                .body(Map.class);

        http.post()
                .uri(adminUrl + "/admin/realms/" + realm + "/users/" + userId + "/role-mappings/realm")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .body(List.of(role))
                .retrieve()
                .toBodilessEntity();
    }
}
