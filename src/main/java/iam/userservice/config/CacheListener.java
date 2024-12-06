package iam.userservice.config;

import com.hazelcast.core.EntryEvent;
import com.hazelcast.core.EntryListener;
import com.hazelcast.map.MapEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import static iam.userservice.service.UserService.USERS;

@Component
@Slf4j
public class CacheListener implements EntryListener<Object, Object> {

    @Override
    public void entryAdded(EntryEvent<Object, Object> entryEvent) {
        logEntryEvent("has been added", entryEvent);
    }

    @Override
    public void entryEvicted(EntryEvent<Object, Object> entryEvent) {
        logEntryEvent("has been evicted", entryEvent);
    }

    @Override
    public void entryExpired(EntryEvent<Object, Object> entryEvent) {
        logEntryEvent("has expired", entryEvent);
    }

    @Override
    public void entryRemoved(EntryEvent<Object, Object> entryEvent) {
        logEntryEvent("has been removed", entryEvent);
    }

    @Override
    public void entryUpdated(EntryEvent<Object, Object> entryEvent) {
        logEntryEvent("has been updated", entryEvent);
    }

    @Override
    public void mapCleared(MapEvent mapEvent) {
        log.info("Cache has been cleared: {}", mapEvent.getName());
    }

    @Override
    public void mapEvicted(MapEvent mapEvent) {
        log.info("Cache has been evicted: {}", mapEvent.getName());
    }

    private void logEntryEvent(String action, EntryEvent<Object, Object> event) {
        String keyType = USERS.equals(event.getName()) ? "key= userId" : "key";
        log.info("Cache entry {} [map= '{}', {} '{}']",
                action,
                event.getName(),
                keyType,
                event.getKey());
    }


}
