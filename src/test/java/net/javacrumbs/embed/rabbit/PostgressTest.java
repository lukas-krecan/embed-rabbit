package net.javacrumbs.embed.rabbit;


import org.junit.Test;
import ru.yandex.qatools.embed.postgresql.PostgresExecutable;
import ru.yandex.qatools.embed.postgresql.PostgresProcess;
import ru.yandex.qatools.embed.postgresql.PostgresStarter;
import ru.yandex.qatools.embed.postgresql.config.PostgresConfig;

import java.io.IOException;

public class PostgressTest {
    @Test
    public void testPostgress() throws IOException {
        PostgresStarter<PostgresExecutable, PostgresProcess> runtime = PostgresStarter.getDefaultInstance();
        final PostgresConfig config = PostgresConfig.defaultWithDbName("test");
        PostgresExecutable exec = runtime.prepare(config);
        PostgresProcess process = exec.start();

        process.stop();
    }

}
