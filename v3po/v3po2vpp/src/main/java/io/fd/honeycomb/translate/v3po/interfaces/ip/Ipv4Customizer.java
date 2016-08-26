/*
 * Copyright (c) 2016 Cisco and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.fd.honeycomb.translate.v3po.interfaces.ip;

import io.fd.honeycomb.translate.spi.write.WriterCustomizer;
import io.fd.honeycomb.translate.v3po.util.FutureJVppCustomizer;
import io.fd.honeycomb.translate.v3po.util.NamingContext;
import io.fd.honeycomb.translate.write.WriteContext;
import io.fd.honeycomb.translate.write.WriteFailedException;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ip.rev140616.interfaces._interface.Ipv4;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.openvpp.jvpp.core.future.FutureJVppCore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Ipv4Customizer extends FutureJVppCustomizer implements WriterCustomizer<Ipv4> {

    private static final Logger LOG = LoggerFactory.getLogger(Ipv4Customizer.class);
    private final NamingContext interfaceContext;

    public Ipv4Customizer(final FutureJVppCore vppApi, final NamingContext interfaceContext) {
        super(vppApi);
        this.interfaceContext = interfaceContext;
    }

    @Override
    public void writeCurrentAttributes(@Nonnull final InstanceIdentifier<Ipv4> id,
                                       @Nonnull final Ipv4 dataAfter, @Nonnull final WriteContext writeContext)
        throws WriteFailedException {
        final String ifcName = id.firstKeyOf(Interface.class).getName();
        setIpv4(id, ifcName, dataAfter, writeContext);
    }

    @Override
    public void updateCurrentAttributes(@Nonnull final InstanceIdentifier<Ipv4> id,
                                        @Nonnull final Ipv4 dataBefore, @Nonnull final Ipv4 dataAfter,
                                        @Nonnull final WriteContext writeContext)
        throws WriteFailedException {
        final String ifcName = id.firstKeyOf(Interface.class).getName();

        // TODO handle update in a better way
        setIpv4(id, ifcName, dataAfter, writeContext);
    }

    @Override
    public void deleteCurrentAttributes(@Nonnull final InstanceIdentifier<Ipv4> id,
                                        @Nonnull final Ipv4 dataBefore, @Nonnull final WriteContext writeContext) {
        // TODO implement delete
    }

    private void setIpv4(final InstanceIdentifier<Ipv4> id, final String name, final Ipv4 ipv4,
                         final WriteContext writeContext)
        throws WriteFailedException {
        final int swIfc = interfaceContext.getIndex(name, writeContext.getMappingContext());

        LOG.warn("Ignoring Ipv4 leaf nodes (create/update is not supported)");
        // TODO add support for enabled leaf
        // TODO add support for forwarding leaf
        // TODO add support for mtu leaf
    }

}