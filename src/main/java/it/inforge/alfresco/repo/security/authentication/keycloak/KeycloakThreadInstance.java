package it.inforge.alfresco.repo.security.authentication.keycloak;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.security.sync.NodeDescription;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.security.AuthorityService;
import org.alfresco.service.cmr.security.AuthorityType;
import org.alfresco.service.namespace.QName;
import org.alfresco.service.namespace.RegexQNamePattern;
import org.apache.commons.logging.Log;
import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.representations.idm.GroupRepresentation;
import org.keycloak.representations.idm.UserRepresentation;

import java.util.*;
import java.util.stream.Collectors;

/**
 * does all the synchronization work as the {@link ThreadLocal}
 * variable {@link it.inforge.alfresco.repo.security.sync.keycloak.KeycloakUserRegistry#keycloakInstanceThreadLocal}.
 * <p>
 * It is called by the {@link it.inforge.alfresco.repo.security.sync.keycloak.KeycloakUserRegistry}
 * after the authentication or by {@link it.inforge.alfresco.repo.security.sync.keycloak.KeycloakRegistrySynchronizerJob}
 * through the {@link org.alfresco.repo.security.sync.UserRegistrySynchronizer}
 * </p>
 *
 * @author  Francesco Milesi
 * @since   1.0
 */
public class KeycloakThreadInstance {

    private static final String PROP_IMPORT_REALM_NAME = "keycloak-realm";

    /**
     * connector's configuration
     */
    private KeycloakConfig config;
    /**
     * keycloak service
     */
    private Keycloak keycloak;
    /**
     * available realms
     */
    private String[] realms;

    /**
     * users to import
     */
    private List<NodeDescription> users = new ArrayList<>();
    /**
     * map of all the groups to import
     */
    private Map<String, Group> groupsMap = new HashMap<>();
    /**
     * nodeof the email contributors group
     */
    private NodeDescription emailContributors;

    public KeycloakThreadInstance(KeycloakConfig config) {
        this(config, config.getRealms().toArray(new String[config.getRealms().size()]));
        init();
    }

    public KeycloakThreadInstance(KeycloakConfig config, String realm, String username) {
        this(config, new String[] {realm});
        init(username);
    }

    private KeycloakThreadInstance(KeycloakConfig config, String[] realms) {
        this.config = config;
        this.realms = realms;
    }

    /**
     * get user's data in batches of {@link KeycloakConfig#getUserListingBatchSize()} for all configured realms
     */
    private void init() {
        initGroups();
        Keycloak keycloak = getKeycloak();
        for (String realm : realms) {
            Integer userCount = keycloak.realm(realm).users().count();
            int batchSize = config.getUserListingBatchSize();
            int firstResult = 0;
            while(firstResult < userCount) {
                List<UserRepresentation> userRepresentations = keycloak.realm(realm).users().list(firstResult, batchSize);
                for (UserRepresentation userRepresentation : userRepresentations) {
                    NodeDescription user = toUser(realm, userRepresentation.getId());
                    users.add(user);
                }
                firstResult = firstResult + batchSize;
            }
        }
    }

    /**
     * get the data and the groups of an authenticating  user
     *
     * @param username
     */
    private void init(String username) {
        initGroups();
        Keycloak keycloak = getKeycloak();
        List<UserRepresentation> userRepresentations = keycloak.realm(realms[0]).users().search(username);
        if(userRepresentations.isEmpty())
            return;
        NodeDescription user = toUser(realms[0], userRepresentations.get(0).getId());
        users.add(user);
    }

    /**
     * get the {@link NodeDescription} of a user and populate the group map and emailContributors node
     *
     * @param  realm
     * @param id keycloak user id
     * @return user {@link NodeDescription}
     */
    private NodeDescription toUser(String realm, String id) {
        Keycloak keycloak = getKeycloak();
        UserRepresentation userRepresentation = keycloak.realm(realm).users().get(id).toRepresentation();
        String username = userRepresentation.getUsername();
        NodeDescription userNodeDescription = toUserNodeDescription(userRepresentation, realm);
        UserResource userResource = keycloak.realm(realm).users().get(userRepresentation.getId());
        List<GroupRepresentation> groups = userResource.groups();
        for (GroupRepresentation group : groups) {
            String groupName = getGroupName(realm, group.getName());
            groupsMap.get(groupName).groupNodedescription.getChildAssociations().add(username);
        }
        boolean emailContributor = getBooleanAttribute(userRepresentation, config.getEmailContributorAttribute());
        if(emailContributor) {
            emailContributors.getChildAssociations().add(username);
        }
        return userNodeDescription;
    }

