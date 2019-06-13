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

package org.jboss.as.test.compat.jpa.hibernate.transformer;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.runner.RunWith;

/**
 * Enable Hibernate bytecode transformer globally with system-property Hibernate51CompatibilityTransformer=true
 * and also enable it in application with jboss-deployment-structure.xml.
 * <p>
 * It should work, classes should be transformed only once (this test doesn't check it though)
 */
@RunWith(Arquillian.class)
@ServerSetup({
        AbstractVerifyHibernate51CompatibilityTestCase.EnableHibernateBytecodeTransformerSetupTask.class

})
public class VerifyHibernate51CompatibilityPropertyAndJDSEnabledTransformerTestCase
        extends AbstractVerifyHibernate51CompatibilityTestCase {

    @Deployment
    public static Archive<?> deploy() {

        EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class,
                VerifyHibernate51CompatibilityPropertyAndJDSEnabledTransformerTestCase.class.getName() + ".ear");
        ear.addAsLibraries(getLib());

        WebArchive war = getWar();
        war.addClasses(VerifyHibernate51CompatibilityPropertyAndJDSEnabledTransformerTestCase.class);
        ear.addAsModule(war);

        ear.addAsManifestResource(VerifyHibernate51CompatibilityJDSEnabledTransformerTestCase.class.getPackage(),
                "jboss-deployment-structure.xml", "jboss-deployment-structure.xml");
        return ear;
    }
}
