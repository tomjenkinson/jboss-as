package org.wildfly.extension.messaging.activemq;

import java.io.IOException;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.as.controller.capability.registry.CapabilityScope;
import org.jboss.as.controller.capability.registry.RegistrationPoint;
import org.jboss.as.controller.capability.registry.RuntimeCapabilityRegistration;
import org.jboss.as.controller.capability.registry.RuntimeCapabilityRegistry;
import org.jboss.as.controller.extension.ExtensionRegistry;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.controller.security.CredentialReference;
import org.jboss.as.subsystem.test.AbstractSubsystemBaseTest;
import org.jboss.as.subsystem.test.AdditionalInitialization;
import org.jboss.as.subsystem.test.KernelServices;
import org.jboss.msc.service.ServiceController;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.wildfly.clustering.jgroups.spi.JGroupsDefaultRequirement;
import org.wildfly.clustering.spi.ClusteringDefaultRequirement;
import org.wildfly.clustering.spi.ClusteringRequirement;
import org.wildfly.security.auth.server.SecurityDomain;

public class SystemPropertiesAttributesTest extends AbstractSubsystemBaseTest {

    public SystemPropertiesAttributesTest() {
        super(MessagingExtension.SUBSYSTEM_NAME, new MessagingExtension());
    }

    @Test
    public void testJournalFileOpenTimeout() throws Exception {
        ActiveMQServerService service = bootServerService();
        Assert.assertEquals(5, service.getConfiguration().getJournalFileOpenTimeout());

        System.setProperty("brokerconfig.journalFileOpenTimeout", "7");
        service = bootServerService();
        Assert.assertEquals(7, service.getConfiguration().getJournalFileOpenTimeout());
    }

    private ActiveMQServerService bootServerService() throws Exception {
        KernelServices servicesA = createKernelServicesBuilder(createAdditionalInitialization())
                .setSubsystemXml(readResource(getSubsystemXml()))
                .build();

        try {
            Assert.assertTrue("Subsystem boot failed!", servicesA.isSuccessfulBoot());

            ServiceController<?> controller = servicesA.getContainer().getService(MessagingServices.getActiveMQServiceName("default"));
            return (ActiveMQServerService) controller.getService();
        } finally {
            servicesA.shutdown();
        }
    }

    @Override
    protected AdditionalInitialization createAdditionalInitialization() {
        return new AdditionalInitialization() {
            @Override
            protected void initializeExtraSubystemsAndModel(ExtensionRegistry extensionRegistry, Resource rootResource, ManagementResourceRegistration rootRegistration, RuntimeCapabilityRegistry capabilityRegistry) {
                super.initializeExtraSubystemsAndModel(extensionRegistry, rootResource, rootRegistration, capabilityRegistry);
                registerCapabilities(capabilityRegistry, ClusteringRequirement.COMMAND_DISPATCHER_FACTORY.resolve("ee"),
                        ClusteringDefaultRequirement.COMMAND_DISPATCHER_FACTORY.getName(),
                        JGroupsDefaultRequirement.CHANNEL_FACTORY.getName(),
                        Capabilities.ELYTRON_DOMAIN_CAPABILITY,
                        CredentialReference.CREDENTIAL_STORE_CAPABILITY + ".cs1",
                        Capabilities.DATA_SOURCE_CAPABILITY + ".fooDS");

                RuntimeCapability<Void> domainCapability = RuntimeCapability.Builder
                        .of(Capabilities.ELYTRON_DOMAIN_CAPABILITY + ".elytronDomain")
                        .setServiceType(SecurityDomain.class)
                        .build();
                capabilityRegistry.registerCapability(new RuntimeCapabilityRegistration(domainCapability, CapabilityScope.GLOBAL,
                        new RegistrationPoint(PathAddress.EMPTY_ADDRESS, null)));
            }
        };
    }

    @Override
    protected String getSubsystemXml() throws IOException {
        return "subsystem_4_0_simple.xml";
    }


    @Test
    @Ignore
    @Override
    public void testSubsystem() throws Exception {
        // disable
    }

    @Test
    @Ignore
    @Override
    public void testSchema() throws Exception {
        // disable
    }

}
