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

package io.fd.honeycomb.translate.v3po.interfaces.acl;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.mockito.MockitoAnnotations.initMocks;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160708.access.lists.acl.access.list.entries.ace.actions.PacketHandling;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160708.access.lists.acl.access.list.entries.ace.actions.packet.handling.DenyBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160708.access.lists.acl.access.list.entries.ace.matches.ace.type.AceIp;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160708.access.lists.acl.access.list.entries.ace.matches.ace.type.AceIpBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160708.access.lists.acl.access.list.entries.ace.matches.ace.type.ace.ip.ace.ip.version.AceIpv6Builder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Dscp;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv6FlowLabel;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv6Prefix;
import org.openvpp.jvpp.core.dto.ClassifyAddDelSession;
import org.openvpp.jvpp.core.dto.ClassifyAddDelTable;
import org.openvpp.jvpp.core.dto.InputAclSetInterface;
import org.openvpp.jvpp.core.future.FutureJVppCore;

public class AceIp6WriterTest {

    @Mock
    private FutureJVppCore jvpp;
    private AceIp6Writer writer;
    private PacketHandling action;
    private AceIp aceIp;

    @Before
    public void setUp() throws Exception {
        initMocks(this);
        writer = new AceIp6Writer(jvpp);
        action = new DenyBuilder().setDeny(true).build();
        aceIp = new AceIpBuilder()
            .setProtocol((short) 6)
            .setDscp(new Dscp((short) 11))
            .setAceIpVersion(new AceIpv6Builder()
                .setFlowLabel(new Ipv6FlowLabel(123L))
                .setSourceIpv6Network(new Ipv6Prefix("2001:db8:85a3:8d3:1319:8a2e:370:7348/128"))
                .setDestinationIpv6Network(new Ipv6Prefix("fe80:1234:5678:abcd:ef01::/64"))
                .build())
            .build();
    }

    @Test
    public void testGetClassifyAddDelTableRequest() throws Exception {
        final int nextTableIndex = 42;
        final ClassifyAddDelTable request = writer.createClassifyTable(action, aceIp, nextTableIndex);

        assertEquals(1, request.isAdd);
        assertEquals(-1, request.tableIndex);
        assertEquals(1, request.nbuckets);
        assertEquals(-1, request.missNextIndex);
        assertEquals(nextTableIndex, request.nextTableIndex);
        assertEquals(0, request.skipNVectors);
        assertEquals(AceIp6Writer.MATCH_N_VECTORS, request.matchNVectors);
        assertEquals(AceIp6Writer.TABLE_MEM_SIZE, request.memorySize);

        byte[] expectedMask = new byte[] {
            // L2:
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            // version, dscp, flow:
            (byte) 0xff, (byte) 0xcf, (byte) 0xff, (byte) 0xff,
            0, 0, 0, 0,
            // source address:
            (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
            (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
            // destination address:
            (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
            0, 0, 0, 0, 0, 0, 0, 0,
            // padding to multiple of 16B:
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0
        };
        assertArrayEquals(expectedMask, request.mask);
    }

    @Test
    public void testGetClassifyAddDelSessionRequest() throws Exception {
        final int tableIndex = 123;
        final ClassifyAddDelSession request = writer.createClassifySession(action, aceIp, tableIndex);

        assertEquals(1, request.isAdd);
        assertEquals(tableIndex, request.tableIndex);
        assertEquals(0, request.hitNextIndex);

        byte[] expectedMatch = new byte[] {
            // L2:
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            // version(6), dscp(11), flow(123):
            (byte) 0x62, (byte) 0xc0, (byte) 0x00, (byte) 0x7b,
            0, 0, 0, 0,
            // source address:
            (byte) 0x20, (byte) 0x01, (byte) 0x0d, (byte) 0xb8, (byte) 0x85, (byte) 0xa3, (byte) 0x08, (byte) 0xd3,
            (byte) 0x13, (byte) 0x19, (byte) 0x8a, (byte) 0x2e, (byte) 0x03, (byte) 0x70, (byte) 0x73, (byte) 0x48,
            // destination address:
            (byte) 0xfe, (byte) 0x80, (byte) 0x12, (byte) 0x34, (byte) 0x56, (byte) 0x78, (byte) 0xab, (byte) 0xcd,
            0, 0, 0, 0, 0, 0, 0, 0,
            // padding to multiple of 16B:
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0
        };
        assertArrayEquals(expectedMatch, request.match);
    }

    @Test
    public void testSetClassifyTable() throws Exception {
        final int tableIndex = 321;
        final InputAclSetInterface request = new InputAclSetInterface();
        writer.setClassifyTable(request, tableIndex);
        assertEquals(tableIndex, request.ip6TableIndex);
    }
}