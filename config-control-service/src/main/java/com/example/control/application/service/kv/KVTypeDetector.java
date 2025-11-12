package com.example.control.application.service.kv;

import com.example.control.domain.model.kv.KVEntry;
import com.example.control.domain.model.kv.KVType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Heuristics for inferring the logical type of KV entries when explicit flags
 * are absent. This keeps backward compatibility with legacy keys that predate
 * type encoding.
 */
@Slf4j
@Component
public class KVTypeDetector {

    /**
     * Resolve type using the entry's flag metadata.
     */
    public KVType fromEntry(KVEntry entry) {
        if (entry == null) {
            return KVType.LEAF;
        }
        long flags = entry.flags();
        if (KVType.isTypeFlag(flags)) {
            return KVType.fromFlags(flags);
        }
        return KVType.LEAF;
    }

    /**
     * Resolve type from a collection of entries under a prefix and whether a
     * manifest exists.
     *
     * @param entries       entries under the prefix
     * @param manifestFound true when a manifest file is present
     * @return inferred type
     */
    public KVType fromChildren(List<KVEntry> entries, boolean manifestFound) {
        if (manifestFound) {
            return KVType.LIST;
        }
        if (entries == null || entries.isEmpty()) {
            return KVType.LEAF;
        }
        // Nested keys are treated as LEAF (previously would have been OBJECT)
        return KVType.LEAF;
    }
}


