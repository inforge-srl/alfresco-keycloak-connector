package it.inforge.alfresco.repo.security.authentication.keycloak;

import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.namespace.QName;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class KeycloakConfig {

    private String url;
    private List<String> realms;
    private String authenticationClient;
    private String adminClient;
    private String adminSecret;
    private int userListingBatchSize;
    private Set<String> authZones;

    private String emailContributorsGroupName;
    private String emailContributorAttribute;
    private String emailAliasAttribute;

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
