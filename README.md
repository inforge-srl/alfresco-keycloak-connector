# alfresco-keycloak-connector


### Authentication (and synchronization)
The Alfresco's Keycloak connector authenticates users with a keycloak server (tested with 4.8.2.Final and Alrfesco 5.2.0 and 5.2.1). 
The first time a user authenticates, its data and groups are imported into the Alfresco repository.

###Keycloak configuration
keycloak should be configured as follows
1. create a client application named as the value of the property keycloak.authentication.client
2. 
3. 

###Connector configuration
The keycloak-authentication.properties properties are as follows

    keycloak.authentication.active=true
    # keycloak host to authenticate against: usually configured in alfresco-global.properties
    keycloak.authentication.url=
    # comma separated keycloak realms available for authentication. I first does not authenticate, use second and so on.
    # If none authenticates user, the authentication process fails: usually configured in alfresco-global.properties
    keycloak.authentication.realms=
    # default client used to authenticate against each realm.
    # Only one is configurable: that is all realms must have the same authentication client
    keycloak.authentication.client=admin-cli
    # default client used to sync users and groups.
    keycloak.admin.client=
    # secret of default client used to sync users and groups.
    keycloak.admin.secret=
    # batch size of users downloaded from keycloak.
    keycloak.user.listing.batch.size=20
    # comma separated authentication zones
    keycloak.authentication.zones=AUTH.EXT.keycloak1
    # name of the alfresco email contributors group
    keycloak.group.emailContributors=EMAIL_CONTRIBUTORS
    # name of the keycloak attribute that makes a user/group member of the alfresco email contributors group, if true
    keycloak.emailContributor.attribute=alfresco-email-contributor
    # name of the keycloak attribute with the user's email aliases for mail sent to alfresco
    keycloak.emailAlias.attribute=email-alias
    # user aspect to be added to keycloak users
    keycloak.user.aspect=
    keycloak.authentication.allowGuestLogin=false
    # Comma separated list of user names who should be considered administrators by default
    keycloak.authentication.defaultAdministratorUserNames=admin
    # Enable FTP authentication using LDAP
    keycloak.authentication.authenticateFTP=true
    # activates keycloak's user and group synchronization
    keycloak.synchronization.active=true

###alfresco-global.properties configuration example
A typical alfresco alfresco-global.properties should contain the following configuration properties

    authentication.chain=alfrescoNtlm1:alfrescoNtlm,keycloak1:keycloak
    keycloak.authentication.url=https://<your-keycloak-host>/auth
    keycloak.authentication.realms=your-keycloak-realm-name-1, ..., your-keycloak-realm-name-n
    keycloak.admin.client=<your keycloak admin client app name>
    keycloak.admin.secret=<your keycloak admin secret>
    keycloak.user.aspect={http://<your-model>/<your model-context-path>}<your-user-aspect-name>


##Scheduled synchronization

It is possible to schedule the synchronization of user and groups as in this example configuration

    <bean id="keycloakSyncTrigger" class="org.alfresco.util.CronTriggerBean">
        <property name="jobDetail">
            <bean id="keycloakPeopleJobDetail" class="org.springframework.scheduling.quartz.JobDetailBean">
                <property name="jobClass">
                    <value>it.inforge.alfresco.repo.security.sync.keycloak.KeycloakRegistrySynchronizerJob</value>
                </property>
                <property name="jobDataAsMap">
                    <map>
                        <entry key="keycloakSystem" value="keycloak1" />
                        <entry key="applicationContextManager">
                            <ref bean="Authentication" />
                        </entry>
                        <entry key="userRegistrySynchronizer">
                            <ref bean="userRegistrySynchronizer" />
                        </entry>
                        <entry key="synchronizeChangesOnly">
                            <value>${synchronization.synchronizeChangesOnly}</value>
                        </entry>
                    </map>
                </property>
            </bean>
        </property>
        <property name="cronExpression">
            <value>${synchronization.import.cron}</value>
        </property>
        <property name="scheduler">
            <ref bean="schedulerFactory" />
        </property>
        <property name="startDelayMinutes">
            <value>${system.cronJob.startDelayMinutes}</value>
        </property>
    </bean>


