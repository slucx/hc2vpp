/*
 * Copyright (c) 2017 Cisco and/or its affiliates.
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

package io.fd.hc2vpp.l3.write.ipv4;

import static com.google.common.base.Preconditions.checkNotNull;

import io.fd.hc2vpp.common.translate.util.FutureJVppCustomizer;
import io.fd.hc2vpp.common.translate.util.NamingContext;
import io.fd.hc2vpp.l3.utils.ip.write.IpWriter;
import io.fd.honeycomb.translate.spi.write.ListWriterCustomizer;
import io.fd.honeycomb.translate.write.WriteContext;
import io.fd.honeycomb.translate.write.WriteFailedException;
import io.fd.vpp.jvpp.core.future.FutureJVppCore;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ip.rev140616.interfaces._interface.ipv4.Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ip.rev140616.interfaces._interface.ipv4.AddressKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ip.rev140616.interfaces._interface.ipv4.address.Subnet;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ip.rev140616.interfaces._interface.ipv4.address.subnet.Netmask;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ip.rev140616.interfaces._interface.ipv4.address.subnet.PrefixLength;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.DottedQuad;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Customizer for writing {@link Address}
 */
public class Ipv4AddressCustomizer extends FutureJVppCustomizer
        implements ListWriterCustomizer<Address, AddressKey>, IpWriter {

    private static final Logger LOG = LoggerFactory.getLogger(Ipv4AddressCustomizer.class);
    private final NamingContext interfaceContext;

    public Ipv4AddressCustomizer(@Nonnull final FutureJVppCore futureJVppCore,
                                 @Nonnull final NamingContext interfaceContext) {
        super(futureJVppCore);
        this.interfaceContext = checkNotNull(interfaceContext, "Interface context cannot be null");
    }

    @Override
    public void writeCurrentAttributes(InstanceIdentifier<Address> id, Address dataAfter, WriteContext writeContext)
            throws WriteFailedException {

        final String interfaceName = id.firstKeyOf(Interface.class).getName();
        final int interfaceIndex = interfaceContext.getIndex(interfaceName, writeContext.getMappingContext());
        // TODO - HC2VPP-92 - Add more descriptive exception handling after https://jira.fd.io/browse/VPP-649
        setAddress(true, id, interfaceName, interfaceIndex, dataAfter, writeContext);
    }

    @Override
    public void deleteCurrentAttributes(InstanceIdentifier<Address> id, Address dataBefore, WriteContext writeContext)
            throws WriteFailedException {

        final String interfaceName = id.firstKeyOf(Interface.class).getName();
        final int interfaceIndex = interfaceContext.getIndex(interfaceName, writeContext.getMappingContext());

        setAddress(false, id, interfaceName, interfaceIndex, dataBefore, writeContext);
    }

    private void setAddress(boolean add, final InstanceIdentifier<Address> id, final String interfaceName,
                            final int interfaceIndex, final Address address,
                            final WriteContext writeContext) throws WriteFailedException {

        Subnet subnet = address.getSubnet();

        if (subnet instanceof PrefixLength) {
            setPrefixLengthSubnet(add, id, interfaceName, interfaceIndex, address, (PrefixLength) subnet);
        } else if (subnet instanceof Netmask) {
            setNetmaskSubnet(add, id, interfaceName, interfaceIndex, address, (Netmask) subnet);
        } else {
            LOG.error("Unable to handle subnet of type {}", subnet);
            throw new WriteFailedException(id, "Unable to handle subnet of type " + subnet.getClass());
        }
    }

    private void setNetmaskSubnet(final boolean add, @Nonnull final InstanceIdentifier<Address> id,
                                  @Nonnull final String interfaceName, final int interfaceIndex,
                                  @Nonnull final Address address, @Nonnull final Netmask subnet)
            throws WriteFailedException {

        LOG.debug("Setting Subnet(subnet-mask) for interface: {}(id={}). Subnet: {}, address: {}",
                interfaceName, interfaceIndex, subnet, address);

        final DottedQuad netmask = subnet.getNetmask();
        checkNotNull(netmask, "netmask value should not be null");

        final byte subnetLength = getSubnetMaskLength(netmask.getValue());
        addDelAddress(getFutureJVpp(), add, id, interfaceIndex, address.getIp(), subnetLength);
    }

    private void setPrefixLengthSubnet(final boolean add, @Nonnull final InstanceIdentifier<Address> id,
                                       @Nonnull final String interfaceName, final int interfaceIndex,
                                       @Nonnull final Address address, @Nonnull final PrefixLength subnet)
            throws WriteFailedException {
        LOG.debug("Setting Subnet(prefix-length) for interface: {}(id={}). Subnet: {}, address: {}",
                interfaceName, interfaceIndex, subnet, address);

        addDelAddress(getFutureJVpp(), add, id, interfaceIndex, address.getIp(),
                subnet.getPrefixLength().byteValue());

        LOG.debug("Subnet(prefix-length) set successfully for interface: {}(id={}). Subnet: {}, address: {}",
                interfaceName, interfaceIndex, subnet, address);
    }
}
