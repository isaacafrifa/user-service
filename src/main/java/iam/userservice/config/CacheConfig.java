package iam.userservice.config;

import com.hazelcast.core.HazelcastInstance;
import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Configuration;

import static iam.userservice.service.UserService.USERS;


@Configuration
public class CacheConfig {
    private final HazelcastInstance hazelcastInstance;
    private final CacheListener cacheListener;

    public CacheConfig(HazelcastInstance hazelcastInstance, CacheListener cacheListener) {
        this.hazelcastInstance = hazelcastInstance;
        this.cacheListener = cacheListener;
    }

    @PostConstruct
    public void configureCacheListener() {
        hazelcastInstance.getMap(USERS).addEntryListener(cacheListener, true);
    }
}
