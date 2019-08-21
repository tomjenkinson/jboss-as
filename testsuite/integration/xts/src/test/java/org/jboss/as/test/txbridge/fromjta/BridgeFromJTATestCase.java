/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2018, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.jboss.as.test.txbridge.fromjta;

import javax.naming.InitialContext;
import javax.transaction.UserTransaction;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.test.txbridge.fromjta.service.FirstServiceAT;
import org.jboss.as.test.xts.util.DeploymentHelper;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * <p>
 * Simple set of starting JTA transaction and getting it bridged to the XTS-AT.
 * <p>
 * Test ported from https://github.com/jbosstm/quickstart repository.
 */
@RunWith(Arquillian.class)
public class BridgeFromJTATestCase {
    private static final Logger log = Logger.getLogger(BridgeFromJTATestCase.class);

    private static final String DEPLOYMENT = "fromjta-bridge";
    private static final String ManifestMF =
        "Manifest-Version: 1.0\nDependencies: org.jboss.xts\n";
    private static final String persistentXml =
        "<persistence>\n" +
        "    <persistence-unit name=\"first\">\n" +
        "        <jta-data-source>java:jboss/datasources/ExampleDS</jta-data-source>\n" +
        "        <properties>\n" +
        "            <property name=\"hibernate.hbm2ddl.auto\" value=\"create-drop\"/>\n" +
        "        </properties>\n" +
        "    </persistence-unit>\n" +
        "</persistence>";

    private UserTransaction ut;
    private FirstServiceAT firstClient;

    @Deployment(name = DEPLOYMENT)
    public static Archive<?> createDeployment() {
        final WebArchive archive = DeploymentHelper.getInstance().getWebArchiveWithPermissions("test")
            .addPackages(true, BridgeFromJTATestCase.class.getPackage())
            .addAsManifestResource(new StringAsset(ManifestMF), "MANIFEST.MF")
            .addAsWebInfResource(new StringAsset(persistentXml), "classes/META-INF/persistence.xml" );
        return archive;
    }

    @Before
    public void setupTest() throws Exception {
        ut = (UserTransaction) new InitialContext().lookup("java:comp/UserTransaction");
        firstClient = FirstClient.newInstance();
    }

    @After
    public void teardownTest() throws Exception {
        tryRollback(ut);
        try {
            ut.begin();
            firstClient.resetCounter();
            ut.commit();
        } finally {
            tryRollback(ut);
        }
    }

    /**
     * Test starts the JTA transaction while calling the 'incrementCounter' on the stub.
     * Expecting the interceptor bridges from JTA to WS-AT.
     * The commit of the JTA transaction should cause the commit of the WS-AT transaction as well.
     */
    @Test
    public void testCommit() throws Exception {
        ut.begin();
        firstClient.incrementCounter(1);
        ut.commit();

        // second JTA checks if the counter was really incremented
        ut.begin();
        int counter = firstClient.getCounter();
        ut.commit();

        Assert.assertEquals("Bridged JTA transaction should commit the WS-AT and the counter is expected to be incremented",
                1, counter);
    }

    /**
     * Test starts the JTA transaction while calling the 'incrementCounter' on the stub.
     * Expecting the interceptor bridges from JTA to WS-AT.
     * The rollback of the JTA transaction should cause the rollback of the WS-AT transaction as well.
     */
    @Test
    public void testRollback() throws Exception {
        ut.begin();
        firstClient.incrementCounter(1);
        ut.rollback();

        // second JTA checks if the counter was not incremented
        ut.begin();
        int counter = firstClient.getCounter();
        ut.commit();

        Assert.assertEquals("Asserting that the counters were *not* incremented successfully", 0, counter);
    }

    private void tryRollback(UserTransaction ut) {
        try {
            ut.rollback();
        } catch (Throwable th2) {
            log.trace("Cannot rollback transaction " + ut, th2);
        }
    }
}
