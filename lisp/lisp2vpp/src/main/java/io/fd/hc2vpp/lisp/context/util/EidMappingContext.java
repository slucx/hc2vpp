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

package io.fd.hc2vpp.lisp.context.util;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.base.Optional;
import io.fd.hc2vpp.lisp.translate.util.EidTranslator;
import io.fd.honeycomb.translate.MappingContext;
import io.fd.honeycomb.translate.util.RWUtils;

import java.util.stream.Collector;
import javax.annotation.Nonnull;

import org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.eid.mapping.context.rev160801.Contexts;
import org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.eid.mapping.context.rev160801.contexts.EidMappingContextKey;
import org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.eid.mapping.context.rev160801.contexts.eid.mapping.context.Mappings;
import org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.eid.mapping.context.rev160801.contexts.eid.mapping.context.mappings.Mapping;
import org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.eid.mapping.context.rev160801.contexts.eid.mapping.context.mappings.MappingBuilder;
import org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.eid.mapping.context.rev160801.contexts.eid.mapping.context.mappings.MappingKey;
import org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.eid.mapping.context.rev160801.contexts.eid.mapping.context.mappings.mapping.Eid;
import org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.eid.mapping.context.rev160801.contexts.eid.mapping.context.mappings.mapping.EidBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170808.MappingId;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.KeyedInstanceIdentifier;

/**
 * Utility class allowing {@link MappingId} to {@link Eid} mapping
 */
public class EidMappingContext implements EidTranslator {

    private static final Collector<Mapping, ?, Mapping> SINGLE_ITEM_COLLECTOR = RWUtils.singleItemCollector();

    private final KeyedInstanceIdentifier<org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.eid.mapping.context.rev160801.contexts.EidMappingContext, EidMappingContextKey>
            namingContextIid;
    private final String artificialPrefix;

    /**
     * Create new naming context
     *
     * @param instanceName name of this context instance. Will be used as list item identifier within context data tree
     */
    public EidMappingContext(@Nonnull final String instanceName, @Nonnull final String artificialPrefix) {
        namingContextIid = InstanceIdentifier.create(Contexts.class).child(
                org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.eid.mapping.context.rev160801.contexts.EidMappingContext.class,
                new EidMappingContextKey(instanceName));
        this.artificialPrefix = artificialPrefix;
    }

    /**
     * Retrieve name for mapping stored provided mappingContext instance.
     *
     * @param remoteEid      eid of a mapped item
     * @param mappingContext mapping context providing context data for current transaction
     * @return name mapped to provided index
     */
    @Nonnull
    public synchronized MappingId getId(
            @Nonnull final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170808.dp.subtable.grouping.remote.mappings.remote.mapping.Eid remoteEid,
            @Nonnull final MappingContext mappingContext) {

        final Optional<Mappings> read = mappingContext.read(namingContextIid.child(Mappings.class));
        // create artificial mapping if no mapping present or does not contain key
        // !read.isPresent() - no mappings are present,
        // for example after restart and clean of persistence
        // !containsId() - can happen with case described above after first mapping is
        // created or if trying to find mapping for some eid that was created by vpp as
        // byproduct of other call, or while trying to find mapping for default data
        if (!read.isPresent() || !containsId(remoteEid, mappingContext)) {
            final MappingId artificialMappingId = getMappingId(remoteEid.toString(), artificialPrefix);
            addEid(artificialMappingId, remoteEid, mappingContext);
            return artificialMappingId;
        }

        return read.get().getMapping()
                .stream()
                //cannot split to map + filtering,because its collecting mappings,not eid's
                .filter(mapping -> compareEids(mapping.getEid(), remoteEid))
                .collect(SINGLE_ITEM_COLLECTOR).getId();
    }

    private static MappingId getMappingId(final String eidValue, final String artificialPrefix) {
        return new MappingId(artificialPrefix.concat(eidValue));
    }

    /**
     * Retrieve name for mapping stored provided mappingContext instance.
     *
     * @param eid            eid of a mapped item
     * @param mappingContext mapping context providing context data for current transaction
     * @return name mapped to provided index
     */
    @Nonnull
    public synchronized MappingId getId(
            @Nonnull final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170808.dp.subtable.grouping.local.mappings.local.mapping.Eid eid,
            @Nonnull final MappingContext mappingContext) {

        final Optional<Mappings> read = mappingContext.read(namingContextIid.child(Mappings.class));
        // create artificial mapping if no mapping present or does not contain key
        // !read.isPresent() - no mappings are present,
        // for example after restart and clean of persistence
        // !containsId() - can happen with case described above after first mapping is
        // created or if trying to find mapping for some eid that was created by vpp as
        // byproduct of other call, or while trying to find mapping for default data
        if (!read.isPresent() || !containsId(eid, mappingContext)) {
            final MappingId artificialMappingId = getMappingId(eid.toString(), artificialPrefix);
            addEid(artificialMappingId, eid, mappingContext);
            return artificialMappingId;
        }

        return read.get().getMapping().stream()
                .filter(mapping -> compareEids(mapping.getEid(), eid))
                .collect(SINGLE_ITEM_COLLECTOR).getId();
    }

