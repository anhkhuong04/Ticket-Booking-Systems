import { afterEach, expect, it, vi } from 'vitest'
import { apiClient } from '../../shared/api/apiClient'
import { uploadMovieMedia } from './adminApi'

vi.mock('../../shared/api/apiClient', () => ({ apiClient: { post: vi.fn() } }))

afterEach(() => { vi.restoreAllMocks(); vi.mocked(apiClient.post).mockReset(); vi.unstubAllGlobals() })

it('explains a Cloudinary authentication rejection without exposing the provider response', async () => {
  vi.mocked(apiClient.post).mockResolvedValue({ data: { cloudName: 'example', apiKey: 'key', folder: 'lak-movies', resourceType: 'image', timestamp: 1, signature: 'signed' } })
  vi.stubGlobal('fetch', vi.fn().mockResolvedValue({ status: 401, ok: false }))
  const file = new File(['poster'], 'poster.png', { type: 'image/png' })

  await expect(uploadMovieMedia(file)).rejects.toThrow('Cloudinary từ chối xác thực upload')
})
