/**
 * Copyright 2009-2016 the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.javacrumbs.embed.rabbit;

import de.flapdoodle.embed.process.config.IRuntimeConfig;
import de.flapdoodle.embed.process.config.RuntimeConfigBuilder;
import de.flapdoodle.embed.process.config.io.ProcessOutput;
import de.flapdoodle.embed.process.config.store.*;
import de.flapdoodle.embed.process.distribution.ArchiveType;
import de.flapdoodle.embed.process.distribution.Distribution;
import de.flapdoodle.embed.process.extract.IExtractedFileSet;
import de.flapdoodle.embed.process.extract.UUIDTempNaming;
import de.flapdoodle.embed.process.io.directories.PropertyOrPlatformTempDir;
import de.flapdoodle.embed.process.io.directories.UserHome;
import de.flapdoodle.embed.process.io.progress.StandardConsoleProgressListener;
import de.flapdoodle.embed.process.runtime.ICommandLinePostProcessor;
import de.flapdoodle.embed.process.runtime.Starter;
import de.flapdoodle.embed.process.store.Downloader;

public class RabbitMqServerStarter extends Starter<IRabbitMqServerConfig, RabbitMqServerExecutable, RabbitMqServerProcess> {

    private static final String RABBITMQ_SERVER_EXECUTABLE = "rabbitmq-server";
    private static final String DIR_NAME = ".embed-rabbit";

    private RabbitMqServerStarter(IRuntimeConfig config) {
        super(config);
    }

    public static RabbitMqServerStarter getInstance(IRuntimeConfig config) {
        return new RabbitMqServerStarter(config);
    }

    public static RabbitMqServerStarter getDefaultInstance() {
        return getInstance(new RabbitMqServerRuntimeConfigBuilder().defaults().build());
    }

    @Override
    protected RabbitMqServerExecutable newExecutable(IRabbitMqServerConfig rabbitMqServerConfig, Distribution distribution, IRuntimeConfig runtime, IExtractedFileSet files) {
        return new RabbitMqServerExecutable(distribution, rabbitMqServerConfig, runtime, files);
    }

    private static class RabbitMqServerRuntimeConfigBuilder extends RuntimeConfigBuilder {
        RabbitMqServerRuntimeConfigBuilder defaults() {
            processOutput().setDefault(ProcessOutput.getDefaultInstance(RABBITMQ_SERVER_EXECUTABLE)); // TODO log
            commandLinePostProcessor().setDefault(new ICommandLinePostProcessor.Noop());
            artifactStore().setDefault(
                    new RabbitMqFilesToExtract.RabbitMqArtifactStoreBuilder()
                            .tempDir(new PropertyOrPlatformTempDir())
                            .executableNaming(new UUIDTempNaming())
                            .download(new RabbitDownloadConfigBuilder().defaults().build())
                            .downloader(new Downloader())
                            .build()
            );
            return this;
        }
    }


    private static class RabbitDownloadConfigBuilder extends DownloadConfigBuilder {
        public RabbitDownloadConfigBuilder defaults() {
            fileNaming().setDefault(new UUIDTempNaming());
            downloadPath().setDefault(new DownloadPath("https://www.rabbitmq.com/releases/rabbitmq-server/"));
            progressListener().setDefault(new StandardConsoleProgressListener());
            artifactStorePath().setDefault(new UserHome(DIR_NAME));
            downloadPrefix().setDefault(new DownloadPrefix("embed-rabbit-download"));
            userAgent().setDefault(new UserAgent("Mozilla/5.0 (compatible; Embedded RabbitMQ; +https://github.com/lukas-krecan/embed-rabbit)"));
            packageResolver(new RabbitPackageResolver());
            return this;
        }
    }

    private static class RabbitPackageResolver implements IPackageResolver {

        @Override
        public FileSet getFileSet(Distribution distribution) {
            //FIXME: Windows
            return FileSet.builder()
                    .addEntry(FileType.Executable, RABBITMQ_SERVER_EXECUTABLE)
                    .build();
        }

        @Override
        public ArchiveType getArchiveType(Distribution distribution) {
            //FIXME: Windows
            return ArchiveType.TXZ;
        }

        @Override
        public String getPath(Distribution distribution) {
            //FIXME: Windows
            String version = distribution.getVersion().asInDownloadPath();
            return "v" + version + "/" + "rabbitmq-server-generic-unix-" + version + ".tar.xz";
        }
    }
}