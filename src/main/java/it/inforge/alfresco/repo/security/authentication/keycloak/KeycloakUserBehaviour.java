package it.inforge.alfresco.repo.security.authentication.keycloak;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.node.NodeServicePolicies;
import org.alfresco.repo.policy.Behaviour;
import org.alfresco.repo.policy.JavaBehaviour;
import org.alfresco.repo.policy.PolicyComponent;
import org.alfresco.service.cmr.dictionary.AspectDefinition;
import org.alfresco.service.cmr.dictionary.DictionaryService;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class KeycloakUserBehaviour implements NodeServicePolicies.OnCreateNodePolicy {

    private final Log logger = LogFactory.getLog(getClass());

    // Dependencies
    private NodeService nodeService;
    private DictionaryService dictionaryService;
    private PolicyComponent policyComponent;

    // Behaviours
    private Behaviour onCreateNode;
    private KeycloakConfig config;


    public void init() {
        if (logger.isDebugEnabled()) logger.debug("Initializing rateable behaviors");
        // Create behaviours
        this.onCreateNode = new JavaBehaviour(this, "onCreateNode", Behaviour.NotificationFrequency.EVERY_EVENT);
        // Bind behaviours to node policies
        this.policyComponent.bindClassBehaviour(
                QName.createQName(NamespaceService.ALFRESCO_URI, "onCreateNode"), ContentModel.TYPE_PERSON, this.onCreateNode);
    }

    public void onCreateNode(ChildAssociationRef childAssocRef) {
        if (logger.isDebugEnabled()) logger.debug("Inside onCreateNode");
        NodeRef childNodeRef = childAssocRef.getChildRef();
        Map<QName, Serializable> propsMap = nodeService.getProperties(childNodeRef);
        AspectDefinition aspectDefinition = dictionaryService.getAspect(config.getUserAspect());

        Set<QName> userAspectPropNames = new HashSet<>(aspectDefinition.getProperties().keySet());
        Set<QName> propNames = new HashSet<>(propsMap.keySet());
        propNames.retainAll(userAspectPropNames);

        if(!propNames.isEmpty())
            nodeService.addAspect(childNodeRef, config.getUserAspect(), propsMap);
    }

    public void setConfig(KeycloakConfig config) {
        this.config = config;
    }

    public NodeService getNodeService() {
        return nodeService;
    }

    public void setDictionaryService(DictionaryService dictionaryService) {
        this.dictionaryService = dictionaryService;
    }

    public void setNodeService(NodeService nodeService) {
        this.nodeService = nodeService;
    }


    public PolicyComponent getPolicyComponent() {
        return policyComponent;
    }


    public void setPolicyComponent(PolicyComponent policyComponent) {
        this.policyComponent = policyComponent;
    }
}