def root = new XmlParser().parse(inputFile)

/**
 * Fix logging: optionally remove CONSOLE handler and set a specific log file
 *
 */
def logHandlers = root.profile.subsystem.'root-logger'.handlers[0]
def consoleHandler = logHandlers.find{it.@name == 'CONSOLE'}
if (!project.properties['enableServerLoggingToConsole']) logHandlers.remove(consoleHandler)
def file = root.profile.subsystem.'periodic-rotating-file-handler'.file[0]
file.attributes()['path'] = serverLog

/**
 * Modify ApplicationRealm security-realm to use custom properties files
 *
 * <security-realm name="ApplicationRealm">
 *   <authentication>
 *     <local default-user="$local" allowed-users="*" skip-group-loading="true"/>
 *     <properties path="jbws-application-users.properties" relative-to="jboss.server.config.dir"/>
 *   </authentication>
 *   <authorization>
 *     <properties path="jbws-application-roles.properties" relative-to="jboss.server.config.dir"/>
 *   </authorization>
 * </security-realm>
 **/
def securityRealms = root.management.'security-realms'[0]
def appRealm = securityRealms.find{it.@name == 'ApplicationRealm'}
def realmAuthentication = appRealm.'authentication'[0];
def authenticationProps = realmAuthentication.'properties'
authenticationProps.@path = 'jbws-application-users.properties'
def realmAuthorization = appRealm.'authorization'[0];
def authorizationProps = realmAuthorization.'properties'
authorizationProps.@path = 'jbws-application-roles.properties'

/**
 * Add a JMS queue like this
 *
 *  <subsystem xmlns="urn:jboss:domain:messaging:2.0">
 *      <hornetq-server>
 *          <jms-destinations>
 *              <jms-queue name="testQueue">
 *                  <entry name="queue/test"/>
 *                  <entry name="java:jboss/exported/jms/queue/test"/>
 *              </jms-queue>
 *          </jms-destinations>
 *      </hornetq-server>
 *  </subsystem>
 **/
def hornetqServer = root.profile.subsystem.'hornetq-server'[0]
def jmsDestinations = hornetqServer.'jms-destinations'[0]
if (jmsDestinations == null) {
    jmsDestinations = hornetqServer.appendNode('jms-destinations');
}
def jmsQueue = jmsDestinations.appendNode('jms-queue', ['name':'testQueue'])
jmsQueue.appendNode('entry', ['name':'queue/test'])
jmsQueue.appendNode('entry', ['name':'java:jboss/exported/jms/queue/test'])

/**
 * Save the configuration to a new file
 */
def writer = new StringWriter()
writer.println('<?xml version="1.0" encoding="UTF-8"?>')
new XmlNodePrinter(new PrintWriter(writer)).print(root)
def f = new File(outputFile)
f.write(writer.toString())

/*
 * copy the preconfigured jbws-application-roles.properties and jbws-application-users.properties
 * files into the standalone/configure directory
 */
new AntBuilder().copy( file:srcUsersProperties,
    tofile:destUsersProperties)

new AntBuilder().copy( file:srcRolesProperties,
    tofile:destRolesProperties)
