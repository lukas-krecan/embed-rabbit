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

import de.flapdoodle.embed.process.collections.Collections;
import de.flapdoodle.embed.process.config.IRuntimeConfig;
import de.flapdoodle.embed.process.config.io.ProcessOutput;
import de.flapdoodle.embed.process.distribution.Distribution;
import de.flapdoodle.embed.process.extract.IExtractedFileSet;
import de.flapdoodle.embed.process.io.LoggingOutputStreamProcessor;
import de.flapdoodle.embed.process.io.directories.IDirectory;
import de.flapdoodle.embed.process.io.progress.LoggingProgressListener;
import de.flapdoodle.embed.process.runtime.AbstractProcess;
import de.flapdoodle.embed.process.runtime.Executable;
import de.flapdoodle.embed.process.runtime.IStopable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;

import static de.flapdoodle.embed.process.io.file.Files.forceDelete;

public class RabbitMqServerProcess extends AbstractProcess<IRabbitMqServerConfig, RabbitMqServerExecutable, RabbitMqServerProcess> implements IStopable {
    private static Logger logger = LoggerFactory.getLogger(RabbitMqServerProcess.class);
    private boolean stopped = false;
    private final IRuntimeConfig runtimeConfig;

    public RabbitMqServerProcess(Distribution distribution, IRabbitMqServerConfig config, IRuntimeConfig runtime, RabbitMqServerExecutable executable) throws IOException {
        super(distribution, config, runtime, executable);
        runtimeConfig = runtimeConfig;
    }

    @Override
    protected List<String> getCommandLine(Distribution distribution, IRabbitMqServerConfig rabbitMqServerConfig, IExtractedFileSet exe) throws IOException {
        List<String> commandLine = Collections.newArrayList(
                exe.executable().getAbsolutePath()
        );
        return commandLine;
    }

    @Override
    protected void stopInternal() {
        synchronized (this) {
            if (!stopped) {
                stopped = true;
                logger.info("trying to stop postgresql");
                if (!sendStopToPostgresqlInstance()) {
                    logger.warn("could not stop postgresql with command, try next");
                    if (!sendKillToProcess()) {
                        logger.warn("could not stop postgresql, try next");
                        if (!sendTermToProcess()) {
                            logger.warn("could not stop postgresql, try next");
                            if (!tryKillToProcess()) {
                                logger.warn("could not stop postgresql the second time, try one last thing");
                            }
                        }
                    }
                }
            }
            deleteTempFiles();
        }
    }

    protected final boolean sendStopToRabbitInstance() {
        final boolean result = shutdownRabbit(getConfig());
        if (runtimeConfig.getArtifactStore() instanceof RabbitMqFilesToExtract.RabbitMqArtifactStore) {
            final IDirectory tempDir = ((RabbitMqFilesToExtract.RabbitMqArtifactStore) runtimeConfig.getArtifactStore()).getTempDir();
            if (tempDir != null && tempDir.asFile() != null) {
                logger.info("Cleaning up after the embedded process (removing {})...", tempDir.asFile().getAbsolutePath()))
                ;
                forceDelete(tempDir.asFile());
            }
        }
        return result;
    }

    private boolean shutdownRabbit(IRabbitMqServerConfig config) {
        try {
            return runCmd(config, Command.PgCtl, "server stopped", 1000, "stop");
        } catch (Exception e) {
            logger.warn("Failed to stop postgres by pg_ctl!");
        }
        return false;
    }

    private static <P extends AbstractRabbitMqProcess> boolean runCmd(
            IRabbitMqServerConfig config, Command cmd, String successOutput, int timoeut, String... args) {
        return runCmd(config, cmd, successOutput, java.util.Collections.<String>emptySet(), timoeut, args);
    }

    private static <P extends AbstractRabbitMqProcess> boolean runCmd(
            PostgresConfig config, Command cmd, String successOutput, Set<String> failOutput, long timeout, String... args) {
        try {
            LogWatchStreamProcessor logWatch = new LogWatchStreamProcessor(successOutput,
                    failOutput, new LoggingOutputStreamProcessor(logger, Level.ALL));
            final RuntimeConfigBuilder rtConfigBuilder = new RuntimeConfigBuilder().defaults(cmd);
            IRuntimeConfig runtimeConfig = rtConfigBuilder
                    .processOutput(new ProcessOutput(logWatch, logWatch, logWatch))
                    .artifactStore(new ArtifactStoreBuilder().defaults(cmd)
                            .download(new DownloadConfigBuilder().defaultsForCommand(cmd)
                                    .progressListener(
                                            new LoggingProgressListener(logger, Level.ALL)).build()))
                    .build();
            Executable exec = getCommand(cmd, runtimeConfig)
                    .prepare(new PostgresConfig(config).withArgs(args));
            P proc = (P) exec.start();
            logWatch.waitForResult(timeout);
            proc.waitFor();
            return true;
        } catch (IOException | InterruptedException e) {
            logger.log(Level.WARNING, e.getMessage());
        }
        return false;
    }

    protected void deleteTempFiles() {
        //FIXME: delete rabbit data
    }

    @Override
    protected void cleanupInternal() {

    }
}
