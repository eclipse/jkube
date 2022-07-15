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
package org.eclipse.jkube.kit.resource.helm;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;

import org.eclipse.jkube.kit.common.KitLogger;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class HelmUploaderTest {

  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();

  KitLogger kitLogger;

  HelmUploader helmUploader;

  @Before
  public void setUp() throws Exception {
    kitLogger = mock(KitLogger.class);
    helmUploader = new HelmUploader(kitLogger);
  }

  @After
  public void tearDown() {
    kitLogger = null;
    helmUploader = null;
  }

  @Test
  public void uploadSingle_withMissingType_shouldThrowException() {
    HelmRepository helmRepository = mock(HelmRepository.class);
    // Given
    File file = new File("test");
    when(helmRepository.getType()).thenReturn(null);
    // When
    final IllegalArgumentException result = assertThrows(IllegalArgumentException.class,
        () -> helmUploader.uploadSingle(file, helmRepository));
    // Then
    assertThat(result)
        .isNotNull()
        .hasMessage("Repository type missing. Check your plugin configuration.");
  }

  @Test
  public void uploadSingle_withServerErrorAndErrorStream_shouldThrowException() throws IOException {
    HelmRepository helmRepository = mock(HelmRepository.class);
    HttpURLConnection httpURLConnection = mock(HttpURLConnection.class);
    // Given
    File file = temporaryFolder.newFile("test.tmp");
    when(helmRepository.getType().createConnection((File) any(), helmRepository)).thenReturn(httpURLConnection);
    when(httpURLConnection.getResponseCode()).thenReturn(500);
    when(httpURLConnection.getErrorStream()).thenReturn(new ByteArrayInputStream("Server error in ES".getBytes()));
    when(httpURLConnection.getInputStream()).thenReturn(new ByteArrayInputStream("Server error in IS".getBytes()));

    // When
    final BadUploadException result = assertThrows(BadUploadException.class,
        () -> helmUploader.uploadSingle(file, helmRepository));
    // Then
    assertThat(result)
        .isNotNull()
        .hasMessage("Server error in ES");
  }

  @Test
  public void uploadSingle_withServerErrorAndInputStream_shouldThrowException() throws IOException {
    HelmRepository helmRepository = mock(HelmRepository.class,RETURNS_DEEP_STUBS);
    HttpURLConnection httpURLConnection = mock(HttpURLConnection.class);
    HelmRepository.HelmRepoType repoType = mock(HelmRepository.HelmRepoType.class);
    // Given
    File file = temporaryFolder.newFile("test.tmp");
    //when(helmRepository.getType().createConnection((File) any(), helmRepository)).thenReturn(httpURLConnection);
    when(helmRepository.getType()).thenReturn(repoType);
    when(repoType.createConnection((File) any(), helmRepository)).thenReturn(httpURLConnection);
    when(httpURLConnection.getResponseCode()).thenReturn(500);
    when(httpURLConnection.getErrorStream()).thenReturn(null);
    when(httpURLConnection.getInputStream()).thenReturn(new ByteArrayInputStream("Server error in IS".getBytes()));
    // When
    final BadUploadException result = assertThrows(BadUploadException.class,
        () -> helmUploader.uploadSingle(file, helmRepository));
    // Then
    assertThat(result)
        .isNotNull()
        .hasMessage("Server error in IS");
  }

  @Test
  public void uploadSingle_withServerError_shouldThrowException() throws IOException {
    HelmRepository helmRepository = mock(HelmRepository.class);
    HttpURLConnection httpURLConnection = mock(HttpURLConnection.class);
    // Given
    File file = temporaryFolder.newFile("test.tmp");
    when(helmRepository.getType().createConnection((File) any(), helmRepository)).thenReturn(httpURLConnection);
    when(httpURLConnection.getResponseCode()).thenReturn(500);
    when(httpURLConnection.getErrorStream()).thenReturn(null);
    when(httpURLConnection.getInputStream()).thenReturn(null);
    // When
    final BadUploadException result = assertThrows(BadUploadException.class,
        () -> helmUploader.uploadSingle(file, helmRepository));
    // Then
    assertThat(result)
        .isNotNull()
        .hasMessage("No details provided");
  }

  @Test
  public void uploadSingle_withCreatedStatus_shouldDisconnect()throws IOException, BadUploadException {
    HelmRepository helmRepository = mock(HelmRepository.class);
    HttpURLConnection httpURLConnection = mock(HttpURLConnection.class);
    // Given
    File file = temporaryFolder.newFile("test.tmp");
    when(helmRepository.getType().createConnection((File)any(), helmRepository)).thenReturn(httpURLConnection);
    when(httpURLConnection.getResponseCode()).thenReturn(201);
    when(httpURLConnection.getInputStream()).thenReturn(null);
    // When
    helmUploader.uploadSingle(file, helmRepository);
    // Then
    verify(httpURLConnection,times(1)).disconnect();
  }
}
