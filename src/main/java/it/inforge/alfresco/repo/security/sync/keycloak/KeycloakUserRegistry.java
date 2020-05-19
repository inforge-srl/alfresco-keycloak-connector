package it.inforge.alfresco.repo.security.sync.keycloak;

import it.inforge.alfresco.repo.security.authentication.keycloak.KeycloakConfig;
import it.inforge.alfresco.repo.security.authentication.keycloak.KeycloakThreadInstance;
import org.alfresco.repo.management.subsystems.ActivateableBean;
import org.alfresco.repo.security.authentication.AuthenticationDiagnostic;
import org.alfresco.repo.security.authentication.AuthenticationException;
import org.alfresco.repo.security.sync.NodeDescription;
import org.alfresco.repo.security.sync.UserRegistry;
import org.alfresco.repo.security.sync.ldap.LDAPNameResolver;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.namespace.QName;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.InitializingBean;

import java.util.*;

public class KeycloakUserRegistry implements UserRegistry, LDAPNameResolver, InitializingBean, ActivateableBean {

    private final Log logger = LogFactory.getLog(getClass());

    private ThreadLocal<KeycloakThreadInstance> keycloakInstanceThreadLocal = new ThreadLocal<>();

    private ServiceRegistry serviceRegistry;
    private KeycloakConfig config;

    private boolean active;

    /** The person attribute mapping. */
    private Map<String, String> personAttributeMapping;

    private Map<QName, String> personMappedPropertyMap;

    /** The person attribute defaults. */
    private Map<String, String> personAttributeDefaults = Collections.emptyMap();

    /** The group attribute mapping. */
    private Map<String, String> groupAttributeMapping;

    private Map<QName, String> groupMappedPropertyMap;

    @Override
    public void afterPropertiesSet() throws Exception {
        personMappedPropertyMap = new HashMap<>();
        for (Map.Entry<String, String> entry : personAttributeMapping.entrySet()) {
            QName qName = QName.createQName(entry.getKey(), serviceRegistry.getNamespaceService());
            personMappedPropertyMap.put(qName, entry.getValue());
        }
    }

    public void pushThreadLocalInstance() {
        logger.debug("Pushing ThreadLocal scheduled context");
        KeycloakThreadInstance keycloakThreadInstance = new KeycloakThreadInstance(config);
        keycloakInstanceThreadLocal.set(keycloakThreadInstance);
    }

    public void pushThreadLocalInstance(KeycloakThreadInstance keycloakThreadInstance) {
        logger.debug("Pushing ThreadLocal login context");
        this.keycloakInstanceThreadLocal.set(keycloakThreadInstance);
    }

    public void clearThreadLocalInstance() {
        logger.debug("Clearing ThreadLocal context context");
        KeycloakThreadInstance keycloakThreadInstance = this.keycloakInstanceThreadLocal.get();
        if(keycloakThreadInstance != null) { // in case there was an exception before finishing push (when the real sync happens)
            this.keycloakInstanceThreadLocal.get().dispose(logger);
            this.keycloakInstanceThreadLocal.remove();
        }
    }

    private KeycloakThreadInstance getKeycloakThreadInstance() {
        KeycloakThreadInstance keycloakThreadInstance = keycloakInstanceThreadLocal.get();
        return keycloakThreadInstance;
    }

    @Override
    public Collection<NodeDescription> getPersons(Date modifiedSince) {
        if(getKeycloakThreadInstance() == null) return Collections.emptyList();
        List persons = getKeycloakThreadInstance().getUsers();
        return persons;
    }

    @Override
    public Collection<NodeDescription> getGroups(Date modifiedSince) {
        if(getKeycloakThreadInstance() == null) return Collections.emptyList();
        List groups = getKeycloakThreadInstance().getGroups();
        return groups;
    }

    @Override
    public Collection<String> getPersonNames() {
        if(getKeycloakThreadInstance() == null) return Collections.emptyList();
        List persons = getKeycloakThreadInstance().getPersonNames();
        return persons;
    }

    @Override
    public Collection<String> getGroupNames() {
        if(getKeycloakThreadInstance() == null) return Collections.emptyList();
        List<String> groups = getKeycloakThreadInstance().getGroupNames();
        return groups;
    }

    @Override
    public Set<QName> getPersonMappedProperties() {
        if(getKeycloakThreadInstance() == null) return Collections.emptySet();
        return personMappedPropertyMap.keySet();
    }

    @Override
    public String resolveDistinguishedName(String userId, AuthenticationDiagnostic diagnostic) throws AuthenticationException {
        return null;
    }

    public void setPersonAttributeMapping(Map<String, String> personAttributeMapping) {
        this.personAttributeMapping = personAttributeMapping;
    }

    public void setGroupAttributeMapping(Map<String, String> groupAttributeMapping) {
        this.groupAttributeMapping = groupAttributeMapping;
    }

    @Override
    public boolean isActive() {
        boolean active = this.active && getKeycloakThreadInstance() != null;
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public void setServiceRegistry(ServiceRegistry serviceRegistry) {
        this.serviceRegistry = serviceRegistry;
    }

    public void setConfig(KeycloakConfig config) {
        this.config = config;
    }
}
