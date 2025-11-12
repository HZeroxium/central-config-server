package com.example.control.api.http.mapper.kv;

import com.example.control.api.http.dto.kv.KVDtos;
import com.example.control.api.http.dto.kv.KVDtos.ListResponseV2.ListItem;
import com.example.control.domain.model.kv.KVEntry;
import com.example.control.domain.model.kv.KVListManifest;
import com.example.control.domain.model.kv.KVListStructure;
import com.example.control.domain.model.kv.KVTransactionOperation;
import com.example.control.domain.model.kv.KVTransactionRequest;
import com.example.control.domain.model.kv.KVTransactionResponse;
import com.example.control.domain.model.kv.KVType;
import com.example.control.domain.port.KVStorePort;
import com.example.control.infrastructure.adapter.kv.PrefixPolicy;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

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

    public static KVDtos.ListResponseV2 toListResponse(KVListStructure structure) {
        List<ListItem> items = structure.items().stream()
                .map(item -> new ListItem(item.id(), item.data()))
                .toList();
        return new KVDtos.ListResponseV2(items, toManifestDto(structure.manifest()), structure.type().name());
    }

    public static KVDtos.ListManifest toManifestDto(KVListManifest manifest) {
        return new KVDtos.ListManifest(
                manifest.order(),
                manifest.version(),
                manifest.etag(),
                manifest.metadata()
        );
    }

    public static KVListManifest toManifest(KVDtos.ListManifest dto) {
        if (dto == null) {
            return KVListManifest.empty();
        }
        return new KVListManifest(
                dto.order() != null ? dto.order() : List.of(),
                dto.version(),
                dto.etag(),
                dto.metadata() != null ? dto.metadata() : Map.of()
        );
    }

    public static KVListStructure toListStructure(KVDtos.ListWriteRequest request) {
        List<KVListStructure.Item> items = request.items() == null
                ? List.of()
                : request.items().stream()
                .map(item -> new KVListStructure.Item(item.id(), item.data()))
                .toList();
        return new KVListStructure(items, toManifest(request.manifest()), KVType.LIST);
    }

    public static KVTransactionRequest toTransactionRequest(String serviceId,
                                                             KVDtos.TransactionRequest request,
                                                             PrefixPolicy prefixPolicy) {
        List<KVTransactionOperation> operations = request.operations().stream()
                .map(op -> toTransactionOperation(serviceId, op, prefixPolicy))
                .toList();
        return new KVTransactionRequest(serviceId, operations);
    }

    private static KVTransactionOperation toTransactionOperation(String serviceId,
                                                                 KVDtos.TransactionRequest.Operation op,
                                                                 PrefixPolicy prefixPolicy) {
        String absoluteKey = prefixPolicy.buildAbsoluteKey(serviceId, op.path());
        return switch (op.op().toLowerCase()) {
            case "set" -> KVTransactionOperation.SetOperation.builder()
                    .key(absoluteKey)
                    .value(op.valueBytes())
                    .flags(op.flags())
                    .cas(op.cas())
                    .targetType(KVType.LEAF)
                    .build();
            case "delete" -> KVTransactionOperation.DeleteOperation.builder()
                    .key(absoluteKey)
                    .cas(op.cas())
                    .recurse(false)
                    .targetType(KVType.LEAF)
                    .build();
            default -> throw new IllegalArgumentException("Unsupported transaction operation: " + op.op());
        };
    }

    public static KVDtos.TransactionResponse toTransactionResponse(String serviceId,
                                                                    KVTransactionResponse response,
                                                                    PrefixPolicy prefixPolicy) {
        List<KVDtos.TransactionResponse.TransactionResult> results = response.results().stream()
                .map(result -> new KVDtos.TransactionResponse.TransactionResult(
                        prefixPolicy.extractRelativePath(serviceId, result.key()),
                        result.success(),
                        result.modifyIndex(),
                        result.message() == null || result.message().isEmpty() ? null : result.message()
                ))
                .toList();
        return new KVDtos.TransactionResponse(
                response.success(),
                results,
                response.errorMessage() == null || response.errorMessage().isEmpty()
                        ? null
                        : response.errorMessage()
        );
    }

    public static List<String> toDeleteIds(KVDtos.ListWriteRequest request) {
        return request.deletes() == null ? List.of() : request.deletes();
    }
}

