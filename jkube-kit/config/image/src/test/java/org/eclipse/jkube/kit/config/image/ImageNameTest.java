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
package org.eclipse.jkube.kit.config.image;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import org.junit.Test;

public class ImageNameTest {

    @Test
    public void simple() {

        Object[] data = {
                "jolokia/jolokia_demo",
                r().repository("jolokia/jolokia_demo")
                   .fullName("jolokia/jolokia_demo").fullNameWithTag("jolokia/jolokia_demo:latest").simpleName("jolokia_demo").tag("latest"),

                "jolokia/jolokia_demo:0.9.6",
                r().repository("jolokia/jolokia_demo").tag("0.9.6")
                        .fullName("jolokia/jolokia_demo").fullNameWithTag("jolokia/jolokia_demo:0.9.6").simpleName("jolokia_demo"),

                "test.org/jolokia/jolokia_demo:0.9.6",
                r().registry("test.org").repository("jolokia/jolokia_demo").tag("0.9.6")
                        .fullName("test.org/jolokia/jolokia_demo").fullNameWithTag("test.org/jolokia/jolokia_demo:0.9.6").simpleName("jolokia_demo"),

                "test.org/jolokia/jolokia_demo",
                r().registry("test.org").repository("jolokia/jolokia_demo")
                        .fullName("test.org/jolokia/jolokia_demo").fullNameWithTag("test.org/jolokia/jolokia_demo:latest").simpleName("jolokia_demo").tag("latest"),

                "test.org:8000/jolokia/jolokia_demo:8.0",
                r().registry("test.org:8000").repository("jolokia/jolokia_demo").tag("8.0")
                        .fullName("test.org:8000/jolokia/jolokia_demo").fullNameWithTag("test.org:8000/jolokia/jolokia_demo:8.0").simpleName("jolokia_demo"),

                "jolokia_demo",
                r().repository("jolokia_demo")
                        .fullName("jolokia_demo").fullNameWithTag("jolokia_demo:latest").simpleName("jolokia_demo").tag("latest"),

                "jolokia_demo:0.9.6",
                r().repository("jolokia_demo").tag("0.9.6")
                        .fullName("jolokia_demo").fullNameWithTag("jolokia_demo:0.9.6").simpleName("jolokia_demo"),

                "consol/tomcat-8.0:8.0.9",
                r().repository("consol/tomcat-8.0").tag("8.0.9")
                        .fullName("consol/tomcat-8.0").fullNameWithTag("consol/tomcat-8.0:8.0.9").simpleName("tomcat-8.0"),

                "test.org/user/subproject/image:latest",
                r().registry("test.org").repository("user/subproject/image").tag("latest")
                        .fullName("test.org/user/subproject/image").fullNameWithTag("test.org/user/subproject/image:latest").simpleName("subproject/image")

        };

        verifyData(data);
    }

    @Test
    public void testMultipleSubComponents() {
        Object[] data = {
                "org/jolokia/jolokia_demo",
                r().repository("org/jolokia/jolokia_demo")
                        .fullName("org/jolokia/jolokia_demo").fullNameWithTag("org/jolokia/jolokia_demo:latest").simpleName("jolokia/jolokia_demo").tag("latest"),

                "org/jolokia/jolokia_demo:0.9.6",
                r().repository("org/jolokia/jolokia_demo").tag("0.9.6")
                        .fullName("org/jolokia/jolokia_demo").fullNameWithTag("org/jolokia/jolokia_demo:0.9.6").simpleName("jolokia/jolokia_demo"),

                "repo.example.com/org/jolokia/jolokia_demo:0.9.6",
                r().registry("repo.example.com").repository("org/jolokia/jolokia_demo").tag("0.9.6")
                        .fullName("repo.example.com/org/jolokia/jolokia_demo").fullNameWithTag("repo.example.com/org/jolokia/jolokia_demo:0.9.6").simpleName("jolokia/jolokia_demo"),

                "repo.example.com/org/jolokia/jolokia_demo",
                r().registry("repo.example.com").repository("org/jolokia/jolokia_demo")
                        .fullName("repo.example.com/org/jolokia/jolokia_demo").fullNameWithTag("repo.example.com/org/jolokia/jolokia_demo:latest").simpleName("jolokia/jolokia_demo").tag("latest"),

                "repo.example.com:8000/org/jolokia/jolokia_demo:8.0",
                r().registry("repo.example.com:8000").repository("org/jolokia/jolokia_demo").tag("8.0")
                        .fullName("repo.example.com:8000/org/jolokia/jolokia_demo").fullNameWithTag("repo.example.com:8000/org/jolokia/jolokia_demo:8.0").simpleName("jolokia/jolokia_demo"),
                "org/jolokia_demo",
                r().repository("org/jolokia_demo")
                        .fullName("org/jolokia_demo").fullNameWithTag("org/jolokia_demo:latest").simpleName("jolokia_demo").tag("latest"),

                "org/jolokia_demo:0.9.6",
                r().repository("org/jolokia_demo").tag("0.9.6")
                        .fullName("org/jolokia_demo").fullNameWithTag("org/jolokia_demo:0.9.6").simpleName("jolokia_demo"),
        };

        verifyData(data);
    }

