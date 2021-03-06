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

package io.fd.hc2vpp.lisp.translate.read;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableList;
import io.fd.hc2vpp.common.test.read.ListReaderCustomizerTest;
import io.fd.hc2vpp.common.translate.util.NamingContext;
import io.fd.honeycomb.translate.read.ReadFailedException;
import io.fd.honeycomb.translate.spi.read.ReaderCustomizer;
import io.fd.vpp.jvpp.core.dto.OneLocatorDetails;
import io.fd.vpp.jvpp.core.dto.OneLocatorDetailsReplyDump;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170808.locator.sets.grouping.LocatorSets;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170808.locator.sets.grouping.locator.sets.LocatorSet;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170808.locator.sets.grouping.locator.sets.LocatorSetBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170808.locator.sets.grouping.locator.sets.LocatorSetKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170808.locator.sets.grouping.locator.sets.locator.set.Interface;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170808.locator.sets.grouping.locator.sets.locator.set.InterfaceBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170808.locator.sets.grouping.locator.sets.locator.set.InterfaceKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class InterfaceCustomizerTest
        extends ListReaderCustomizerTest<Interface, InterfaceKey, InterfaceBuilder> {

    public InterfaceCustomizerTest() {
        super(Interface.class, LocatorSetBuilder.class);
    }

    private InstanceIdentifier<Interface> validId;

    @Before
    public void init() {
        validId = InstanceIdentifier.create(LocatorSets.class).child(LocatorSet.class, new LocatorSetKey("loc-set-1"))
                .child(Interface.class, new InterfaceKey("interface-1"));

        //mappings
        defineMappings();
        //dump data
        defineDumpData();
    }

    private void defineDumpData() {
        final OneLocatorDetailsReplyDump dump = new OneLocatorDetailsReplyDump();

        final OneLocatorDetails detail1 = new OneLocatorDetails();
        detail1.swIfIndex = 1;
        detail1.ipAddress = new byte[]{-64, -88, 2, 1};
        detail1.isIpv6 = 0;
        detail1.local = 0;
        detail1.priority = 1;
        detail1.weight = 2;

        final OneLocatorDetails detail2 = new OneLocatorDetails();
        detail2.swIfIndex = 2;
        detail2.ipAddress = new byte[]{-64, -88, 2, 2};
        detail2.isIpv6 = 0;
        detail2.local = 0;
        detail2.priority = 2;
        detail2.weight = 3;

        dump.oneLocatorDetails = ImmutableList.of(detail1, detail2);

        when(api.oneLocatorDump(Mockito.any())).thenReturn(future(dump));
    }

    private void defineMappings() {
        defineMapping(mappingContext, "interface-1", 1, "interface-context");
        defineMapping(mappingContext, "interface-2", 2, "interface-context");
        defineMapping(mappingContext, "loc-set-1", 3, "locator-set-context");
    }

    @Test
    public void testGetAllIds() throws ReadFailedException {

        final List<InterfaceKey> keys = getCustomizer().getAllIds(validId, ctx);

        assertEquals(2, keys.size());
        assertEquals("interface-1", keys.get(0).getInterfaceRef());
        assertEquals("interface-2", keys.get(1).getInterfaceRef());
    }

    @Test
    public void testReadCurrentAttributes() throws ReadFailedException {
        InterfaceBuilder builder = new InterfaceBuilder();
        getCustomizer().readCurrentAttributes(validId, builder, ctx);

        final Interface iface = builder.build();
        assertEquals("interface-1", iface.getInterfaceRef());
        assertEquals("interface-1", iface.getKey().getInterfaceRef());

    }

    @Override
    protected ReaderCustomizer<Interface, InterfaceBuilder> initCustomizer() {
        return new InterfaceCustomizer(api, new NamingContext("interface", "interface-context"),
                new NamingContext("loc-set", "locator-set-context"));
    }
}
