import type { BookingHistoryItem } from './ticketApi'
import i18n from '../../i18n'

export type DashboardNotice = {
  booking: BookingHistoryItem
  category: 'issue' | 'action' | 'processing'
  title: string
  description: string
  actionLabel: string
  actionTo: string
}

export function bookingStatusLabel(booking: BookingHistoryItem): string {
  if (booking.showtimeStatus === 'CANCELLED') return i18n.t('statusShowtimeCancelled')
  if (booking.refundStatus === 'REFUND_FAILED' || booking.bookingStatus === 'REFUND_FAILED') return i18n.t('statusRefundFailed')
  if (booking.refundStatus === 'REFUNDED' || booking.bookingStatus === 'REFUNDED') return i18n.t('statusRefunded')
  if (booking.refundStatus === 'REQUESTED' || booking.bookingStatus === 'REFUND_PENDING') return i18n.t('statusRefundPending')
  switch (booking.bookingStatus) {
    case 'PAID': return i18n.t('statusPaid')
    case 'PAYMENT_REVIEW': return i18n.t('statusPaymentReview')
    case 'PENDING_PAYMENT': return booking.canResumePayment ? i18n.t('statusPendingPayment') : i18n.t('statusPaymentUpdating')
    case 'EXPIRED': return i18n.t('statusExpired')
    case 'CANCELLED': return i18n.t('statusCancelled')
    default: return i18n.t('statusUpdating')
  }
}

export function refundStatusLabel(booking: BookingHistoryItem): string | null {
  if (booking.refundStatus === 'REFUND_FAILED' || booking.bookingStatus === 'REFUND_FAILED') return i18n.t('statusRefundFailed')
  if (booking.refundStatus === 'REFUNDED' || booking.bookingStatus === 'REFUNDED') return i18n.t('statusRefunded')
  if (booking.refundStatus === 'REQUESTED' || booking.bookingStatus === 'REFUND_PENDING') return i18n.t('statusRefundPending')
  return null
}

export function dashboardNoticeFor(booking: BookingHistoryItem): DashboardNotice | null {
  const historyLink = `/me/bookings?q=${encodeURIComponent(booking.bookingCode)}`
  const refundLabel = refundStatusLabel(booking)
  if (booking.showtimeStatus === 'CANCELLED' && booking.refundStatus !== 'REFUNDED' && booking.bookingStatus !== 'REFUNDED') {
    return {
      booking, category: 'issue', title: i18n.t('statusShowtimeCancelled'),
      description: i18n.t('noticeShowtimeCancelled', { code: booking.bookingCode, detail: refundLabel ? i18n.t('noticeStatus', { status: refundLabel.toLowerCase() }) : booking.ticketCode ? i18n.t('noticeRefundTrack') : i18n.t('noticeNoPayment') }),
      actionLabel: i18n.t('viewBooking'), actionTo: historyLink,
    }
  }
  if (booking.refundStatus === 'REFUND_FAILED' || booking.bookingStatus === 'REFUND_FAILED') {
    return { booking, category: 'issue', title: i18n.t('statusRefundFailed'),
      description: i18n.t('noticeRefundFailed', { code: booking.bookingCode }),
      actionLabel: i18n.t('track'), actionTo: historyLink }
  }
  if (booking.bookingStatus === 'PENDING_PAYMENT' && booking.canResumePayment) {
    return { booking, category: 'action', title: i18n.t('noticeCompletePayment'),
      description: i18n.t('noticeCompletePaymentHint', { code: booking.bookingCode }),
      actionLabel: i18n.t('noticeContinuePayment'), actionTo: `/checkout/${encodeURIComponent(booking.bookingCode)}` }
  }
  if (booking.bookingStatus === 'PAYMENT_REVIEW' || booking.bookingStatus === 'PENDING_PAYMENT' && booking.paymentStatus === 'INITIATED') {
    return { booking, category: 'processing', title: i18n.t('noticeVerifyPayment'),
      description: i18n.t('noticeVerifyPaymentHint', { code: booking.bookingCode }),
      actionLabel: i18n.t('track'), actionTo: historyLink }
  }
  if (booking.refundStatus === 'REQUESTED' || booking.bookingStatus === 'REFUND_PENDING') {
    return { booking, category: 'processing', title: i18n.t('noticeRefundProcessing'),
      description: i18n.t('noticeRefundProcessingHint', { code: booking.bookingCode }),
      actionLabel: i18n.t('track'), actionTo: historyLink }
  }
  return null
}
