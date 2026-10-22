package com.luairan.business;

import com.luairan.service.context.State;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.*;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(name = "paxos.business.enabled", havingValue = "true")
public class BusinessConfiguration {
    @Bean(destroyMethod = "close")
    @ConfigurationProperties("paxos.business.datasource")
    HikariDataSource businessDataSource() { return new HikariDataSource(); }
    @Bean FencedStore fencedStore(HikariDataSource dataSource,
            @Value("${paxos.business.namespace:paxos-demo}") String namespace,
            @Value("${paxos.business.initialize-schema:false}") boolean initializeSchema) {
        if (initializeSchema) new ResourceDatabasePopulator(new ClassPathResource("db/fencing.sql")).execute(dataSource);
        return new JdbcFencedStore(dataSource, namespace);
    }
    @Bean FencedWriteService fencedWriteService(State state, FencedStore store) { return new FencedWriteService(state, store); }
}
