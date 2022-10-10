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
package org.eclipse.jkube.micronaut.enricher;

import java.util.Arrays;
import java.util.Properties;
import java.io.File;
import java.util.Collections;

import org.eclipse.jkube.kit.common.JavaProject;
import org.eclipse.jkube.kit.config.image.ImageConfiguration;
import org.eclipse.jkube.kit.config.image.build.BuildConfiguration;
import org.eclipse.jkube.kit.config.resource.PlatformMode;
import org.eclipse.jkube.kit.config.resource.ProcessorConfig;
import org.eclipse.jkube.kit.enricher.api.JKubeEnricherContext;

import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.KubernetesListBuilder;
import io.fabric8.kubernetes.api.model.PodSpec;
import io.fabric8.kubernetes.api.model.PodTemplateSpec;
import io.fabric8.kubernetes.api.model.ServiceBuilder;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.apps.DeploymentBuilder;
import io.fabric8.kubernetes.api.model.apps.DeploymentSpec;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.eclipse.jkube.micronaut.MicronautUtils;
import org.junit.jupiter.api.AfterEach;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MicronautHealthCheckEnricherTest {
  private MockedStatic<MicronautUtils> micronautUtils;
  private JKubeEnricherContext context;
  private JavaProject project;
  private KubernetesListBuilder klb;

  @BeforeEach
  void setUp() {
    context = mock(JKubeEnricherContext.class, RETURNS_DEEP_STUBS);
    project = mock(JavaProject.class);
    micronautUtils = Mockito.mockStatic(MicronautUtils.class);
    Properties properties = new Properties();
    ProcessorConfig processorConfig = new ProcessorConfig();
    klb = new KubernetesListBuilder();
    klb.addToItems(new ServiceBuilder()
        .withNewMetadata().withName("make-it-real").endMetadata()
        .build());
    when(project.getOutputDirectory()).thenReturn(new File("target"));
    when(context.getProject()).thenReturn(project);
    when(context.getProperties()).thenReturn(properties);
    when(context.getConfiguration().getProcessorConfig()).thenReturn(processorConfig);
    when(context.getConfiguration().getImages()).thenReturn(Collections.emptyList());
  }

  @AfterEach
  public void tearDown() {
    micronautUtils.close();
  }

  @Test
  void createWithNoDeployment() {
    // When
    new MicronautHealthCheckEnricher(context).create(PlatformMode.kubernetes, klb);
    // Then
    assertThat(klb.build().getItems())
        .hasSize(1)
        .extracting("kind")
        .containsOnly("Service");
  }

  @Test
  void createWithDeploymentAndNoPlugin() {
    // Given
    klb.addToItems(emptyDeployment());
    // When
    new MicronautHealthCheckEnricher(context).create(PlatformMode.kubernetes, klb);
    // Then
    assertThat(klb.build().getItems())
        .hasSize(2)
        .element(1, InstanceOfAssertFactories.type(Deployment.class))
        .extracting(Deployment::getSpec)
        .extracting(DeploymentSpec::getTemplate)
        .extracting(PodTemplateSpec::getSpec)
        .extracting(PodSpec::getContainers)
        .asList()
        .element(0, InstanceOfAssertFactories.type(Container.class))
        .hasFieldOrPropertyWithValue("livenessProbe", null)
        .hasFieldOrPropertyWithValue("readinessProbe", null);
  }
  @Test
  void createWithDeploymentAndPluginAndNoHealth() {
    // Given
    klb.addToItems(emptyDeployment());
    withMicronautPlugin();
    // When
    new MicronautHealthCheckEnricher(context).create(PlatformMode.kubernetes, klb);
    // Then
    assertThat(klb.build().getItems())
        .hasSize(2)
        .element(1, InstanceOfAssertFactories.type(Deployment.class))
        .extracting(Deployment::getSpec)
        .extracting(DeploymentSpec::getTemplate)
        .extracting(PodTemplateSpec::getSpec)
        .extracting(PodSpec::getContainers)
        .asList()
        .element(0, InstanceOfAssertFactories.type(Container.class))
        .hasFieldOrPropertyWithValue("livenessProbe", null)
        .hasFieldOrPropertyWithValue("readinessProbe", null);
  }

  @Test
  void createWithDeploymentAndPluginAndHealth() {
    // Given
    klb.addToItems(emptyDeployment());
    withHealthEnabled();
    withMicronautPlugin();
    // When
    new MicronautHealthCheckEnricher(context).create(PlatformMode.kubernetes, klb);
    // Then
    assertThat(klb.build().getItems())
        .hasSize(2)
        .element(1, InstanceOfAssertFactories.type(Deployment.class))
        .extracting("spec.template.spec.containers")
        .asList()
        .element(0, InstanceOfAssertFactories.type(Container.class))
        .extracting(
            "livenessProbe.httpGet.path",
            "readinessProbe.httpGet.path",
            "readinessProbe.httpGet.port.IntVal"
        )
        .contains("/health", "/health", null);
  }

  @Test
  void createWithDeploymentAndPluginAndImageConfig() {
    // Given
    klb.addToItems(emptyDeployment());
    withHealthEnabled();
    withMicronautPlugin();
    when(context.getConfiguration().getImages()).thenReturn(Arrays.asList(
            ImageConfiguration.builder().build(BuildConfiguration.builder().port("1337").build()).build(),
            ImageConfiguration.builder().build(BuildConfiguration.builder().port("8082").build()).build()
    ));
    // When
    new MicronautHealthCheckEnricher(context).create(PlatformMode.kubernetes, klb);
    // Then
    assertThat(klb.build().getItems())
        .hasSize(2)
        .element(1, InstanceOfAssertFactories.type(Deployment.class))
        .extracting("spec.template.spec.containers")
        .asList()
        .element(0, InstanceOfAssertFactories.type(Container.class))
        .extracting(
            "livenessProbe.httpGet.path",
            "readinessProbe.httpGet.path",
            "readinessProbe.httpGet.port.IntVal"
        )
        .contains("/health", "/health", 1337);
  }

  private void withHealthEnabled() {
    micronautUtils.when(() -> MicronautUtils.isHealthEnabled(any())).thenReturn(true);
  }

  private void withMicronautPlugin() {
    micronautUtils.when(() -> MicronautUtils.hasMicronautPlugin(project)).thenReturn(true);
  }

  private static DeploymentBuilder emptyDeployment() {
    // @formatter:off
    return new DeploymentBuilder()
        .editOrNewSpec()
          .editOrNewTemplate()
            .editOrNewSpec()
              .addNewContainer()
              .endContainer()
            .endSpec()
          .endTemplate()
        .endSpec();
    // @formatter:off
  }

}
