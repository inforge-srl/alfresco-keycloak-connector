package it.inforge.alfresco.repo.security.authentication.keycloak;

import java.util.Collection;

final class Utils {

    public static final boolean isEmpty(String string) {
        return string == null || string.trim().length() < 1;
    }

    public static final boolean isEmpty(Collection collection) {
        return collection == null || collection.isEmpty();
    }

}