    private void verifyData(Object[] data) {
        for (int i = 0; i < data.length; i += 2) {
            ImageName name = new ImageName((String) data[i]);
            Res res = (Res) data[i + 1];
            assertEquals("Registry " + i,res.registry,name.getRegistry());
            assertEquals("Repository " + i,res.repository,name.getRepository());
            assertEquals("Tag " + i,res.tag,name.getTag());
            assertEquals("RepoWithRegistry " + i,res.fullName, name.getNameWithoutTag(null));
            assertEquals("FullName " + i,res.fullNameWithTag, name.getFullName(null));
            assertEquals("Simple Name " + i,res.simpleName, name.getSimpleName());
        }
    }

    @Test
    public void testRegistryNaming() {
        assertEquals("docker.jolokia.org/jolokia/jolokia_demo:0.18",
                     new ImageName("jolokia/jolokia_demo:0.18").getFullName("docker.jolokia.org"));
        assertEquals("docker.jolokia.org/jolokia/jolokia_demo:latest",
                     new ImageName("jolokia/jolokia_demo").getFullName("docker.jolokia.org"));
        assertEquals("jolokia/jolokia_demo:latest",
                     new ImageName("jolokia/jolokia_demo").getFullName(null));
        assertEquals("docker.jolokia.org/jolokia/jolokia_demo:latest",
                     new ImageName("docker.jolokia.org/jolokia/jolokia_demo").getFullName("another.registry.org"));
        assertEquals("docker.jolokia.org/jolokia/jolokia_demo:latest",
                     new ImageName("docker.jolokia.org/jolokia/jolokia_demo").getFullName(null));
    }

    @Test
    public void testRegistryNamingExtended() {
        assertEquals("docker.jolokia.org/org/jolokia/jolokia_demo:0.18",
                new ImageName("org/jolokia/jolokia_demo:0.18").getFullName("docker.jolokia.org"));
        assertEquals("docker.jolokia.org/org/jolokia/jolokia_demo:latest",
                new ImageName("org/jolokia/jolokia_demo").getFullName("docker.jolokia.org"));
        assertEquals("org/jolokia/jolokia_demo:latest",
                new ImageName("org/jolokia/jolokia_demo").getFullName(null));
        assertEquals("docker.jolokia.org/org/jolokia/jolokia_demo:latest",
                new ImageName("docker.jolokia.org/org/jolokia/jolokia_demo").getFullName("another.registry.org"));
        assertEquals("docker.jolokia.org/org/jolokia/jolokia_demo:latest",
                new ImageName("docker.jolokia.org/org/jolokia/jolokia_demo").getFullName(null));
    }


    @Test(expected = IllegalArgumentException.class)
    public void testIllegalFormat() {
        new ImageName("");
    }

    @Test
    public void namesUsedByDockerTests() {
        StringBuilder longTag = new StringBuilder();
        for (int i = 0; i < 130; i++) {
            longTag.append("a");
        }
        String[] illegal = {
            "fo$z$", "Foo@3cc", "Foo$3", "Foo*3", "Fo^3", "Foo!3", "F)xcz(", "fo%asd", "FOO/bar",
            "repo:fo$z$", "repo:Foo@3cc", "repo:Foo$3", "repo:Foo*3", "repo:Fo^3", "repo:Foo!3",
            "repo:%goodbye", "repo:#hashtagit", "repo:F)xcz(", "repo:-foo", "repo:..","repo:" + longTag.toString(),
            "-busybox:test", "-test/busybox:test", "-index:5000/busybox:test"

        };

        for (String i : illegal) {
            try {
                new ImageName(i);
                fail(String.format("Name '%s' should fail", i));
            } catch (IllegalArgumentException exp) { /* expected */}
        }

        String[] legal = {
            "fooo/bar", "fooaa/test", "foooo:t", "HOSTNAME.DOMAIN.COM:443/foo/bar"
        };

        for (String l : legal) {
            new ImageName(l);
        }
    }

    @Test
    public void testImageNameWithUsernameHavingPeriods() {
        // Given
        String name = "roman.gordill/customer-service-cache:latest";

        // When
        ImageName imageName = new ImageName(name);

        // Then
        assertNotNull(imageName);
        assertEquals("roman.gordill", imageName.getUser());
        assertEquals("roman.gordill/customer-service-cache", imageName.getRepository());
        assertEquals("latest", imageName.getTag());
        assertNull(imageName.getRegistry());
    }

    @Test
    public void testImageNameWithNameContainingRegistryAndName() {
        // Given
        String name = "foo.com:5000/customer-service-cache:latest";

        // When
        ImageName imageName = new ImageName(name);

        // Then
        assertNotNull(imageName);
        assertNull(imageName.getUser());
        assertEquals("foo.com:5000", imageName.getRegistry());
        assertEquals("customer-service-cache", imageName.getRepository());
        assertEquals("latest", imageName.getTag());
    }

    // =======================================================================================
    private static Res r() {
        return new Res();
    }

    private static class Res {
        private String registry,repository,tag, fullName, fullNameWithTag, simpleName;
        boolean hasRegistry = false;

        Res registry(String registry) {
            this.registry = registry;
            this.hasRegistry = registry != null;
            return this;
        }

        Res repository(String repository) {
            this.repository = repository;
            return this;
        }

        Res tag(String tag) {
            this.tag = tag;
            return this;
        }

        Res fullName(String fullName) {
            this.fullName = fullName;
            return this;
        }

        Res fullNameWithTag(String fullNameWithTag) {
            this.fullNameWithTag = fullNameWithTag;
            return this;
        }

        Res simpleName(String simpleName) {
            this.simpleName = simpleName;
            return this;
        }
    }
}