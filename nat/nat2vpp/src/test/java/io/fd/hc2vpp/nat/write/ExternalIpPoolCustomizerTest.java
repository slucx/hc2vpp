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

package io.fd.hc2vpp.nat.write;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.fd.hc2vpp.common.test.write.WriterCustomizerTest;
import io.fd.hc2vpp.nat.NatTestSchemaContext;
import io.fd.honeycomb.test.tools.HoneycombTestRunner;
import io.fd.honeycomb.test.tools.annotations.InjectTestData;
import io.fd.honeycomb.translate.write.WriteFailedException;
import io.fd.vpp.jvpp.snat.dto.SnatAddAddressRange;
import io.fd.vpp.jvpp.snat.dto.SnatAddAddressRangeReply;
import io.fd.vpp.jvpp.snat.future.FutureJVppSnatFacade;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.nat.rev150908.NatConfig;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.nat.rev150908.nat.config.NatInstances;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.nat.rev150908.nat.config.nat.instances.NatInstance;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.nat.rev150908.nat.config.nat.instances.NatInstanceKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.nat.rev150908.nat.parameters.ExternalIpAddressPool;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.nat.rev150908.nat.parameters.ExternalIpAddressPoolKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

@RunWith(HoneycombTestRunner.class)
public class ExternalIpPoolCustomizerTest extends WriterCustomizerTest implements NatTestSchemaContext {

    private static final long NAT_INSTANCE_ID = 0;
    private static final long POOL_ID = 22;
    private static final InstanceIdentifier<ExternalIpAddressPool> IID = InstanceIdentifier.create(NatConfig.class)
        .child(NatInstances.class).child(NatInstance.class, new NatInstanceKey(NAT_INSTANCE_ID))
        .child(ExternalIpAddressPool.class, new ExternalIpAddressPoolKey(POOL_ID));

    private static final String NAT_INSTANCES_PATH = "/ietf-nat:nat-config/ietf-nat:nat-instances";

    @Mock
    private FutureJVppSnatFacade jvppSnat;
    private ExternalIpPoolCustomizer customizer;

    @Override
    public void setUpTest() {
        customizer = new ExternalIpPoolCustomizer(jvppSnat);
        when(jvppSnat.snatAddAddressRange(any())).thenReturn(future(new SnatAddAddressRangeReply()));
    }

    @Test
    public void testWrite(
        @InjectTestData(resourcePath = "/nat/external-ip-pool.json", id = NAT_INSTANCES_PATH) NatInstances data)
        throws WriteFailedException {
        customizer.writeCurrentAttributes(IID, extractIpPool(data), writeContext);
        final SnatAddAddressRange expectedRequest = getExpectedRequest();
        expectedRequest.isAdd = 1;
        verify(jvppSnat).snatAddAddressRange(expectedRequest);
    }

    @Test(expected = WriteFailedException.UpdateFailedException.class)
    public void testUpdate() throws WriteFailedException {
        final ExternalIpAddressPool data = mock(ExternalIpAddressPool.class);
        customizer.updateCurrentAttributes(IID, data, data, writeContext);
    }

    @Test
    public void testDelete(
        @InjectTestData(resourcePath = "/nat/external-ip-pool.json", id = NAT_INSTANCES_PATH) NatInstances data)
        throws WriteFailedException {
        customizer.deleteCurrentAttributes(IID, extractIpPool(data), writeContext);
        final SnatAddAddressRange expectedRequest = getExpectedRequest();
        verify(jvppSnat).snatAddAddressRange(expectedRequest);
    }

    private static ExternalIpAddressPool extractIpPool(NatInstances data) {
        // assumes single nat instance and single ip pool
        return data.getNatInstance().get(0).getExternalIpAddressPool().get(0);
    }

    private static SnatAddAddressRange getExpectedRequest() {
        final SnatAddAddressRange expectedRequest = new SnatAddAddressRange();
        expectedRequest.isIp4 = 1;
        expectedRequest.firstIpAddress = new byte[] {(byte) 192, (byte) 168, 1, 0};
        expectedRequest.lastIpAddress = new byte[] {(byte) 192, (byte) 168, 1, (byte) 255};
        return expectedRequest;
    }
}