/**
 * Copyright (c) 2019 Red Hat, Inc.
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at:
 *
 *     https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   Red Hat, Inc. - initial API and implementation
 */
package io.jkube.maven.enricher.specific;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

import io.fabric8.kubernetes.api.model.KubernetesListBuilder;
import io.fabric8.kubernetes.api.model.ServiceBuilder;
import io.jkube.kit.build.service.docker.ImageConfiguration;
import io.jkube.kit.common.Configs;
import io.jkube.kit.config.image.build.BuildConfiguration;
import io.jkube.kit.config.resource.PlatformMode;
import io.jkube.kit.config.resource.ProcessorConfig;
import io.jkube.maven.enricher.api.MavenEnricherContext;
import io.jkube.maven.enricher.api.model.Configuration;
import mockit.Expectations;
import mockit.Mocked;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class PrometheusEnricherTest {

    @Mocked
    private MavenEnricherContext context;
    @Mocked
    ImageConfiguration imageConfiguration;

    private enum Config implements Configs.Key {
        prometheusPort,
        prometheusPath;
        public String def() { return d; } protected String d;
    }

    // *******************************
    // Tests
    // *******************************

    @Test
    public void testCustomPrometheusPort() throws Exception {
        final ProcessorConfig config = new ProcessorConfig(
            null,
            null,
            Collections.singletonMap(
                PrometheusEnricher.ENRICHER_NAME,
                new TreeMap(Collections.singletonMap(
                    Config.prometheusPort.name(),
                    "1234")
                )
            )
        );

        // Setup mock behaviour
        new Expectations() {{
            context.getConfiguration(); result = new Configuration.Builder().processorConfig(config).build();
        }};

        KubernetesListBuilder builder = new KubernetesListBuilder().withItems(new ServiceBuilder().withNewMetadata().withName("foo").endMetadata().build());
        PrometheusEnricher enricher = new PrometheusEnricher(context);
        enricher.create(PlatformMode.kubernetes, builder);
        Map<String, String> annotations = builder.buildFirstItem().getMetadata().getAnnotations();

        assertEquals(2, annotations.size());
        assertEquals("1234", annotations.get(PrometheusEnricher.ANNOTATION_PROMETHEUS_PORT));
        assertEquals("true", annotations.get(PrometheusEnricher.ANNOTATION_PROMETHEUS_SCRAPE));
    }

    @Test
    public void testDetectPrometheusPort() throws Exception {
        final ProcessorConfig config = new ProcessorConfig(
            null,
            null,
            Collections.singletonMap(
                PrometheusEnricher.ENRICHER_NAME,
                new TreeMap()
            )
        );

        final BuildConfiguration imageConfig = new BuildConfiguration.Builder()
            .ports(Arrays.asList(PrometheusEnricher.PROMETHEUS_PORT))
            .build();


        // Setup mock behaviour
        new Expectations() {{
            context.getConfiguration();
            result = new Configuration.Builder()
                .processorConfig(config)
                .images(Arrays.asList(imageConfiguration))
                .build();

            imageConfiguration.getBuildConfiguration(); result = imageConfig;
        }};

        KubernetesListBuilder builder = new KubernetesListBuilder().withItems(new ServiceBuilder().withNewMetadata().withName("foo").endMetadata().build());
        PrometheusEnricher enricher = new PrometheusEnricher(context);
        enricher.create(PlatformMode.kubernetes, builder);
        Map<String, String> annotations = builder.buildFirstItem().getMetadata().getAnnotations();

        assertEquals(2, annotations.size());
        assertEquals("9779", annotations.get(PrometheusEnricher.ANNOTATION_PROMETHEUS_PORT));
        assertEquals("true", annotations.get(PrometheusEnricher.ANNOTATION_PROMETHEUS_SCRAPE));
    }

    @Test
    public void testNoDefinedPrometheusPort() throws Exception {
        final ProcessorConfig config = new ProcessorConfig(
            null,
            null,
            Collections.singletonMap(
                PrometheusEnricher.ENRICHER_NAME,
                new TreeMap()
            )
        );

        final BuildConfiguration imageConfig = new BuildConfiguration.Builder()
            .build();

        // Setup mock behaviour
        new Expectations() {{
            context.getConfiguration();
            result = new Configuration.Builder()
                .processorConfig(config)
                .images(Arrays.asList(imageConfiguration))
                .build();

            imageConfiguration.getBuildConfiguration(); result = imageConfig;
        }};

        KubernetesListBuilder builder = new KubernetesListBuilder().withItems(new ServiceBuilder().withNewMetadata().withName("foo").endMetadata().build());
        PrometheusEnricher enricher = new PrometheusEnricher(context);
        enricher.create(PlatformMode.kubernetes, builder);
        Map<String, String> annotations = builder.buildFirstItem().getMetadata().getAnnotations();

        assertEquals(Collections.emptyMap(), annotations);
    }

    @Test
    public void testCustomPrometheusPath() throws Exception {
        final ProcessorConfig config = new ProcessorConfig(
                null,
                null,
                Collections.singletonMap(
                        PrometheusEnricher.ENRICHER_NAME,
                        new TreeMap(Collections.singletonMap(
                                Config.prometheusPath.name(),
                                "/prometheus")
                        )
                )
        );

        final BuildConfiguration imageConfig = new BuildConfiguration.Builder()
                .ports(Arrays.asList(PrometheusEnricher.PROMETHEUS_PORT))
                .build();


        // Setup mock behaviour
        new Expectations() {{
            context.getConfiguration();
            result = new Configuration.Builder()
                    .processorConfig(config)
                    .images(Arrays.asList(imageConfiguration))
                    .build();

            imageConfiguration.getBuildConfiguration(); result = imageConfig;
        }};

        KubernetesListBuilder builder = new KubernetesListBuilder().withItems(new ServiceBuilder().withNewMetadata().withName("foo").endMetadata().build());
        PrometheusEnricher enricher = new PrometheusEnricher(context);
        enricher.create(PlatformMode.kubernetes, builder);
        Map<String, String> annotations = builder.buildFirstItem().getMetadata().getAnnotations();

        assertEquals(3, annotations.size());
        assertEquals("9779", annotations.get(PrometheusEnricher.ANNOTATION_PROMETHEUS_PORT));
        assertEquals("true", annotations.get(PrometheusEnricher.ANNOTATION_PROMETHEUS_SCRAPE));
        assertEquals("/prometheus", annotations.get(PrometheusEnricher.ANNOTATION_PROMETHEUS_PATH));
    }
}
