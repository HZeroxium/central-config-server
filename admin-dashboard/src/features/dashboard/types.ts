export interface ServiceDistributionData {
  name: string;
  value: number;
  color: string;
}

export interface InstanceStatusData {
  name: string;
  value: number;
  color: string;
}

export interface DriftEventsData {
  date: string;
  critical: number;
  high: number;
  medium: number;
  low: number;
}

export interface ActivityItem {
  id: string;
  type: 'approval' | 'drift' | 'service' | 'user';
  message: string;
  timestamp: string;
  severity?: 'low' | 'medium' | 'high' | 'critical';
}

