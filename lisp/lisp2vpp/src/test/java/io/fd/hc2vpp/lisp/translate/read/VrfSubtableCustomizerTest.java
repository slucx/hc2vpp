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


import static io.fd.hc2vpp.lisp.translate.read.dump.executor.params.SubtableDumpParams.MapLevel.L3;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import io.fd.hc2vpp.lisp.translate.read.trait.SubtableReaderTestCase;
import io.fd.honeycomb.translate.read.ReadFailedException;
import io.fd.honeycomb.translate.spi.read.ReaderCustomizer;
import io.fd.vpp.jvpp.VppCallbackException;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170808.eid.table.grouping.EidTable;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170808.eid.table.grouping.eid.table.VniTable;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170808.eid.table.grouping.eid.table.VniTableBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170808.eid.table.grouping.eid.table.VniTableKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170808.eid.table.grouping.eid.table.vni.table.VrfSubtable;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170808.eid.table.grouping.eid.table.vni.table.VrfSubtableBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class VrfSubtableCustomizerTest extends SubtableReaderTestCase<VrfSubtable, VrfSubtableBuilder> {

    private InstanceIdentifier<VrfSubtable> validId;

    public VrfSubtableCustomizerTest() {
        super(VrfSubtable.class, VrfSubtableBuilder.class);
    }

    @Before
    public void init() {
        validId = InstanceIdentifier.create(EidTable.class).child(VniTable.class, new VniTableKey(expectedVni))
                .child(VrfSubtable.class);
    }

    @Test
    public void testReadCurrentSuccessfull() throws ReadFailedException {
        doReturnValidNonEmptyDataOnDump();
        VrfSubtableBuilder builder = new VrfSubtableBuilder();
        customizer.readCurrentAttributes(validId, builder, ctx);

        verifyOneEidTableMapDumpCalled(L3);

        final VrfSubtable subtable = builder.build();
        assertNotNull(subtable);
        assertEquals(expectedTableId, subtable.getTableId().longValue());
    }

    @Test
    public void testReadCurrentEmptyDump() throws ReadFailedException {
        doReturnEmptyDataOnDump();
        VrfSubtableBuilder builder = new VrfSubtableBuilder();
        customizer.readCurrentAttributes(validId, builder, ctx);

        verifyOneEidTableMapDumpCalled(L3);

        final VrfSubtable subtable = builder.build();
        assertNotNull(subtable);
        assertNull(subtable.getTableId());
    }

    @Test
    public void testReadCurrentFailed() {
        doThrowOnDump();
        VrfSubtableBuilder builder = new VrfSubtableBuilder();
        try {
            customizer.readCurrentAttributes(validId, builder, ctx);
        } catch (ReadFailedException e) {
            assertTrue(e.getCause() instanceof VppCallbackException);
            assertTrue(builder.getTableId() == null);
            verifyOneEidTableMapDumpNotCalled();

            return;
        }

        fail("Test should throw ReadFailedException");
    }

    @Override
    protected ReaderCustomizer<VrfSubtable, VrfSubtableBuilder> initCustomizer() {
        return new VrfSubtableCustomizer(api);
    }

    @Test
    public void testGetBuilder() {
        final VrfSubtableBuilder builder = customizer.getBuilder(validId);

        assertNotNull(builder);
        assertNull(builder.getLocalMappings());
        assertNull(builder.getRemoteMappings());
        assertNull(builder.getTableId());
    }

    @Test
    public void testMerge() {
        VniTableBuilder parentBuilder = new VniTableBuilder();
        VrfSubtable subtable = new VrfSubtableBuilder().build();

        customizer.merge(parentBuilder, subtable);
        assertEquals(subtable, parentBuilder.getVrfSubtable());
    }
}
