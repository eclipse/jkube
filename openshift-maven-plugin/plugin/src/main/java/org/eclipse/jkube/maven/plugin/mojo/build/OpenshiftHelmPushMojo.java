/*
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
package org.eclipse.jkube.maven.plugin.mojo.build;

import java.io.File;
import java.nio.file.Path;

import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.eclipse.jkube.kit.resource.helm.HelmConfig;
import org.eclipse.jkube.maven.plugin.mojo.OpenShift;

@Mojo(name = "helm-push", defaultPhase = LifecyclePhase.INSTALL, requiresDependencyResolution = ResolutionScope.COMPILE)
public class OpenshiftHelmPushMojo extends HelmPushMojo {

  /**
   * One of:
   * <ul>
   * <li>A directory containing OpenShift Templates to use as Helm parameters.</li>
   * <li>A file containing a Kubernetes List with OpenShift Template entries to be used as Helm parameters.</li>
   * </ul>
   */
  @Parameter(property = "jkube.openshiftTemplate", defaultValue = "${basedir}/target/classes/META-INF/jkube/openshift")
  private File openShiftTemplate;

  @Override
  protected File getKubernetesTemplate() {
    return openShiftTemplate;
  }

  @Override
  protected HelmConfig.HelmType getDefaultHelmType() {
    return HelmConfig.HelmType.OPENSHIFT;
  }

  @Override
  protected String getLogPrefix() {
    return OpenShift.DEFAULT_LOG_PREFIX;
  }

  @Override
  protected void logChartNotFoundWarning(final Path chart) {
    getKitLogger().warn("No Helm chart has been generated yet by the oc:helm goal at: " + chart);
  }
}
