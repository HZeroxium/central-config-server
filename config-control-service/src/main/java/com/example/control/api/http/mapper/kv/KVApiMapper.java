package com.example.control.api.http.mapper.kv;

import com.example.control.api.http.dto.kv.KVDtos;
import com.example.control.domain.model.KVEntry;
import com.example.control.domain.port.KVStorePort;
import com.example.control.infrastructure.adapter.kv.PrefixPolicy;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Mapper for KV API operations.
 * <p>
 * Provides mapping between domain objects and API DTOs.
 * </p>
 */
@Component
public final class KVApiMapper {

    /**
     * Map domain KVEntry to EntryResponse DTO.
     *
     * @param entry      the domain entry
     * @param serviceId  the service ID (for extracting relative path)
     * @param prefixPolicy the prefix policy
     * @return the response DTO
     */
    public static KVDtos.EntryResponse toEntryResponse(KVEntry entry, String serviceId, PrefixPolicy prefixPolicy) {
        String relativePath = prefixPolicy.extractRelativePath(serviceId, entry.key());
        return new KVDtos.EntryResponse(
                relativePath,
                entry.getValueAsBase64(),
                entry.modifyIndex(),
                entry.createIndex(),
                entry.flags()
        );
    }

    /**
     * Map list of domain entries to ListResponse DTO.
     *
     * @param entries     the domain entries
     * @param serviceId   the service ID
     * @param prefixPolicy the prefix policy
     * @return the response DTO
     */
    public static KVDtos.ListResponse toListResponse(List<KVEntry> entries, String serviceId, PrefixPolicy prefixPolicy) {
        List<KVDtos.EntryResponse> items = entries.stream()
                .map(entry -> toEntryResponse(entry, serviceId, prefixPolicy))
                .toList();
        return new KVDtos.ListResponse(items);
    }

    /**
     * Map PutRequest DTO to KVWriteOptions.
     *
     * @param request the put request
     * @return the write options
     */
    public static KVStorePort.KVWriteOptions toWriteOptions(KVDtos.PutRequest request) {
        return KVStorePort.KVWriteOptions.builder()
                .cas(request.cas())
                .flags(request.flags() != null ? request.flags() : 0L)
                .build();
    }

    /**
     * Map KVWriteResult to WriteResponse DTO.
     *
     * @param result the write result
     * @return the response DTO
     */
    public static KVDtos.WriteResponse toWriteResponse(KVStorePort.KVWriteResult result) {
        return new KVDtos.WriteResponse(result.success(), result.modifyIndex());
    }

    /**
     * Map KVDeleteResult to DeleteResponse DTO.
     *
     * @param result the delete result
     * @return the response DTO
     */
    public static KVDtos.DeleteResponse toDeleteResponse(KVStorePort.KVDeleteResult result) {
        return new KVDtos.DeleteResponse(result.success());
    }
}

