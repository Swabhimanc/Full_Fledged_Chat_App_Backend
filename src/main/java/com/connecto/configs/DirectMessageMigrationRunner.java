package com.connecto.configs;

import com.connecto.repositories.OneToOneMessageRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.migrations.direct-messages-enabled", havingValue = "true")
public class DirectMessageMigrationRunner implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(DirectMessageMigrationRunner.class);

    private final OneToOneMessageRepository repository;

    public DirectMessageMigrationRunner(OneToOneMessageRepository repository) {
        this.repository = repository;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        int migrated = repository.migrateAllConversations();
        log.info("Direct-message migration completed: {} conversation(s) migrated", migrated);
    }
}
