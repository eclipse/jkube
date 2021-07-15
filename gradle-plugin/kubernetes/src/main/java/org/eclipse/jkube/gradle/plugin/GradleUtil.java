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

import java.util.AbstractMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.jkube.kit.common.Dependency;
import org.eclipse.jkube.kit.common.JavaProject;

import groovy.lang.Closure;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.DependencySet;

public class GradleUtil {

  private static final ObjectMapper closureMapper = new ObjectMapper();

  private GradleUtil() {}

  public static <T> T closureTo(Closure<?> closure, Class<T> targetType) {
    final Map<String, ?> map = new HashMap<>();
    final Closure<?> copy = closure.rehydrate(map, map, closure.getThisObject());
    copy.call();
    final Map<String, ?> result = map.entrySet().stream()
        .map(e -> {
          if (e.getValue() instanceof Closure) {
            return new AbstractMap.SimpleEntry<>(e.getKey(), closureTo((Closure<?>) e.getValue(), Map.class));
          }
          return e;
        })
        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    if (Map.class.isAssignableFrom(targetType)) {
      return (T)result;
    }
    return closureMapper.convertValue(result, targetType);
  }

  public static JavaProject convertGradleProject(Project gradleProject) {
    return JavaProject.builder()
        .properties(extractProperties(gradleProject))
        .name(gradleProject.getName())
        .description(gradleProject.getDescription())
        .groupId(Objects.toString(gradleProject.getGroup()))
        .artifactId(gradleProject.getName())
        .version(Objects.toString(gradleProject.getVersion()))
        .baseDirectory(gradleProject.getProjectDir())
//        .documentationUrl(gradleProject.)
//        .compileClassPathElements(gradleProject.)
//        .packaging(gradleProject.)
        .dependencies(extractDependencies(gradleProject))
//        .dependenciesWithTransitive(gradleProject.getDependencies().)
//        .localRepositoryBaseDirectory(gradleProject.)
//        .plugins(gradleProject.)
//
//        .site(gradleProject.)
//        .organizationName(gradleProject.)
//
        .outputDirectory(gradleProject.getBuildDir())
//        .buildFinalName(gradleProject.)
        .buildDirectory(gradleProject.getBuildDir())
//
//        .issueManagementSystem(gradleProject.)
//        .issueManagementUrl(gradleProject.)
//
//        .scmTag(gradleProject.)
//        .scmUrl(gradleProject.)
//      .artifact(gradleProject)
        .build();
  }

  private static Properties extractProperties(Project gradleProject) {
    return gradleProject.getProperties().entrySet().stream().filter(e -> Objects.nonNull(e.getValue()))
        .reduce(new Properties(), (acc, e) -> {
          acc.put(e.getKey(), e.getValue());
          return acc;
        }, (acc, e) -> acc);
  }

  private static List<Dependency> extractDependencies(Project gradleProject) {
    return gradleProject.getConfigurations().stream()
        .map(Configuration::getAllDependencies).flatMap(DependencySet::stream)
        .map(d -> Dependency.builder().groupId(d.getGroup()).artifactId(d.getName()).version(d.getVersion()).build())
        .distinct()
        .collect(Collectors.toList());
  }
}
