package com.delivery.backoffice.shared.security;

/** Realm roles defined in the Keycloak "staff" realm (see delivery-platform-infra/keycloak/staff-realm.json). */
public final class Roles {

    /** Platform operator: creates restaurants and assigns staff. Not bound to a restaurant. */
    public static final String ADMIN = "ADMIN";
    /** Restaurant manager: menus, settings, reports and orders of their own restaurant. */
    public static final String MANAGER = "MANAGER";
    /** Kitchen / counter staff: works the order board of their own restaurant. */
    public static final String STAFF = "STAFF";
    /** Granted to the webshop service account only (client credentials). */
    public static final String CATALOG_READER = "CATALOG_READER";
    /** Delivery driver: picks up and delivers orders. */
    public static final String DRIVER = "DRIVER";

    private Roles() {
    }
}
