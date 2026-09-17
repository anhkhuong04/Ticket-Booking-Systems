import type { BookingHistoryItem } from './ticketApi'

export type DashboardNotice = {
  booking: BookingHistoryItem
  category: 'issue' | 'action' | 'processing'
  title: string
  description: string
  actionLabel: string
  actionTo: string
}

export function bookingStatusLabel(booking: BookingHistoryItem): string {
  if (booking.showtimeStatus === 'CANCELLED') return 'Suất chiếu đã hủy'
  if (booking.refundStatus === 'REFUND_FAILED' || booking.bookingStatus === 'REFUND_FAILED') return 'Hoàn tiền cần hỗ trợ'
  if (booking.refundStatus === 'REFUNDED' || booking.bookingStatus === 'REFUNDED') return 'Đã hoàn tiền'
  if (booking.refundStatus === 'REQUESTED' || booking.bookingStatus === 'REFUND_PENDING') return 'Đang hoàn tiền'
  switch (booking.bookingStatus) {
    case 'PAID': return 'Đã thanh toán'
    case 'PAYMENT_REVIEW': return 'Đang đối soát thanh toán'
    case 'PENDING_PAYMENT': return booking.canResumePayment ? 'Chờ thanh toán' : 'Đang cập nhật thanh toán'
    case 'EXPIRED': return 'Đã hết hạn'
    case 'CANCELLED': return 'Đã hủy'
    default: return 'Đang cập nhật trạng thái'
  }
}

export function refundStatusLabel(booking: BookingHistoryItem): string | null {
  if (booking.refundStatus === 'REFUND_FAILED' || booking.bookingStatus === 'REFUND_FAILED') return 'Hoàn tiền cần hỗ trợ'
  if (booking.refundStatus === 'REFUNDED' || booking.bookingStatus === 'REFUNDED') return 'Đã hoàn tiền'
  if (booking.refundStatus === 'REQUESTED' || booking.bookingStatus === 'REFUND_PENDING') return 'Đang hoàn tiền'
  return null
}

export function dashboardNoticeFor(booking: BookingHistoryItem): DashboardNotice | null {
  const historyLink = `/me/bookings?q=${encodeURIComponent(booking.bookingCode)}`
  const refundLabel = refundStatusLabel(booking)
  if (booking.showtimeStatus === 'CANCELLED' && refundLabel !== 'Đã hoàn tiền') {
    return {
      booking, category: 'issue', title: 'Suất chiếu đã hủy',
      description: `Suất chiếu của booking ${booking.bookingCode} đã bị hủy; vé không còn hiệu lực. ${refundLabel ? `Trạng thái: ${refundLabel.toLowerCase()}.` : booking.ticketCode ? 'Vui lòng theo dõi trạng thái hoàn tiền.' : 'Bạn không cần tiếp tục thanh toán.'}`,
      actionLabel: 'Xem booking', actionTo: historyLink,
    }
  }
  if (refundLabel === 'Hoàn tiền cần hỗ trợ') {
    return { booking, category: 'issue', title: 'Hoàn tiền cần hỗ trợ',
      description: `Bộ phận hỗ trợ đang xử lý hoàn tiền cho booking ${booking.bookingCode}. Bạn không cần gửi yêu cầu mới.`,
      actionLabel: 'Theo dõi', actionTo: historyLink }
  }
  if (booking.bookingStatus === 'PENDING_PAYMENT' && booking.canResumePayment) {
    return { booking, category: 'action', title: 'Hoàn tất thanh toán',
      description: `Booking ${booking.bookingCode} còn trong hạn thanh toán. Trang xác nhận sẽ kiểm tra lại thời hạn trước khi tiếp tục.`,
      actionLabel: 'Tiếp tục thanh toán', actionTo: `/checkout/${encodeURIComponent(booking.bookingCode)}` }
  }
  if (booking.bookingStatus === 'PAYMENT_REVIEW' || booking.bookingStatus === 'PENDING_PAYMENT' && booking.paymentStatus === 'INITIATED') {
    return { booking, category: 'processing', title: 'Đang xác minh thanh toán',
      description: `Booking ${booking.bookingCode} đang được hệ thống xác minh. Vui lòng không thanh toán lại.`,
      actionLabel: 'Theo dõi', actionTo: historyLink }
  }
  if (refundLabel === 'Đang hoàn tiền') {
    return { booking, category: 'processing', title: 'Đang xử lý hoàn tiền',
      description: `Yêu cầu hoàn tiền cho booking ${booking.bookingCode} đã được tiếp nhận.`,
      actionLabel: 'Theo dõi', actionTo: historyLink }
  }
  return null
}