    private NodeDescription toUserNodeDescription(UserRepresentation userRepresentation, String realm) {
        NodeDescription userNodeDescription = new NodeDescription(userRepresentation.getId());
        userNodeDescription.setLastModified(new Date());
        userNodeDescription.getProperties().put(ContentModel.PROP_USERNAME, userRepresentation.getUsername());
        userNodeDescription.getProperties().put(ContentModel.PROP_EMAIL, userRepresentation.getEmail());
        userNodeDescription.getProperties().put(ContentModel.PROP_FIRSTNAME, userRepresentation.getFirstName());
        userNodeDescription.getProperties().put(ContentModel.PROP_LASTNAME, userRepresentation.getLastName());

        String emailAliasAttr = config.getEmailAliasAttribute();
        Map<String, List<String>> attributes = userRepresentation.getAttributes();
        if(attributes != null) {
            List<String> emailAliases = userRepresentation.getAttributes().get(emailAliasAttr);
            if(emailAliases != null && !emailAliases.isEmpty()) {
                QName emailAliasProp = QName.createQName(config.getUserAspect().getNamespaceURI(), emailAliasAttr);
                userNodeDescription.getProperties().put(emailAliasProp, new ArrayList<>(emailAliases));
            }
        }

        QName realmProp = QName.createQName(config.getUserAspect().getNamespaceURI(), PROP_IMPORT_REALM_NAME);
        userNodeDescription.getProperties().put(realmProp, realm);

        return userNodeDescription;
    }

    private Keycloak getKeycloak() {
        if(keycloak == null) {
            keycloak = KeycloakBuilder.builder()
                    .serverUrl(config.getUrl())
                    .realm("master")
                    .clientId(config.getAdminClient())
                    .clientSecret(config.getAdminSecret())
                    .grantType(OAuth2Constants.CLIENT_CREDENTIALS)
                    .build();
        }
        return keycloak;
    }

    public void dispose(Log logger) {
        if(keycloak != null) {
            try {
                keycloak.close();
            } catch (Exception e) {
                logger.error("Error closing keycloak admin client", e);
            }
        }
    }

    public List<NodeDescription> getUsers() {
        return users;
    }

    public List<String> getPersonNames() {
        List<String> names = users.stream()
                .map(nodeDescription -> (String) nodeDescription.getProperties()
                        .get(ContentModel.PROP_USERNAME)).collect(Collectors.toList());
        return names;
    }

    public List<NodeDescription> getGroups() {
        List<NodeDescription> groups = groupsMap.values()
                .stream().map(group -> group.groupNodedescription).collect(Collectors.toList());
        if(!emailContributors.getChildAssociations().isEmpty())
            groups.add(emailContributors);
        return groups;
    }

    public List<String> getGroupNames() {
        List<String> names = groupsMap.values().stream()
                .map(group -> (String) group.groupNodedescription.getProperties()
                        .get(ContentModel.PROP_AUTHORITY_NAME)).collect(Collectors.toList());
        if(!emailContributors.getChildAssociations().isEmpty())
            names.add((String) emailContributors.getProperties().get(ContentModel.PROP_AUTHORITY_NAME));
        return names;
    }

    private void initGroups() {
        emailContributors = getGroup(config.getEmailContributorsGroupName(), "", config.getEmailContributorsGroupName());
        String emailContribGroupName = String.format("GROUP_%s", config.getEmailContributorsGroupName());
        emailContributors.getProperties().put(ContentModel.PROP_AUTHORITY_NAME, emailContribGroupName);

        boolean doit = true;
        if(doit) {
            AuthenticationUtil.runAsSystem((AuthenticationUtil.RunAsWork<Set<Void>>) () -> {
                Set<String> nonImportedEmailContribGroups = config.getServiceRegistry().getAuthorityService().getContainedAuthorities(AuthorityType.GROUP, emailContribGroupName, true);
                nonImportedEmailContribGroups = filterNonKeycloakAuthorities(nonImportedEmailContribGroups);
                emailContributors.getChildAssociations().addAll(nonImportedEmailContribGroups);
                Set<String> nonImportedEmailContribUsers = config.getServiceRegistry().getAuthorityService().getContainedAuthorities(AuthorityType.USER, emailContribGroupName, true);
                nonImportedEmailContribUsers = filterNonKeycloakAuthorities(nonImportedEmailContribUsers);
                emailContributors.getChildAssociations().addAll(nonImportedEmailContribUsers);
                return null;
            });
        }

        for (String realm : realms) {
            Group group = new Group(realm);
            collectGroups(group);
        }
    }

