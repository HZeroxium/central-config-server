#!/bin/bash

# =============================================================================
# Entrypoint script for user-rpc-thrift-service
# =============================================================================
# This script ensures proper log directory permissions and starts the application
# =============================================================================

set -euo pipefail

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Logging functions
log_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

log_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

log_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Fix log directory permissions
fix_log_permissions() {
    log_info "Fixing log directory permissions..."
    
    # Ensure logs directory exists
    mkdir -p /app/logs
    
    # Try to fix ownership and permissions, but don't fail if not possible
    if chown -R appuser:appuser /app/logs 2>/dev/null; then
        log_success "Log directory ownership fixed successfully"
    else
        log_warning "Could not change ownership of log directory (volume mount), continuing..."
    fi
    
    # Always try to fix permissions
    chmod -R 755 /app/logs 2>/dev/null || true
    
    # Verify permissions
    if [ -w /app/logs ]; then
        log_success "Log directory permissions verified"
    else
        log_warning "Log directory may not be writable, but continuing..."
    fi
}

# Main execution
main() {
    log_info "Starting user-rpc-thrift-service initialization..."
    
    # Fix log directory permissions
    fix_log_permissions
    
    # Switch to appuser
    log_info "Switching to appuser..."
    exec gosu appuser "$@"
}

# Run main function with all arguments
main "$@"
