package it.inforge.alfresco.repo.security.sync.keycloak;

import org.alfresco.repo.management.subsystems.ChildApplicationContextManager;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.security.sync.UserRegistrySynchronizer;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.context.ApplicationContext;

import java.util.Collection;

/**
 * quartz job that synchronizes keycloak users and group to alfresco.
 *
 * @author  Francesco Milesi
 * @since   1.0
 */
public class KeycloakRegistrySynchronizerJob implements Job
{
    private final Log logger = LogFactory.getLog(getClass());

    public void execute(JobExecutionContext executionContext) throws JobExecutionException
    {
        JobDataMap jobDataMap = executionContext.getJobDetail().getJobDataMap();
        final KeycloakUserRegistry userRegistry = getUserRegistry(jobDataMap);
        logger.info("Starting keycloak importer");
        if(userRegistry == null) {
            logger.info("Cannot start keycloak importer because userRegistry is null");
            return;
        }
        final UserRegistrySynchronizer userRegistrySynchronizer = (UserRegistrySynchronizer) executionContext
                .getJobDetail().getJobDataMap().get("userRegistrySynchronizer");
        final String synchronizeChangesOnly = (String) executionContext.getJobDetail().getJobDataMap().get("synchronizeChangesOnly");
        AuthenticationUtil.runAs(() -> {
            try {
                if(userRegistry != null)
                    userRegistry.pushThreadLocalInstance();
                userRegistrySynchronizer.synchronize(synchronizeChangesOnly == null || !Boolean.parseBoolean(synchronizeChangesOnly), true);
                return null;
            } finally {
                if(userRegistry != null)
                    userRegistry.clearThreadLocalInstance();
            }
        }, AuthenticationUtil.getSystemUserName());
    }

    private KeycloakUserRegistry getUserRegistry(JobDataMap jobDataMap) {
        final String keycloakSystem = (String) jobDataMap.get("keycloakSystem");
        logger.info(String.format("Keycloak importer system is [%s]", keycloakSystem));
        KeycloakUserRegistry userRegistry = null;
        final ChildApplicationContextManager applicationContextManager =
                (ChildApplicationContextManager) jobDataMap.get("applicationContextManager");
        Collection<String> instanceIds = applicationContextManager.getInstanceIds();
        if(instanceIds.contains(keycloakSystem)) {
            logger.error(String.format("Keycloak importer system [%s] FOUND", keycloakSystem));
            ApplicationContext context = applicationContextManager.getApplicationContext(keycloakSystem);
            userRegistry = (KeycloakUserRegistry) context.getBean("userRegistry");
        }
        else {
            logger.error(String.format("Keycloak importer system [%s] NOT FOUND", keycloakSystem));
        }
        return userRegistry;
    }

}
