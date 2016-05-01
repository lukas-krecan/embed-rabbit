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

import de.flapdoodle.embed.process.builder.AbstractBuilder;
import de.flapdoodle.embed.process.builder.TypedProperty;
import de.flapdoodle.embed.process.config.ISupportConfig;
import de.flapdoodle.embed.process.distribution.IVersion;

public class RabbitMqServerConfigBuilder extends AbstractBuilder<IRabbitMqServerConfig> {
    private static final TypedProperty<Version> VERSION = TypedProperty.with("Version", Version.class);

    public RabbitMqServerConfigBuilder version(Version version) {
        property(VERSION).set(version);
   		return this;
   	}

    @Override
    public IRabbitMqServerConfig build() {
        return new ImmutableRabbitMqServerConfig(property(VERSION).get());
    }

    private static class ImmutableRabbitMqServerConfig implements  IRabbitMqServerConfig {
        private final Version version;
        private final ISupportConfig supportConfig;

        private ImmutableRabbitMqServerConfig(Version version) {
            this.version = version;
            this.supportConfig = new SupportConfig();
        }

        @Override
        public IVersion version() {
            return version;
        }

        @Override
        public ISupportConfig supportConfig() {
            return supportConfig;
        }
    }

    private static class SupportConfig implements ISupportConfig {
        @Override
        public String getName() {
            throw new UnsupportedOperationException();
        }

        @Override
        public String getSupportUrl() {
            throw new UnsupportedOperationException();
        }

        @Override
        public String messageOnException(Class<?> context, Exception exception) {
            return null;
        }
    }
}
