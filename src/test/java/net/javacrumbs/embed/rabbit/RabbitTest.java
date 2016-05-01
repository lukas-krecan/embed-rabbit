package net.javacrumbs.embed.rabbit;

import org.junit.Test;

import java.io.IOException;

public class RabbitTest {
    @Test
    //TODO port, config
    public void testRabbit() throws IOException {
        RabbitMqServerStarter starter = RabbitMqServerStarter.getDefaultInstance();

        IRabbitMqServerConfig rabbitConfig = new RabbitMqServerConfigBuilder()
                .version(Version.V_3_6_1)
                .build();

        RabbitMqServerExecutable executable = null;
        try {
            executable = starter.prepare(rabbitConfig);
            RabbitMqServerProcess rabbit = executable.start();

        } finally {
            if (executable != null)
                executable.stop();
        }
    }

}
