package it.inforge.alfresco.repo.security.authentication.keycloak;

import it.inforge.alfresco.repo.security.sync.keycloak.KeycloakUserRegistry;
import org.alfresco.repo.management.subsystems.ActivateableBean;
import org.alfresco.repo.security.authentication.AbstractAuthenticationComponent;
import org.alfresco.repo.security.authentication.AuthenticationException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.representations.AccessTokenResponse;
import org.springframework.beans.factory.InitializingBean;

/**
 * authentication component for the keycloak server, base on the {@link AbstractAuthenticationComponent}.
 * It authenticates a user against one of the available realms and set it as the current Alfresco user
 *
 * @author  Francesco Milesi
 * @see     AbstractAuthenticationComponent
 * @since   1.0
 */
public class KeycloakAuthenticationComponentImpl extends AbstractAuthenticationComponent implements InitializingBean, ActivateableBean {

    private final Log logger = LogFactory.getLog(getClass());

    private String id;
    private boolean active = true;
    private KeycloakConfig config;

    private KeycloakUserRegistry userRegistry;

    /**
     * @throws Exception if the mandatory configuration properties are missing
     */
    @Override
    public void afterPropertiesSet() throws Exception {
        if (Utils.isEmpty(config.getUrl()))
        {
            throw new IllegalStateException("The keycloak url must be set");
        }
        if (Utils.isEmpty(config.getAuthenticationClient()))
        {
            throw new IllegalStateException("The keycloak client must be set");
        }
        if (Utils.isEmpty(config.getRealms()))
        {
            throw new IllegalStateException("The keycloak realms must be set");
        }
    }

    /**
     *
     * authenticate a user and set it as the current Alfresco user
     *
     * @param userName the username of the authenticating user.
     * @param password the password of the authenticating user.
     * @throws AuthenticationException if the authentication was unsuccesfull
     */
    @Override
    protected void authenticateImpl(String userName, char[] password) {
        String realm = realmsAuthenticate(userName, password);
        if(realm == null) {
            if (logger.isDebugEnabled()) {
                logger.debug(String.format("User \"%s\" non present in any keycloak realm", userName));
            }
            throw new AuthenticationException("Invalid user name");
        }
        try {
            userRegistry.pushThreadLocalInstance(new KeycloakThreadInstance(config, realm, userName));
            setCurrentUser(userName);
        } finally {
            userRegistry.clearThreadLocalInstance();
        }
    }

    /**
     *
     * Tries to authenticate the user against one of the available keycloak realms
     *
     * @param userName the username of the authenticating user.
     * @param password the password of the authenticating user.
     * @return the name of the realm the user was authenticated against. null if the authentication was unsuccesfull
     */
    protected String realmsAuthenticate(String userName, char[] password) {
        if (!userName.equals(userName.trim()))
        {
            throw new AuthenticationException("Invalid user ID with leading or trailing whitespace");
        }
        for (String realm : config.getRealms()) {
            try {
                realmAuthenticate(realm, userName, password);
                if (logger.isDebugEnabled()) {
                    logger.debug(String.format("User \"%s\" logged into keycloak realm \"%s\"", userName, realm));
                }
                return realm;
            }
            catch (Exception e) {
                if (logger.isDebugEnabled()) {
                    logger.debug(String.format("User \"%s\" non present in keycloak realm \"%s\"", userName, realm));
                }
            }
        }
        return null;
    }

    /**
     *
     * Tries to authenticate the user against a keycloak realm via the the configured keycloak authentication client
     *
     * @param realm the name of the realm the user is being authenticated against.
     * @param userName the username of the authenticating user.
     * @param password the password of the authenticating user.
     */
    private void realmAuthenticate(String realm, String userName, char[] password) {
        Keycloak keycloak = null;
        try {
            String passwd = String.valueOf(password);
            keycloak = Keycloak.getInstance(config.getUrl(), realm, userName, passwd, config.getAuthenticationClient());
            AccessTokenResponse token = keycloak.tokenManager().getAccessToken();
        }
        finally {
            if(keycloak != null)
                keycloak.close();
        }
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    @Override
    public boolean isActive() {
        return active;
    }

    @Override
    protected boolean implementationAllowsGuestLogin() {
        return false;
    }

    /**
     * Set the unique name of this authentication component e.g. "keycloak1"
     *
     * @param id String
     */
    public void setId(String id)
    {
        this.id = id;
    }

    /**
     * Get the unique name of this authentication component e.g. "keycloak1";
     * @return the unique name of this authentication component
     */
    String getId()
    {
        return id;
    }

    /**
     * Set the configuration used by this component
     *
     * @param config the configuration used for authenticaton
     */
    public void setConfig(KeycloakConfig config) {
        this.config = config;
    }

    /**
     * Set the user registry used to synchronize alfresco with keycloak
     *
     * @param userRegistry used to synchronize alfresco with keycloak
     */
    public void setUserRegistry(KeycloakUserRegistry userRegistry) {
        this.userRegistry = userRegistry;
    }
}