    private Set<String> filterNonKeycloakAuthorities(Set<String> authorities) {
        ServiceRegistry serviceRegistry = config.getServiceRegistry();
        AuthorityService authorityService = serviceRegistry.getAuthorityService();
        NodeService nodeService = serviceRegistry.getNodeService();
        Set<String> filtered = new HashSet<>(authorities);
        for (String authority : authorities) {
            NodeRef nodeRef = authorityService.getAuthorityNodeRef(authority);
            List<ChildAssociationRef> assocs = nodeService.getParentAssocs(nodeRef, ContentModel.ASSOC_IN_ZONE, RegexQNamePattern.MATCH_ALL);
            for (ChildAssociationRef assoc : assocs) {
                NodeRef parentNodeRef = assoc.getParentRef();
                String name = (String) nodeService.getProperty(parentNodeRef, ContentModel.PROP_NAME);
                if(config.getAuthZones().contains((name)))
                    filtered.remove(authority);
            }
        }
        return filtered;
    }

    private void collectGroups(Group parentGroup) {
        Keycloak keycloak = getKeycloak();
        List<GroupRepresentation> groupRepresentations =
                parentGroup.groupRepresentation == null ?
                        keycloak.realm(parentGroup.realm).groups().groups():
                        parentGroup.groupRepresentation.getSubGroups();
        addAsEmailContributor(parentGroup);
        for (GroupRepresentation groupRepresentation : groupRepresentations) {
            GroupRepresentation fullGroupRepresentation = keycloak.realm(parentGroup.realm)
                    .groups().group(groupRepresentation.getId()).toRepresentation();
            String id = groupRepresentation.getId();
            String name = groupRepresentation.getName();
            NodeDescription nodeDescription = getGroup(id, parentGroup.realm, name);
            if(parentGroup.groupRepresentation != null) {
                String groupName = (String) nodeDescription.getProperties().get(ContentModel.PROP_AUTHORITY_NAME);
                parentGroup.groupNodedescription.getChildAssociations().add(groupName);
            }

            Group group = new Group(parentGroup.realm, fullGroupRepresentation, nodeDescription);
            groupsMap.put(group.getAlfrescoName(), group);
            collectGroups(group);
        }
    }

    private void addAsEmailContributor(Group group) {
        if(group.groupRepresentation == null)
            return;
        boolean emailContributor = getBooleanAttribute(group.groupRepresentation.getAttributes(), config.getEmailContributorAttribute());
        if(emailContributor) {
            emailContributors.getChildAssociations().add(group.getAlfrescoName());
        }
    }

    private NodeDescription getGroup(String id, String realm, String name) {
        NodeDescription nodeDescription = new NodeDescription(id);
        nodeDescription.setLastModified(new Date());
        nodeDescription.getProperties().put(ContentModel.PROP_AUTHORITY_NAME, getGroupName(realm, name));
        nodeDescription.getProperties().put(ContentModel.PROP_AUTHORITY_DISPLAY_NAME, name);

        QName propRealm = QName.createQName(config.getUserAspect().getNamespaceURI(), PROP_IMPORT_REALM_NAME);
        nodeDescription.getProperties().put(propRealm, realm);
        return nodeDescription;
    }

    private String getGroupName(String realm, String name) {
        String groupName = String.format("GROUP_%s-%s", realm, name);
        return groupName;
    }

    private boolean getBooleanAttribute(UserRepresentation userRepresentation, String attribute) {
        Map<String, List<String>> attributes = userRepresentation.getAttributes();
        return getBooleanAttribute(attributes, attribute);
    }

    private boolean getBooleanAttribute(Map<String, List<String>> attributes, String attribute) {
        if(attributes != null) {
            List<String> values = attributes.get(attribute);
            if(values != null && !values.isEmpty()) {
                return "true".equals(values.get(0));
            }
        }
        return false;
    }


    private class Group {
        String realm;
        NodeDescription groupNodedescription;
        GroupRepresentation groupRepresentation;

        public Group(String realm) {
            this.realm = realm;
        }

        public Group(String realm, GroupRepresentation groupRepresentation, NodeDescription groupNodedescription) {
            this.realm = realm;
            this.groupNodedescription = groupNodedescription;
            this.groupRepresentation = groupRepresentation;
        }

        String getAlfrescoName() {
            return getGroupName(realm, groupRepresentation.getName());
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Group group = (Group) o;

            if (!realm.equals(group.realm)) return false;
            return groupRepresentation.getName().equals(group.groupRepresentation.getName());
        }

        @Override
        public int hashCode() {
            int result = realm.hashCode();
            result = 31 * result + groupRepresentation.getName().hashCode();
            return result;
        }
    }

}