    /**
     * Check whether mapping is present for index.
     *
     * @param eid            eid of a mapped item
     * @param mappingContext mapping context providing context data for current transaction
     * @return true if present, false otherwise
     */
    public synchronized boolean containsId(
            @Nonnull final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170808.dp.subtable.grouping.local.mappings.local.mapping.Eid eid,
            @Nonnull final MappingContext mappingContext) {
        final Optional<Mappings> read = mappingContext.read(namingContextIid.child(Mappings.class));

        return read.isPresent() &&
                read.get().getMapping()
                        .stream()
                        .anyMatch(mapping -> compareEids(mapping.getEid(), eid));
    }

    /**
     * Check whether mapping is present for index.
     *
     * @param eid            eid of a mapped item
     * @param mappingContext mapping context providing context data for current transaction
     * @return true if present, false otherwise
     */
    public synchronized boolean containsId(
            @Nonnull final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170808.dp.subtable.grouping.remote.mappings.remote.mapping.Eid eid,
            @Nonnull final MappingContext mappingContext) {
        final Optional<Mappings> read = mappingContext.read(namingContextIid.child(Mappings.class));

        return read.isPresent() &&
                read.get().getMapping()
                        .stream()
                        .anyMatch(mapping -> compareEids(mapping.getEid(), eid));
    }


    /**
     * Add mapping to current context
     *
     * @param index          index of a mapped item
     * @param eid            eid data
     * @param mappingContext mapping context providing context data for current transaction
     */
    public synchronized void addEid(
            @Nonnull final MappingId index,
            @Nonnull final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170808.dp.subtable.grouping.local.mappings.local.mapping.Eid eid,
            final MappingContext mappingContext) {

        final KeyedInstanceIdentifier<Mapping, MappingKey> mappingIid = getMappingIid(index);
        mappingContext.put(mappingIid, new MappingBuilder().setId(index).setEid(copyEid(eid)).build());
    }

    /**
     * Add mapping to current context
     *
     * @param index          index of a mapped item
     * @param eid            eid data
     * @param mappingContext mapping context providing context data for current transaction
     */
    public synchronized void addEid(
            @Nonnull final MappingId index,
            @Nonnull final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170808.dp.subtable.grouping.remote.mappings.remote.mapping.Eid eid,
            final MappingContext mappingContext) {

        final KeyedInstanceIdentifier<Mapping, MappingKey> mappingIid = getMappingIid(index);
        mappingContext.put(mappingIid, new MappingBuilder().setId(index).setEid(copyEid(eid)).build());
    }

    private KeyedInstanceIdentifier<Mapping, MappingKey> getMappingIid(final MappingId index) {
        return namingContextIid.child(Mappings.class).child(Mapping.class, new MappingKey(index));
    }

    private Eid copyEid(
            org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170808.dp.subtable.grouping.local.mappings.local.mapping.Eid eid) {
        return new EidBuilder().setAddress(normalizeIfPrefixBased(eid.getAddress())).setAddressType(eid.getAddressType())
                .setVirtualNetworkId(eid.getVirtualNetworkId()).build();
    }

    private Eid copyEid(
            org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170808.dp.subtable.grouping.remote.mappings.remote.mapping.Eid eid) {
        return new EidBuilder().setAddress(normalizeIfPrefixBased(eid.getAddress())).setAddressType(eid.getAddressType())
                .setVirtualNetworkId(eid.getVirtualNetworkId()).build();
    }

    /**
     * Remove mapping from current context
     *
     * @param index          identificator of a mapped item
     * @param mappingContext mapping context providing context data for current transaction
     */
    public synchronized void removeEid(@Nonnull final MappingId index, final MappingContext mappingContext) {
        mappingContext.delete(getMappingIid(index));
    }

    /**
     * Returns index value associated with the given name.
     *
     * @param index          index whitch should value sits on
     * @param mappingContext mapping context providing context data for current transaction
     * @return integer index value matching supplied name
     * @throws IllegalArgumentException if name was not found
     */
    public synchronized Eid getEid(@Nonnull final MappingId index, final MappingContext mappingContext) {
        final Optional<Mapping> read = mappingContext.read(getMappingIid(index));
        checkArgument(read.isPresent(), "No mapping stored for index: %s", index);
        return read.get().getEid();
    }

    /**
     * Check whether mapping is present for name.
     *
     * @param index          index of a mapped item
     * @param mappingContext mapping context providing context data for current transaction
     * @return true if present, false otherwise
     */
    public synchronized boolean containsEid(@Nonnull final MappingId index,
                                            @Nonnull final MappingContext mappingContext) {
        return mappingContext.read(getMappingIid(index)).isPresent();
    }
}
