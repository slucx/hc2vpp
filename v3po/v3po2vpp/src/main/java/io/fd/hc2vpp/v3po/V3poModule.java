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

package io.fd.hc2vpp.v3po;

import com.google.inject.AbstractModule;
import com.google.inject.Singleton;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.name.Names;
import io.fd.hc2vpp.common.translate.util.NamingContext;
import io.fd.hc2vpp.v3po.factory.InterfacesStateReaderFactory;
import io.fd.hc2vpp.v3po.factory.InterfacesWriterFactory;
import io.fd.hc2vpp.v3po.factory.L2HoneycombWriterFactory;
import io.fd.hc2vpp.v3po.factory.L2StateHoneycombReaderFactory;
import io.fd.hc2vpp.v3po.factory.SubinterfaceAugmentationWriterFactory;
import io.fd.hc2vpp.v3po.factory.SubinterfaceStateAugmentationReaderFactory;
import io.fd.hc2vpp.v3po.interfacesstate.cache.InterfaceCacheDumpManager;
import io.fd.hc2vpp.v3po.interfacesstate.cache.InterfaceCacheDumpManagerProvider;
import io.fd.hc2vpp.v3po.notification.InterfaceChangeNotificationProducerProvider;
import io.fd.honeycomb.notification.ManagedNotificationProducer;
import io.fd.honeycomb.translate.read.ReaderFactory;
import io.fd.honeycomb.translate.write.WriterFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class V3poModule extends AbstractModule {

    private static final Logger LOG = LoggerFactory.getLogger(V3poModule.class);

    @Override
    protected void configure() {
        LOG.debug("Installing V3PO module");

        // TODO HONEYCOMB-173 put into constants
        // Naming contexts
        bind(NamingContext.class)
                .annotatedWith(Names.named("interface-context"))
                .toInstance(new NamingContext("interface-", "interface-context"));
        bind(NamingContext.class)
                .annotatedWith(Names.named("bridge-domain-context"))
                .toInstance(new NamingContext("bridge-domain-", "bridge-domain-context"));

        bind(InterfaceCacheDumpManager.class).toProvider(InterfaceCacheDumpManagerProvider.class).in(Singleton.class);

        // Context utility for deleted interfaces
        bind(DisabledInterfacesManager.class).toInstance(new DisabledInterfacesManager());

        // Readers
        final Multibinder<ReaderFactory> readerFactoryBinder = Multibinder.newSetBinder(binder(), ReaderFactory.class);
        readerFactoryBinder.addBinding().to(InterfacesStateReaderFactory.class);
        readerFactoryBinder.addBinding().to(SubinterfaceStateAugmentationReaderFactory.class);
        readerFactoryBinder.addBinding().to(L2StateHoneycombReaderFactory.class);

        // Expose disabled interfaces in operational data
        readerFactoryBinder.addBinding().to(DisabledInterfacesManager.ContextsReaderFactory.class);

        // Writers
        final Multibinder<WriterFactory> writerFactoryBinder = Multibinder.newSetBinder(binder(), WriterFactory.class);
        writerFactoryBinder.addBinding().to(InterfacesWriterFactory.class);
        writerFactoryBinder.addBinding().to(SubinterfaceAugmentationWriterFactory.class);
        writerFactoryBinder.addBinding().to(L2HoneycombWriterFactory.class);

        // Notifications
        final Multibinder<ManagedNotificationProducer> notifiersBinder =
                Multibinder.newSetBinder(binder(), ManagedNotificationProducer.class);
        notifiersBinder.addBinding().toProvider(InterfaceChangeNotificationProducerProvider.class);

        LOG.info("Module V3PO successfully configured");
    }
}
