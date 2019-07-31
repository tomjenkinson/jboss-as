/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2015, Red Hat, Inc., and individual contributors
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

package org.wildfly.clustering.web.infinispan.session;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.time.Duration;

import org.kohsuke.MetaInfServices;
import org.wildfly.clustering.marshalling.Externalizer;
import org.wildfly.clustering.marshalling.spi.IndexSerializer;

/**
 * Optimize marshalling of last accessed timestamp.
 * @author Paul Ferraro
 */
@MetaInfServices(Externalizer.class)
public class SessionAccessMetaDataExternalizer implements Externalizer<SimpleSessionAccessMetaData> {

    @Override
    public void writeObject(ObjectOutput output, SimpleSessionAccessMetaData metaData) throws IOException {
        // If last access duration is a sub-second value, persist as 1 second; otherwise, session will still be considered "new"
        IndexSerializer.VARIABLE.writeInt(output, Math.max((int) metaData.getLastAccessedDuration().getSeconds(), 1));
    }

    @Override
    public SimpleSessionAccessMetaData readObject(ObjectInput input) throws IOException, ClassNotFoundException {
        SimpleSessionAccessMetaData metaData = new SimpleSessionAccessMetaData();
        metaData.setLastAccessedDuration(Duration.ofSeconds(IndexSerializer.VARIABLE.readInt(input)));
        return metaData;
    }

    @Override
    public Class<SimpleSessionAccessMetaData> getTargetClass() {
        return SimpleSessionAccessMetaData.class;
    }
}
