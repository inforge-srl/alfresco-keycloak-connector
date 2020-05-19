# alfresco-keycloak-connector
Alfresco authentication through Keycloak


The Alfresco's Keycloak connector authenticates users with a keycloak server (tested with 4.8.2.Final). 
When a user authenticates the first time its anagrafic data and groups are imported into the Alfresco repository


The alfresco alfresco-global.properties should contain the following configuration

authentication.chain=alfrescoNtlm1:alfrescoNtlm,keycloak1:keycloak
keycloak.authentication.url=https://<your-keycloak-host>/auth
keycloak.authentication.realms=your-keycloak-realm-1,your-keycloak-realm-2,your-keycloak-realm-3,.....
keycloak.admin.client=<your keycloak admin client app name>
keycloak.admin.secret=492b6738-7c15-4896-b2d9-23202d1802a6
keycloak.user.aspect={http://<your-model>/<your model-context-path>}<your-user-aspect-name>

keycloak-realm



