export type HealthState = 'UP' | 'DOWN'

export interface ServiceHealth {
  status: HealthState
}

export interface SystemHealthResponse {
  status: HealthState
  timestamp: string
  services: {
    backend: ServiceHealth
    database: ServiceHealth
    redis: ServiceHealth
  }
}
