/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2019, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.integration.ejb.stateful.passivation.store;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.test.integration.ejb.stateful.passivation.Bean;
import org.jboss.as.test.integration.ejb.stateful.passivation.PassivationEnabledBean;
import org.jboss.as.test.integration.ejb.stateful.passivation.PassivationTestCase;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.naming.InitialContext;

/**
 * Test configures two passivation stores per deployment and calls the passivation bean. The call should succeed.
 * Test for [ JBEAP-16144 ].
 *
 * @author Daniel Cihak
 */
@RunWith(Arquillian.class)
@ServerSetup(TwoPassivationStoresServerSetupTask.class)
public class TwoPassivationStoresTestCase {

    private static final String DEPLOYMENT = "DEPLOYMENT";

    @ArquillianResource
    private InitialContext ctx;

    @Deployment
    public static Archive<?> deploy() {
        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, DEPLOYMENT + ".jar");
        jar.addPackage(PassivationTestCase.class.getPackage());
        jar.addClasses(DifferentCachePassivationBean.class, TwoPassivationStoresServerSetupTask.class);
        jar.addAsManifestResource(new StringAsset("Dependencies: org.jboss.as.controller-client, org.jboss.dmr \n"), "MANIFEST.MF");
        jar.addAsManifestResource(PassivationTestCase.class.getPackage(), "persistence.xml", "persistence.xml");
        return jar;
    }

    @Test
    public void testTwoPassivationStores() throws Exception {
        try (Bean bean = (Bean) ctx.lookup("java:module/" + PassivationEnabledBean.class.getSimpleName() + "!" + Bean.class.getName())) {
            bean.doNothing();

            try (Bean differentCacheBean1 = (Bean) ctx.lookup("java:module/" + DifferentCachePassivationBean.class.getSimpleName() + "!" + Bean.class.getName())) {
                differentCacheBean1.doNothing();

                // Create a 2nd set of beans, forcing the first set to passivate
                try (Bean bean2 = (Bean) ctx.lookup("java:module/" + PassivationEnabledBean.class.getSimpleName() + "!" + Bean.class.getName())) {
                    bean2.doNothing();

                    try (Bean differentCacheBean2 = (Bean) ctx.lookup("java:module/" + DifferentCachePassivationBean.class.getSimpleName() + "!" + Bean.class.getName())) {
                        differentCacheBean2.doNothing();

                        Assert.assertTrue("(Annotation based) Stateful bean marked as passivation enabled was not passivated", bean.wasPassivated());
                        Assert.assertTrue("(Annotation based) Stateful bean marked as passivation enabled was not activated", bean.wasActivated());

                        Assert.assertTrue("(Deployment descriptor based) Stateful bean marked as passivation enabled was not passivated", differentCacheBean1.wasPassivated());
                        Assert.assertTrue("(Deployment descriptor based) Stateful bean marked as passivation enabled was not activated", differentCacheBean1.wasActivated());
                    }
                }
            }
        }
    }
}
