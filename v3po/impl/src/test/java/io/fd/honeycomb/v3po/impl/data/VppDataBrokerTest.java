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

package io.fd.honeycomb.v3po.impl.data;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.TransactionChainListener;
import org.opendaylight.controller.md.sal.dom.api.DOMDataBrokerExtension;
import org.opendaylight.controller.md.sal.dom.api.DOMDataChangeListener;
import org.opendaylight.controller.md.sal.dom.api.DOMDataReadOnlyTransaction;
import org.opendaylight.controller.md.sal.dom.api.DOMDataReadWriteTransaction;
import org.opendaylight.controller.md.sal.dom.api.DOMDataWriteTransaction;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;

public class VppDataBrokerTest {

    @Mock
    private ReadableVppDataTree operationalData;
    @Mock
    private VppDataTree confiDataTree;
    @Mock
    private VppDataTreeSnapshot configSnapshot;
    private VppDataBroker broker;

    @Before
    public void setUp() {
        initMocks(this);
        when(confiDataTree.takeSnapshot()).thenReturn(configSnapshot);
        broker = new VppDataBroker(operationalData, confiDataTree);
    }

    @Test
    public void testNewReadWriteTransaction() {
        final DOMDataReadWriteTransaction readWriteTx = broker.newReadWriteTransaction();
        final YangInstanceIdentifier path = mock(YangInstanceIdentifier.class);
        readWriteTx.read(LogicalDatastoreType.CONFIGURATION, path);

        // verify that read and write transactions use the same config snapshot
        verify(configSnapshot).read(path);
        verify(configSnapshot).newModification();
    }

    @Test
    public void testNewWriteOnlyTransaction() {
        final DOMDataWriteTransaction writeTx = broker.newWriteOnlyTransaction();

        // verify that write transactions use config snapshot
        verify(configSnapshot).newModification();
    }

    @Test
    public void testNewReadOnlyTransaction() {
        final DOMDataReadOnlyTransaction readTx = broker.newReadOnlyTransaction();

        final YangInstanceIdentifier path = mock(YangInstanceIdentifier.class);
        readTx.read(LogicalDatastoreType.CONFIGURATION, path);

        // verify that read transactions use config snapshot
        verify(configSnapshot).read(path);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testRegisterDataChangeListener() {
        final YangInstanceIdentifier path = mock(YangInstanceIdentifier.class);
        final DOMDataChangeListener listener = mock(DOMDataChangeListener.class);
        broker.registerDataChangeListener(LogicalDatastoreType.OPERATIONAL, path, listener,
                AsyncDataBroker.DataChangeScope.BASE);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testCreateTransactionChain() {
        final TransactionChainListener listener = mock(TransactionChainListener.class);
        broker.createTransactionChain(listener);
    }

    @Test
    public void testGetSupportedExtensions() {
        final Map<Class<? extends DOMDataBrokerExtension>, DOMDataBrokerExtension> supportedExtensions =
                broker.getSupportedExtensions();
        assertTrue(supportedExtensions.isEmpty());
    }


}