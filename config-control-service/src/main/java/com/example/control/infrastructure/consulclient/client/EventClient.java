package com.example.control.infrastructure.consulclient.client;

import com.example.control.infrastructure.consulclient.core.ConsulResponse;
import com.example.control.infrastructure.consulclient.core.QueryOptions;
import com.example.control.infrastructure.consulclient.core.WriteOptions;
import com.example.control.infrastructure.consulclient.model.Event;

import java.util.List;

/**
 * Client for Consul Event API operations.
 *
 * <p>This interface provides access to the Consul Event API, which manages
 * user-defined events in the Consul cluster. Events can be used for
 * coordination, notifications, and distributed system orchestration.</p>
 *
 * <p>All methods return {@link ConsulResponse} objects that contain both the
 * response data and Consul metadata (index, leader status, etc.).</p>
 *
 * @see <a href="https://www.consul.io/api/event.html">Consul Event API</a>
 */
public interface EventClient {

    /**
     * List all events.
     *
     * @param options Query options
     * @return List of events
     */
    ConsulResponse<List<Event>> list(QueryOptions options);

    /**
     * List events for a specific name.
     *
     * @param name    The event name to query
     * @param options Query options
     * @return List of events
     */
    ConsulResponse<List<Event>> listName(String name, QueryOptions options);

    /**
     * List events for a specific name in a specific datacenter.
     *
     * @param name       The event name to query
     * @param datacenter The datacenter to query
     * @param options    Query options
     * @return List of events
     */
    ConsulResponse<List<Event>> listNameDatacenter(String name, String datacenter, QueryOptions options);

    /**
     * Fire a new event.
     *
     * @param name    The event name
     * @param payload The event payload
     * @param options Write options
     * @return The event ID
     */
    ConsulResponse<String> fire(String name, byte[] payload, WriteOptions options);

    /**
     * Fire a new event in a specific datacenter.
     *
     * @param name       The event name
     * @param payload    The event payload
     * @param datacenter The datacenter
     * @param options    Write options
     * @return The event ID
     */
    ConsulResponse<String> fireDatacenter(String name, byte[] payload, String datacenter, WriteOptions options);

    /**
     * Fire a new event with node filter.
     *
     * @param name       The event name
     * @param payload    The event payload
     * @param nodeFilter The node filter
     * @param options    Write options
     * @return The event ID
     */
    ConsulResponse<String> fireNode(String name, byte[] payload, String nodeFilter, WriteOptions options);

    /**
     * Fire a new event with service filter.
     *
     * @param name          The event name
     * @param payload       The event payload
     * @param serviceFilter The service filter
     * @param options       Write options
     * @return The event ID
     */
    ConsulResponse<String> fireService(String name, byte[] payload, String serviceFilter, WriteOptions options);

    /**
     * Fire a new event with tag filter.
     *
     * @param name      The event name
     * @param payload   The event payload
     * @param tagFilter The tag filter
     * @param options   Write options
     * @return The event ID
     */
    ConsulResponse<String> fireTag(String name, byte[] payload, String tagFilter, WriteOptions options);

    /**
     * Fire a new event with multiple filters.
     *
     * @param name          The event name
     * @param payload       The event payload
     * @param nodeFilter    The node filter
     * @param serviceFilter The service filter
     * @param tagFilter     The tag filter
     * @param options       Write options
     * @return The event ID
     */
    ConsulResponse<String> fireFilters(String name, byte[] payload, String nodeFilter, String serviceFilter, String tagFilter, WriteOptions options);
}
