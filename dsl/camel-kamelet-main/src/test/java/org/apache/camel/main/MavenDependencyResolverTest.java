/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.camel.main;

import java.util.List;
import java.util.function.Predicate;

import org.apache.camel.main.download.MavenDependencyDownloader;
import org.apache.camel.tooling.maven.MavenArtifact;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Disabled("Manual test")
public class MavenDependencyResolverTest {

    public static final Logger LOG = LoggerFactory.getLogger(MavenDependencyResolverTest.class);

    @Test
    public void testDownload() throws Exception {
        List<String> deps = List.of("org.apache.camel:camel-core:3.17.0");
        try (MavenDependencyDownloader downloader = new MavenDependencyDownloader()) {
            // build() creates own DIRegistry and configures the resolver/aether stuff  through
            // org.apache.camel:camel-tooling-maven
            downloader.build();

            List<MavenArtifact> answer = downloader.resolveDependenciesViaAether(deps, null, true, false);
            Assertions.assertNotNull(answer);
            assertTrue(answer.size() > 15);
            for (MavenArtifact ma : answer) {
                LOG.info("Artifact (transitive): {}", ma);
            }

            answer = downloader.resolveDependenciesViaAether(deps, null, false, false);
            Assertions.assertNotNull(answer);
            assertEquals(1, answer.size());
            for (MavenArtifact ma : answer) {
                LOG.info("Artifact (non-transitive): {}", ma);
            }
        }
    }

    @Test
    public void testGeneratePluginUsesCorrectTransitiveDependencies() throws Exception {
        List<String> deps = List.of("org.apache.camel:camel-jbang-plugin-generate:4.8.0");
        Predicate<MavenArtifact> artifactFilter = mavenArtifact -> "jackson-datatype-guava"
                .equals(mavenArtifact.getGav().getArtifactId());
        try (MavenDependencyDownloader downloader = new MavenDependencyDownloader()) {
            downloader.build();
            List<MavenArtifact> answer = downloader.resolveDependenciesViaAether(deps, null,
                    true, false);
            Assertions.assertNotNull(answer);
            Assertions.assertTrue(answer.stream().anyMatch(artifactFilter),
                    "check jackson-datatype-guava is present in transitive dependencies");
            //jackson version from Camel 4.8.0 parent should be 2.17.2
            String expectedVersion = "2.17.2";
            Assertions.assertNotEquals(expectedVersion, answer.stream().filter(artifactFilter)
                    .findFirst().get().getGav().getVersion(),
                    "check jackson-datatype-guava version without parent");

            //resolve the dependencies with parent
            answer = downloader.resolveDependenciesViaAether("org.apache.camel:camel-jbang-parent:4.8.0",
                    deps, null, true, false);
            Assertions.assertNotNull(answer);
            Assertions.assertTrue(answer.stream().anyMatch(artifactFilter),
                    "check jackson-datatype-guava is present in transitive dependencies");
            Assertions.assertEquals(expectedVersion, answer.stream().filter(artifactFilter)
                    .findFirst().get().getGav().getVersion(),
                    "check jackson-datatype-guava version with parent");
        }
    }

}
