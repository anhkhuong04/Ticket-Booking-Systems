import { AxiosError } from 'axios'
import { beforeEach, expect, it } from 'vitest'
import i18n from '../../i18n'
import { localizeApiError } from './apiError'

beforeEach(async () => { await i18n.changeLanguage('en') })

it('uses the API code instead of the server message in English', () => {
  const error = new AxiosError('Bad Request', 'ERR_BAD_REQUEST', undefined, undefined, {
    data: { code: 'INVALID_CREDENTIALS', message: 'Sai mật khẩu' }, status: 401, statusText: 'Unauthorized', headers: {}, config: { headers: {} as never },
  })
  expect(localizeApiError(error)).toBe('Incorrect email or password.')
})
