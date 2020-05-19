package it.inforge.alfresco.repo.security.authentication.keycloak;

import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.namespace.QName;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Configuration of the keycloak authentication component
 *
 * @author  Francesco Milesi
 * @since   1.0
 */
public class KeycloakConfig {

    /**
     * keycloak url
     */
    private String url;
    /**
     * realms a user can be authenticated against
     */
    private List<String> realms;
    /**
     * keycloak client application used for the authentication
     */
    private String authenticationClient;
    /**
     * username used with permission to perform the authentication and synchronization calls
     * to the authentication client application
     */
    private String adminClient;
    /**
     * secret used by adminClient to perform the authentication and synchronization calls
     * to the authentication client application
     */
    private String adminSecret;
    /**
     * user data are imported into alfresco import in batches of this size
     */
    private int userListingBatchSize;
    /**
     * Alfresco's authentication zones
     */
    private Set<String> authZones;

    /**
     * name of the alfresco email contributor group (it should be GROUP_EMAIL_CONTRIBUTORS)
     */
    private String emailContributorsGroupName;
    /**
     * name of the keycloak attribute that makes a user/group member of the alfresco email contributors group, if true
     */
    private String emailContributorAttribute;
    /**
     * name of the keycloak attribute with the user's email aliases for mail sent to alfresco
     */
    private String emailAliasAttribute;

    /**
     * fully qualified name of the Alfresco's aspect added to nodes of users imported from keycloak.
     * It sould have at least a property woth local name keycloak-realm
     */
    private QName userAspect;
    private ServiceRegistry serviceRegistry;

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public List<String> getRealms() {
        return realms;
    }

    public void setRealms(String realms) {
        if(!Utils.isEmpty(realms))
            this.realms = Arrays.asList(realms.split(","));
    }

    public String getAuthenticationClient() {
        return authenticationClient;
    }

    public void setAuthenticationClient(String authenticationClient) {
        this.authenticationClient = authenticationClient;
    }

    public String getAdminClient() {
        return adminClient;
    }

    public void setAdminClient(String adminClient) {
        this.adminClient = adminClient;
    }

    public String getAdminSecret() {
        return adminSecret;
    }

    public void setAdminSecret(String adminSecret) {
        this.adminSecret = adminSecret;
    }

    public int getUserListingBatchSize() {
        return userListingBatchSize;
    }

    public void setUserListingBatchSize(int userListingBatchSize) {
        this.userListingBatchSize = userListingBatchSize;
    }

    public Set<String> getAuthZones() {
        return authZones;
    }

    public void setAuthZones(String authZones) {
        this.authZones = new HashSet<>(Arrays.asList(authZones.split(",")));
    }

    public String getEmailContributorsGroupName() {
        return emailContributorsGroupName;
    }

    public void setEmailContributorsGroupName(String emailContributorsGroupName) {
        this.emailContributorsGroupName = emailContributorsGroupName;
    }

    public String getEmailContributorAttribute() {
        return emailContributorAttribute;
    }

    public void setEmailContributorAttribute(String emailContributorAttribute) {
        this.emailContributorAttribute = emailContributorAttribute;
    }

    public String getEmailAliasAttribute() {
        return emailAliasAttribute;
    }

    public void setEmailAliasAttribute(String emailAliasAttribute) {
        this.emailAliasAttribute = emailAliasAttribute;
    }

    public QName getUserAspect() {
        return userAspect;
    }

    public void setUserAspect(String userAspect) {
        this.userAspect = QName.createQName(userAspect);
    }

    public ServiceRegistry getServiceRegistry() {
        return serviceRegistry;
    }

    public void setServiceRegistry(ServiceRegistry serviceRegistry) {
        this.serviceRegistry = serviceRegistry;
    }
}
