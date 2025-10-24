/**
 * Local types for Service Registry (Consul) feature
 */

export interface ConsulServiceSummary {
  serviceName: string;
  instanceCount: number;
  healthyInstanceCount: number;
  tags: string[];
  status: 'healthy' | 'warning' | 'critical' | 'unknown';
}

export interface ConsulInstanceSummary {
  serviceId: string;
  serviceName: string;
  address: string;
  port: number;
  status: 'passing' | 'warning' | 'critical';
  checks: ConsulHealthCheckSummary[];
  metadata?: Record<string, string>;
}

export interface ConsulHealthCheckSummary {
  checkId: string;
  name: string;
  status: 'passing' | 'warning' | 'critical';
  output: string;
}

