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
package org.eclipse.jkube.gradle.plugin;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Stream;

import org.eclipse.jkube.kit.common.JavaProject;

import groovy.lang.Closure;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.ConfigurationContainer;
import org.gradle.api.artifacts.Dependency;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.eclipse.jkube.gradle.plugin.GradleUtil.closureTo;
import static org.eclipse.jkube.gradle.plugin.GradleUtil.convertGradleProject;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class GradleUtilTest {

  private Project project;

  @Before
  public void setUp() {
    project = mock(Project.class, RETURNS_DEEP_STUBS);
    when(project.getConfigurations().stream()).thenReturn(Stream.empty());
    when(project.getProperties()).thenReturn(Collections.emptyMap());
  }

  @Test
  public void closureTo_withNestedClosure_shouldReturnMap() {
    // Given
    final Closure<?> closure = new Closure<Object>(this) {

      public Object doCall(Object... args) {
        setProperty("property", "value");
        setProperty("nested", new Closure<Object>(this) {

          public Object doCall(Object... args) {
            setProperty("nestedProperty", "nestedValue");
            return null;
          }

        });
        return null;
      }
    };
    // When
    final Map<String, ?> result = closureTo(closure, Map.class);
    // Then
    assertThat(result).isNotNull()
        .hasFieldOrPropertyWithValue("property", "value")
        .hasFieldOrPropertyWithValue("nested.nestedProperty", "nestedValue")
        .hasFieldOrPropertyWithValue("nested", Collections.singletonMap("nestedProperty", "nestedValue"));
  }

  @Test
  public void extractProperties_withComplexMap_shouldReturnValidProperties() {
    // Given
    final Map<String, Object> complexProperties = new HashMap<>();
    when(project.getProperties()).thenAnswer(i -> complexProperties);
    complexProperties.put("property.1", "test");
    complexProperties.put("property.ignored", null);
    complexProperties.put("object", Collections.singletonMap("field1", 1));
    // When
    final JavaProject result = convertGradleProject(project);
    // Then
    assertThat(result.getProperties())
        .hasSize(2)
        .hasFieldOrPropertyWithValue("object.field1", 1)
        .hasFieldOrPropertyWithValue("property.1", null)
        .hasEntrySatisfying("property.1", e -> assertThat(e).isEqualTo("test"));
  }

  @Test
  public void extractDependencies_withMultipleAndDuplicateDependencies_shouldReturnValidDependencies() {
    // Given
    final ConfigurationContainer cc = mock(ConfigurationContainer.class);
    when(project.getConfigurations()).thenReturn(cc);
    final Function<String[], Configuration> mockConfiguration = s -> {
      final Configuration c = mock(Configuration.class, RETURNS_DEEP_STUBS);
      final Dependency d = mock(Dependency.class);
      when(c.getAllDependencies().stream()).thenAnswer(i -> Stream.of(d));
      when(d.getGroup()).thenReturn(s[0]);
      when(d.getName()).thenReturn(s[1]);
      when(d.getVersion()).thenReturn(s[2]);
      return c;
    };
    when(cc.stream()).thenAnswer(i -> Stream.of(
        mockConfiguration.apply(new String[] {"com.example", "artifact", null}),
        mockConfiguration.apply(new String[] {"com.example.sub", "duplicate-artifact", "1.33.7"}),
        mockConfiguration.apply(new String[] {"com.example.sub", "duplicate-artifact", "1.33.7"}),
        mockConfiguration.apply(new String[] {"com.example", "other.artifact", "1.0.0"}),
        mockConfiguration.apply(new String[] {"com.example", "other.artifact", "1.1.0"})));
    // When
    final JavaProject result = convertGradleProject(project);
    // Then
    assertThat(result.getDependencies())
        .hasSize(4)
        .extracting("groupId", "artifactId", "version")
        .containsExactlyInAnyOrder(
            tuple("com.example", "artifact", null),
            tuple("com.example.sub", "duplicate-artifact", "1.33.7"),
            tuple("com.example", "other.artifact", "1.0.0"),
            tuple("com.example", "other.artifact", "1.1.0")
        );

  }
}