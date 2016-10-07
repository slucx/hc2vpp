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

package io.fd.honeycomb.translate.v3po.initializers;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import io.fd.honeycomb.data.init.AbstractDataTreeConverter;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev161214.Vpp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev161214.VppBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev161214.VppState;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev161214.l2.fib.attributes.L2FibTable;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev161214.l2.fib.attributes.L2FibTableBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev161214.l2.fib.attributes.l2.fib.table.L2FibEntryBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev161214.vpp.BridgeDomainsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev161214.vpp.bridge.domains.BridgeDomain;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev161214.vpp.bridge.domains.BridgeDomainBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev161214.vpp.bridge.domains.BridgeDomainKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Initializes vpp node in config data tree based on operational state.
 */
public class VppInitializer extends AbstractDataTreeConverter<VppState, Vpp> {
    private static final Logger LOG = LoggerFactory.getLogger(VppInitializer.class);

    @Inject
    public VppInitializer(@Named("honeycomb-initializer") @Nonnull final DataBroker bindingDataBroker) {
        super(bindingDataBroker, InstanceIdentifier.create(VppState.class), InstanceIdentifier.create(Vpp.class));
    }

    @Override
    protected Vpp convert(final VppState operationalData) {
        LOG.debug("VppInitializer.convert()");

        VppBuilder vppBuilder = new VppBuilder();
        BridgeDomainsBuilder bdsBuilder = new BridgeDomainsBuilder();
        bdsBuilder.setBridgeDomain(Lists.transform(operationalData.getBridgeDomains().getBridgeDomain(), CONVERT_BD));
        vppBuilder.setBridgeDomains(bdsBuilder.build());
        return vppBuilder.build();
    }

    private static final Function<org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev161214.vpp.state.bridge.domains.BridgeDomain, BridgeDomain>
            CONVERT_BD = input -> {
        final BridgeDomainBuilder builder = new BridgeDomainBuilder();
        builder.setLearn(input.isLearn());
        builder.setUnknownUnicastFlood(input.isUnknownUnicastFlood());
        builder.setArpTermination(input.isArpTermination());
        builder.setFlood(input.isFlood());
        builder.setForward(input.isForward());
        builder.setKey(new BridgeDomainKey(input.getKey().getName()));
        builder.setName(input.getName());
        setL2FibTable(builder, input.getL2FibTable());
        return builder.build();
    };

    private static void setL2FibTable(@Nonnull final BridgeDomainBuilder builder,
                                      @Nullable final L2FibTable l2FibTable) {
        if (l2FibTable == null) {
            return;
        }
        final L2FibTableBuilder tableBuilder = new L2FibTableBuilder()
                .setL2FibEntry(
                        l2FibTable.getL2FibEntry().stream()
                                // Convert operational object to config. VPP does not support setting BVI (see v3po.yang)
                                .map(oper -> new L2FibEntryBuilder(oper).setBridgedVirtualInterface(null).build())
                                .collect(Collectors.toList()));
        builder.setL2FibTable(tableBuilder.build());
    }

}
