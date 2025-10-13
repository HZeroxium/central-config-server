package com.example.control.consulclient.client;

import com.example.control.consulclient.core.ConsulResponse;
import com.example.control.consulclient.core.QueryOptions;
import com.example.control.consulclient.core.WriteOptions;
import com.example.control.consulclient.model.Session;
import com.example.control.consulclient.model.SessionCreateRequest;

import java.util.List;
import java.util.Optional;

/**
 * Client for Consul session operations.
 */
public interface SessionClient {
    
    /**
     * Create a new session.
     * 
     * @param request session creation request
     * @param options write options
     * @return response with created session
     */
    ConsulResponse<Session> create(SessionCreateRequest request, WriteOptions options);
    
    /**
     * Create a new session with default options.
     * 
     * @param request session creation request
     * @return response with created session
     */
    default ConsulResponse<Session> create(SessionCreateRequest request) {
        return create(request, null);
    }
    
    /**
     * Destroy a session.
     * 
     * @param sessionId the session ID to destroy
     * @param options write options
     * @return response with void
     */
    ConsulResponse<Void> destroy(String sessionId, WriteOptions options);
    
    /**
     * Destroy a session with default options.
     * 
     * @param sessionId the session ID to destroy
     * @return response with void
     */
    default ConsulResponse<Void> destroy(String sessionId) {
        return destroy(sessionId, null);
    }
    
    /**
     * List all sessions.
     * 
     * @param options query options
     * @return response with list of sessions
     */
    ConsulResponse<List<Session>> list(QueryOptions options);
    
    /**
     * List all sessions with default options.
     * 
     * @return response with list of sessions
     */
    default ConsulResponse<List<Session>> list() {
        return list(null);
    }
    
    /**
     * Read a specific session.
     * 
     * @param sessionId the session ID to read
     * @param options query options
     * @return response with optional session
     */
    ConsulResponse<Optional<Session>> read(String sessionId, QueryOptions options);
    
    /**
     * Read a specific session with default options.
     * 
     * @param sessionId the session ID to read
     * @return response with optional session
     */
    default ConsulResponse<Optional<Session>> read(String sessionId) {
        return read(sessionId, null);
    }
    
    /**
     * Renew a session.
     * 
     * @param sessionId the session ID to renew
     * @param options write options
     * @return response with renewed session
     */
    ConsulResponse<Session> renew(String sessionId, WriteOptions options);
    
    /**
     * Renew a session with default options.
     * 
     * @param sessionId the session ID to renew
     * @return response with renewed session
     */
    default ConsulResponse<Session> renew(String sessionId) {
        return renew(sessionId, null);
    }
}
