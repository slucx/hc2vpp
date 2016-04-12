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

import com.google.common.base.Optional;
import com.google.common.collect.Multimap;
import io.fd.honeycomb.v3po.translate.impl.read.CompositeChildReader;
import io.fd.honeycomb.v3po.translate.impl.read.CompositeListReader;
import io.fd.honeycomb.v3po.translate.impl.read.CompositeRootReader;
import io.fd.honeycomb.v3po.translate.util.read.DelegatingReaderRegistry;
import io.fd.honeycomb.v3po.translate.util.read.ReflexiveChildReaderCustomizer;
import io.fd.honeycomb.v3po.translate.util.read.ReflexiveRootReaderCustomizer;
import io.fd.honeycomb.v3po.translate.util.RWUtils;
import io.fd.honeycomb.v3po.translate.read.ChildReader;
import io.fd.honeycomb.v3po.translate.read.ReadContext;
import io.fd.honeycomb.v3po.translate.read.ReadFailedException;
import io.fd.honeycomb.v3po.translate.read.Reader;
import io.fd.honeycomb.v3po.translate.v3po.vppstate.BridgeDomainCustomizer;
import io.fd.honeycomb.v3po.translate.v3po.vppstate.VersionCustomizer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.VppState;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.VppStateBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.vpp.state.BridgeDomains;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.vpp.state.BridgeDomainsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.vpp.state.Version;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.vpp.state.bridge.domains.BridgeDomain;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.vpp.state.bridge.domains.BridgeDomainBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.vpp.state.bridge.domains.BridgeDomainKey;
import org.opendaylight.yangtools.yang.binding.ChildOf;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.openvpp.vppjapi.vppApi;

// TODO use some DI framework instead of singleton
public class ReaderRegistry implements io.fd.honeycomb.v3po.translate.read.ReaderRegistry {

    private static ReaderRegistry instance;

    private final DelegatingReaderRegistry reader;

    private ReaderRegistry(@Nonnull final vppApi vppApi) {
        final CompositeRootReader<VppState, VppStateBuilder> vppStateReader = initVppStateReader(vppApi);
        // TODO add more root readers
        reader = new DelegatingReaderRegistry(Collections.<Reader<? extends DataObject>>singletonList(vppStateReader));
    }

    private static CompositeRootReader<VppState, VppStateBuilder> initVppStateReader(@Nonnull final vppApi vppApi) {

        final ChildReader<Version> versionReader = new CompositeChildReader<>(
                Version.class, new VersionCustomizer(vppApi));

        final CompositeListReader<BridgeDomain, BridgeDomainKey, BridgeDomainBuilder>
                bridgeDomainReader = new CompositeListReader<>(
                BridgeDomain.class,
                new BridgeDomainCustomizer(vppApi));

        final ChildReader<BridgeDomains> bridgeDomainsReader = new CompositeChildReader<>(
                BridgeDomains.class,
                RWUtils.singletonChildReaderList(bridgeDomainReader),
                new ReflexiveChildReaderCustomizer<>(BridgeDomainsBuilder.class));

        final List<ChildReader<? extends ChildOf<VppState>>> childVppReaders = new ArrayList<>();
        childVppReaders.add(versionReader);
        childVppReaders.add(bridgeDomainsReader);

        return new CompositeRootReader<>(
                VppState.class,
                childVppReaders,
                RWUtils.<VppState>emptyAugReaderList(),
                new ReflexiveRootReaderCustomizer<>(VppStateBuilder.class));
    }

    public static synchronized ReaderRegistry getInstance(@Nonnull final vppApi vppApi) {
        if (instance == null) {
            instance = new ReaderRegistry(vppApi);
        }
        return instance;
    }

    @Nonnull
    @Override
    public Multimap<InstanceIdentifier<? extends DataObject>, ? extends DataObject> readAll(
        @Nonnull final ReadContext ctx) throws ReadFailedException {
        return reader.readAll(ctx);
    }

    @Nonnull
    @Override
    public Optional<? extends DataObject> read(@Nonnull final InstanceIdentifier<? extends DataObject> id,
                                               @Nonnull final ReadContext ctx) throws ReadFailedException {
        return reader.read(id, ctx);
    }

    @Nonnull
    @Override
    public InstanceIdentifier<DataObject> getManagedDataObjectType() {
        return reader.getManagedDataObjectType();
    }
}