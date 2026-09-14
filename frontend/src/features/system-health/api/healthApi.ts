import axios from 'axios'
import { apiClient, assertApiConfigured } from '../../../shared/api/apiClient'
import type {
  HealthState,
  ServiceHealth,
  SystemHealthResponse,
} from '../types'

const isRecord = (value: unknown): value is Record<string, unknown> =>
  typeof value === 'object' && value !== null

const isHealthState = (value: unknown): value is HealthState =>
  value === 'UP' || value === 'DOWN'

const isServiceHealth = (value: unknown): value is ServiceHealth =>
  isRecord(value) && isHealthState(value.status)

function parseHealthResponse(value: unknown): SystemHealthResponse | null {
  if (!isRecord(value) || !isHealthState(value.status)) {
    return null
  }

  if (typeof value.timestamp !== 'string' || !isRecord(value.services)) {
    return null
  }

  const { backend, database, redis } = value.services
  if (
    !isServiceHealth(backend) ||
    !isServiceHealth(database) ||
    !isServiceHealth(redis)
  ) {
    return null
  }

  return {
    status: value.status,
    timestamp: value.timestamp,
    services: { backend, database, redis },
  }
}

export async function getSystemHealth(
  signal?: AbortSignal,
): Promise<SystemHealthResponse> {
  assertApiConfigured()

  try {
    const response = await apiClient.get<unknown>('/api/health', { signal })
    const health = parseHealthResponse(response.data)
    if (!health) {
      throw new Error('Backend trả về health response không hợp lệ.')
    }
    return health
  } catch (error) {
    if (axios.isAxiosError(error)) {
      const health = parseHealthResponse(error.response?.data)
      if (health) {
        return health
      }
    }
    throw error
  }
}
