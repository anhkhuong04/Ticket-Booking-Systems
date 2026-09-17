import { isAxiosError } from 'axios'
import i18n from '../../i18n'

const codeKeys: Record<string, string> = {
  INVALID_CREDENTIALS: 'apiInvalidCredentials',
  ACCOUNT_LOCKED: 'apiAccountLocked',
  EMAIL_ALREADY_REGISTERED: 'apiEmailRegistered',
  PASSWORD_RESET_TOKEN_INVALID: 'apiPasswordResetInvalid',
  VALIDATION_FAILED: 'apiValidationFailed',
  BOOKING_NOT_FOUND: 'apiBookingNotFound',
  BOOKING_FORBIDDEN: 'apiBookingForbidden',
  SEAT_HOLD_NOT_ACTIVE: 'apiSeatHoldInactive',
  PAYMENT_REVIEW: 'apiPaymentReview',
}

type ApiErrorBody = { code?: string; message?: string }

export function localizeApiError(error: unknown, fallbackKey = 'apiGenericError'): string {
  if (!isAxiosError<ApiErrorBody>(error)) return i18n.t('apiConnectionError')
  const code = error.response?.data?.code
  if (code && codeKeys[code]) return i18n.t(codeKeys[code])
  if (i18n.language === 'vi' && error.response?.data?.message) return error.response.data.message
  return i18n.t(fallbackKey)
}
