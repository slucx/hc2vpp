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

package io.fd.hc2vpp.v3po.interfacesstate;

import static com.google.common.base.Preconditions.checkArgument;

import io.fd.hc2vpp.common.translate.util.ByteDataTranslator;
import io.fd.hc2vpp.common.translate.util.JvppReplyConsumer;
import io.fd.hc2vpp.v3po.interfacesstate.cache.InterfaceCacheDumpManager;
import io.fd.honeycomb.translate.read.ReadContext;
import io.fd.honeycomb.translate.read.ReadFailedException;
import io.fd.honeycomb.translate.util.RWUtils;
import io.fd.vpp.jvpp.core.dto.SwInterfaceDetails;
import java.math.BigInteger;
import java.util.Objects;
import java.util.stream.Collector;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.iana._if.type.rev140508.EthernetCsmacd;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.InterfaceType;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.Gauge64;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev170607.GreTunnel;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev170607.Loopback;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev170607.Tap;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev170607.VhostUser;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev170607.VxlanGpeTunnel;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev170607.VxlanTunnel;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public interface InterfaceDataTranslator extends ByteDataTranslator, JvppReplyConsumer {

    InterfaceDataTranslator INSTANCE = new InterfaceDataTranslator() {
    };

    Gauge64 vppLinkSpeed0 = new Gauge64(BigInteger.ZERO);
    Gauge64 vppLinkSpeed1 = new Gauge64(BigInteger.valueOf(10L * 1000000));
    Gauge64 vppLinkSpeed2 = new Gauge64(BigInteger.valueOf(100L * 1000000));
    Gauge64 vppLinkSpeed4 = new Gauge64(BigInteger.valueOf(1000L * 1000000));
    Gauge64 vppLinkSpeed8 = new Gauge64(BigInteger.valueOf(10000L * 1000000));
    Gauge64 vppLinkSpeed16 = new Gauge64(BigInteger.valueOf(40000L * 1000000));
    Gauge64 vppLinkSpeed32 = new Gauge64(BigInteger.valueOf(100000L * 1000000));

    int PHYSICAL_ADDRESS_LENGTH = 6;

    Collector<SwInterfaceDetails, ?, SwInterfaceDetails> SINGLE_ITEM_COLLECTOR =
            RWUtils.singleItemCollector();

    /**
     * Convert VPP's link speed bitmask to Yang type. 1 = 10M, 2 = 100M, 4 = 1G, 8 = 10G, 16 = 40G, 32 = 100G
     *
     * @param vppLinkSpeed Link speed in bitmask format from VPP.
     * @return Converted value from VPP link speed
     */
    default Gauge64 vppInterfaceSpeedToYang(byte vppLinkSpeed) {
        switch (vppLinkSpeed) {
            case 1:
                return vppLinkSpeed1;
            case 2:
                return vppLinkSpeed2;
            case 4:
                return vppLinkSpeed4;
            case 8:
                return vppLinkSpeed8;
            case 16:
                return vppLinkSpeed16;
            case 32:
                return vppLinkSpeed32;
            default:
                return vppLinkSpeed0;
        }
    }

    /**
     * Reads first 6 bytes of supplied byte array and converts to string as Yang dictates <p> Replace later with
     * https://git.opendaylight.org/gerrit/#/c/34869/10/model/ietf/ietf-type- util/src/main/
     * java/org/opendaylight/mdsal/model/ietf/util/AbstractIetfYangUtil.java
     *
     * @param vppPhysAddress byte array of bytes in big endian order, constructing the network IF physical address.
     * @return String like "aa:bb:cc:dd:ee:ff"
     * @throws NullPointerException     if vppPhysAddress is null
     * @throws IllegalArgumentException if vppPhysAddress.length < 6
     */
    default String vppPhysAddrToYang(@Nonnull final byte[] vppPhysAddress) {
        return vppPhysAddrToYang(vppPhysAddress, 0);
    }

    default String vppPhysAddrToYang(@Nonnull final byte[] vppPhysAddress, final int startIndex) {
        Objects.requireNonNull(vppPhysAddress, "Empty physical address bytes");
        final int endIndex = startIndex + PHYSICAL_ADDRESS_LENGTH;
        checkArgument(endIndex <= vppPhysAddress.length,
                "Invalid physical address size (%s) for given startIndex (%s), expected >= %s", vppPhysAddress.length,
                startIndex, endIndex);
        return printHexBinary(vppPhysAddress, startIndex, endIndex);
    }

    /**
     * VPP's interface index is counted from 0, whereas ietf-interface's if-index is from 1. This function converts from
     * VPP's interface index to YANG's interface index.
     *
     * @param vppIfIndex the sw interface index VPP reported.
     * @return VPP's interface index incremented by one
     */
    default int vppIfIndexToYang(int vppIfIndex) {
        return vppIfIndex + 1;
    }

    /**
     * This function does the opposite of what {@link #vppIfIndexToYang(int)} does.
     *
     * @param yangIfIndex if-index from ietf-interfaces.
     * @return VPP's representation of the if-index
     */
    default int yangIfIndexToVpp(int yangIfIndex) {
        checkArgument(yangIfIndex >= 1, "YANG if-index has invalid value %s", yangIfIndex);
        return yangIfIndex - 1;
    }

    /**
     * Determine interface type based on its VPP name (relying on VPP's interface naming conventions)
     *
     * @param interfaceName VPP generated interface name
     * @return Interface type
     */
    @Nonnull
    default Class<? extends InterfaceType> getInterfaceType(@Nonnull final String interfaceName) {
        if (interfaceName.startsWith("tap")) {
            return Tap.class;
        }

        if (interfaceName.startsWith("vxlan_gpe")) {
            return VxlanGpeTunnel.class;
        }

        if (interfaceName.startsWith("vxlan")) {
            return VxlanTunnel.class;
        }

        if (interfaceName.startsWith("gre")) {
            return GreTunnel.class;
        }

        if (interfaceName.startsWith("VirtualEthernet")) {
            return VhostUser.class;
        }

        if (interfaceName.startsWith("loop")) {
            return Loopback.class;
        }

        return EthernetCsmacd.class;
    }

    /**
     * Check interface type. Uses interface details from VPP to determine.
     */
    default boolean isInterfaceOfType(@Nonnull final InterfaceCacheDumpManager dumpManager,
                                      @Nonnull final InstanceIdentifier<?> id,
                                      @Nonnull final ReadContext ctx,
                                      @Nonnull final Class<? extends InterfaceType> ifcType)
            throws ReadFailedException {
        final String name = id.firstKeyOf(Interface.class).getName();
        final SwInterfaceDetails vppInterfaceDetails = dumpManager.getInterfaceDetail(id, ctx, name);

        return isInterfaceOfType(ifcType, vppInterfaceDetails);
    }

    default boolean isInterfaceOfType(final Class<? extends InterfaceType> ifcType,
                                      final SwInterfaceDetails cachedDetails) {
        return ifcType.equals(getInterfaceType(toString(cachedDetails.interfaceName)));
    }

    /**
     * Checks whether provided {@link SwInterfaceDetails} is detail of sub-interface<br> <li>subId == unique number of
     * sub-interface within set of sub-interfaces of single interface <li>swIfIndex == unique index of
     * interface/sub-interface within all interfaces <li>supSwIfIndex == unique index of parent interface <li>in case of
     * interface , swIfIndex value equals supSwIfIndex <li>in case of subinterface, supSwIfIndex equals index of parent
     * interface, swIfIndex is index of subinterface itselt
     */
    default boolean isSubInterface(@Nonnull final SwInterfaceDetails elt) {
        //cant check by subId != 0, because you can pick 0 as value
        return elt.supSwIfIndex != elt.swIfIndex;
    }

    /**
     * Checks whether provided {@link SwInterfaceDetails} is detail of interface<br> <li>subId == unique number of
     * subinterface within set of subinterfaces of single interface <li>swIfIndex == unique index of
     * interface/subinterface within all interfaces <li>supSwIfIndex == unique index of parent interface <li>in case of
     * interface , swIfIndex value equals supSwIfIndex <li>in case of subinterface, supSwIfIndex equals index of parent
     * interface, swIfIndex is index of subinterface itselt
     */
    default boolean isRegularInterface(@Nonnull final SwInterfaceDetails elt) {
        return !isSubInterface(elt);
    }
}
