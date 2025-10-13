package com.example.control.consulclient.exception;

/**
 * Exception thrown when a CAS (Compare-And-Swap) operation fails.
 */
public class CasFailedException extends ConsulException {
    
    private final String key;
    private final Long expectedIndex;
    
    public CasFailedException(String key, Long expectedIndex) {
        super("CAS_FAILED", String.format("CAS failed for key '%s' with expected index %d", key, expectedIndex));
        this.key = key;
        this.expectedIndex = expectedIndex;
    }
    
    public CasFailedException(String key, Long expectedIndex, Throwable cause) {
        super("CAS_FAILED", String.format("CAS failed for key '%s' with expected index %d", key, expectedIndex), cause);
        this.key = key;
        this.expectedIndex = expectedIndex;
    }
    
    public String getKey() {
        return key;
    }
    
    public Long getExpectedIndex() {
        return expectedIndex;
    }
}
