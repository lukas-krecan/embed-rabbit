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

import de.flapdoodle.embed.process.config.store.FileSet;
import de.flapdoodle.embed.process.config.store.FileType;
import de.flapdoodle.embed.process.config.store.IDownloadConfig;
import de.flapdoodle.embed.process.config.store.IPackageResolver;
import de.flapdoodle.embed.process.distribution.ArchiveType;
import de.flapdoodle.embed.process.distribution.Distribution;
import de.flapdoodle.embed.process.extract.*;
import de.flapdoodle.embed.process.io.directories.IDirectory;
import de.flapdoodle.embed.process.io.file.Files;
import de.flapdoodle.embed.process.store.*;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;

/**
 * Hack taken from PostgreSQL
 */
class RabbitMqFilesToExtract extends FilesToExtract {
    final FileSet fileSet;
    final IDirectory extractDir;

    public RabbitMqFilesToExtract(IDirectory dirFactory, ITempNaming executableNaming, FileSet fileSet) {
        super(dirFactory, executableNaming, fileSet);
        this.fileSet = fileSet;
        this.extractDir = dirFactory;
    }

    /**
     * This is actually the very dirty hack method to adopt the Flapdoodle's API to the compatible way to extract and run
     * TODO: very very hacky method!
     */
    @Override
    public IExtractionMatch find(final IArchiveEntry entry) {
        return new IExtractionMatch() {
            @Override
            public File write(InputStream source, long size) throws IOException {
                if (extractDir == null || extractDir.asFile() == null) {
                    return null;
                }

                final String basePath = extractDir.asFile().getPath();
                final File outputFile = Paths.get(basePath, entry.getName()).toFile();
                if (entry.isDirectory()) {
                    if (!outputFile.exists()) {
                        Files.createDir(outputFile);
                    }
                } else {
                    if (!outputFile.exists()) { // prevent double extraction (for other binaries)
                        Files.write(source, outputFile);
                    }
                    // hack to mark binaries as executable
                    if ((entry.getName().matches(".+/sbin/.+"))) {
                        outputFile.setExecutable(true);
                    }
                }
                return outputFile;
            }

            @Override
            public FileType type() {
                // does this archive entry match to any of the provided fileset entries?
                for (FileSet.Entry matchingEntry : fileSet.entries()) {
                    if (matchingEntry.matchingPattern().matcher(entry.getName()).matches()) {
                        return matchingEntry.type();
                    }
                }
                // Otherwise - it's just an library file
                return FileType.Library;
            }
        };
    }

    /**
     * Just to use RabbitMqFilesToExtract
     */
    static class RabbitMqArtifactStoreBuilder extends ArtifactStoreBuilder {
        @Override
       	public IArtifactStore build() {
       		boolean useCache = get(USE_CACHE, true);

       		IArtifactStore artifactStore;

       		artifactStore = new RabbitMqArtifactStore(get(DOWNLOAD_CONFIG), get(TEMP_DIR_FACTORY), get(EXECUTABLE_NAMING), get(DOWNLOADER));

       		if (useCache) {
       			artifactStore = new CachingArtifactStore(artifactStore);
       		}

       		return artifactStore;
       	}
    }

    static class RabbitMqArtifactStore extends ArtifactStore {
        private IDownloadConfig _downloadConfig;
           private IDirectory _tempDirFactory;
           private ITempNaming _executableNaming;

           public RabbitMqArtifactStore(IDownloadConfig downloadConfig, IDirectory tempDirFactory, ITempNaming executableNaming, IDownloader downloader) {
               super(downloadConfig, tempDirFactory, executableNaming, downloader);
               _downloadConfig = downloadConfig;
               _tempDirFactory = tempDirFactory;
               _executableNaming = executableNaming;
           }

           private static File getArtifact(IDownloadConfig runtime, Distribution distribution) {
               File dir = createOrGetBaseDir(runtime);
               File artifactFile = new File(dir, runtime.getPackageResolver().getPath(distribution));
               if ((artifactFile.exists()) && (artifactFile.isFile()))
                   return artifactFile;
               return null;
           }

           private static File createOrGetBaseDir(IDownloadConfig runtime) {
               File dir = runtime.getArtifactStorePath().asFile();
               createOrCheckDir(dir);
               return dir;
           }

           private static void createOrCheckDir(File dir) {
               if (!dir.exists()) {
                   if (!dir.mkdirs())
                       throw new IllegalArgumentException("Could NOT create Directory " + dir);
               }
               if (!dir.isDirectory())
                   throw new IllegalArgumentException("" + dir + " is not a Directory");
           }

           public IDirectory getTempDir() {
               return _tempDirFactory;
           }

           @Override
           public void removeFileSet(Distribution distribution, IExtractedFileSet all) {
               try {
                   super.removeFileSet(distribution, all);
               } catch (IllegalArgumentException e) {
                   System.err.println("Failed to remove file set: " + e.getMessage());//NOSONAR
               }
           }

           /**
            * Actually this entirely class does exist because of this method only!
            * TODO: Look for the more native way to override the default FilesToExtract strategy
            */
           @Override
           public IExtractedFileSet extractFileSet(Distribution distribution) throws IOException {
               IPackageResolver packageResolver = _downloadConfig.getPackageResolver();
               File artifact = getArtifact(_downloadConfig, distribution);
               final ArchiveType archiveType = packageResolver.getArchiveType(distribution);
               IExtractor extractor = Extractors.getExtractor(archiveType);
               try {
                   final FileSet fileSet = packageResolver.getFileSet(distribution);
                   return extractor.extract(_downloadConfig, artifact, new RabbitMqFilesToExtract(_tempDirFactory, _executableNaming, fileSet));
               } catch (Exception e) {
                  throw new IOException(e);
               }
           }

    }

}
