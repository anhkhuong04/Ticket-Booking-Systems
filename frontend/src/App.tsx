import { Link, Navigate, Route, Routes } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import { LanguageSwitcher } from './shared/i18n/LanguageSwitcher'
import { AuthProvider, useAuth } from './features/auth/AuthProvider'
import { ForgotPasswordPage, LoginPage, RegisterPage, ResetPasswordPage } from './features/auth/AuthPages'
import { ProtectedRoute } from './features/auth/ProtectedRoute'
import { AdminAuditoriumsPage, AdminCinemasPage, AdminMoviesPage } from './features/admin/AdminPages'
import { AdminPricingPage, AdminShowtimesPage } from './features/admin/AdminShowtimePages'
import { AdminShell } from './features/admin/AdminShell'
import { AdminBookingsPage, AdminPaymentsPage, AdminRefundsPage, AdminUsersPage } from './features/admin/AdminOperationsPages'
import { AdminDashboardPage } from './features/admin/AdminDashboardPage'
import { CinemasPage, HomePage, MovieDetailPage, MoviesPage } from './features/catalog/CatalogPages'
import { BookingHistoryPage } from './features/account/BookingHistoryPage'
import { CustomerShell } from './features/catalog/CustomerShell'
import { ShowtimeSelectionPage } from './features/showtime/ShowtimeSelectionPage'
import { SeatSelectionPage } from './features/reservation/SeatSelectionPage'
import { CheckoutPage } from './features/booking/CheckoutPage'
import { PaymentResultPage } from './features/payment/PaymentResultPage'
import { RefundRequestPage } from './features/refund/RefundRequestPage'
import { MyTicketsPage, TicketDetailPage } from './features/ticketing/TicketPages'
import { StaffScannerPage } from './features/ticketing/StaffScannerPage'
import { UserDashboardPage } from './features/account/UserDashboardPage'
import { CustomerProfilePage } from './features/account/CustomerProfilePage'
import { BillingPreferencesPage } from './features/account/BillingPreferencesPage'

function RoleShell() {
  const { t } = useTranslation()
  const { user, logout } = useAuth()
  return <main className="min-h-screen bg-background p-6"><header className="mx-auto flex max-w-5xl items-center justify-between"><Link className="font-bold text-primary" to="/">LAK</Link><div className="flex items-center gap-3"><LanguageSwitcher /><button onClick={() => void logout()} className="min-h-11 text-sm font-semibold text-primary">{t('signOut')}</button></div></header><section className="mx-auto mt-12 max-w-5xl rounded-xl border border-border bg-surface p-8"><h1 className="text-2xl font-bold">{t('staffArea')}</h1><p className="mt-2 text-text-secondary">{t('signedInRole', { roles: user?.roles.join(', ') })}</p></section></main>
}

function ForbiddenPage() {
  const { t } = useTranslation()
  return <main className="grid min-h-screen place-items-center bg-background p-6"><section className="max-w-md text-center"><LanguageSwitcher /><h1 className="mt-4 text-2xl font-bold">{t('forbidden')}</h1><Link className="mt-4 inline-block text-primary underline" to="/">{t('back')}</Link></section></main>
}

function App() {
  return <AuthProvider><Routes>
    <Route element={<CustomerShell />}>
      <Route path="/" element={<HomePage />} />
      <Route path="/movies" element={<MoviesPage />} />
      <Route path="/movies/:movieId" element={<MovieDetailPage />} />
      <Route path="/movies/:movieId/showtimes" element={<ShowtimeSelectionPage />} />
      <Route path="/cinemas" element={<CinemasPage />} />
      <Route element={<ProtectedRoute roles={['CUSTOMER']} />}><Route path="/me" element={<UserDashboardPage />} /></Route>
      <Route element={<ProtectedRoute roles={['CUSTOMER']} />}><Route path="/me/bookings" element={<BookingHistoryPage />} /></Route>
      <Route element={<ProtectedRoute roles={['CUSTOMER']} />}><Route path="/me/profile" element={<CustomerProfilePage />} /></Route>
      <Route element={<ProtectedRoute roles={['CUSTOMER']} />}><Route path="/me/billing" element={<BillingPreferencesPage />} /></Route>
    </Route>
    <Route path="/login" element={<LoginPage />} />
    <Route path="/register" element={<RegisterPage />} />
    <Route path="/forgot-password" element={<ForgotPasswordPage />} />
    <Route path="/reset-password" element={<ResetPasswordPage />} />
    <Route element={<ProtectedRoute roles={['CUSTOMER']} />}><Route path="/me/tickets" element={<MyTicketsPage />} /></Route>
    <Route element={<ProtectedRoute roles={['CUSTOMER']} />}><Route path="/tickets/:ticketCode" element={<TicketDetailPage />} /></Route>
    <Route element={<ProtectedRoute roles={['CUSTOMER']} />}><Route path="/showtimes/:showtimeId/seats" element={<SeatSelectionPage />} /></Route>
    <Route element={<ProtectedRoute roles={['CUSTOMER']} />}><Route path="/checkout/:bookingId" element={<CheckoutPage />} /></Route>
    <Route element={<ProtectedRoute roles={['CUSTOMER']} />}><Route path="/payments/:paymentId/result" element={<PaymentResultPage />} /></Route>
    <Route element={<ProtectedRoute roles={['CUSTOMER']} />}><Route path="/me/bookings/:bookingId/refund" element={<RefundRequestPage />} /></Route>
    <Route element={<ProtectedRoute roles={['TICKET_STAFF', 'SUPER_ADMIN']} />}><Route path="/staff" element={<RoleShell />} /></Route>
    <Route element={<ProtectedRoute roles={['CINEMA_MANAGER', 'SUPER_ADMIN']} />}>
      <Route element={<AdminShell />}>
        <Route path="/admin" element={<AdminDashboardPage />} />
        <Route path="/admin/movies" element={<AdminMoviesPage />} />
        <Route path="/admin/cinemas" element={<AdminCinemasPage />} />
        <Route path="/admin/cinemas/:cinemaId/auditoriums" element={<AdminAuditoriumsPage />} />
        <Route path="/admin/showtimes" element={<AdminShowtimesPage />} />
        <Route path="/admin/pricing" element={<AdminPricingPage />} />
        <Route path="/admin/bookings" element={<AdminBookingsPage />} />
        <Route path="/admin/payments" element={<AdminPaymentsPage />} />
        <Route path="/admin/refunds" element={<AdminRefundsPage />} />
        <Route path="/admin/users" element={<AdminUsersPage />} />
      </Route>
    </Route>
    <Route element={<ProtectedRoute roles={['TICKET_STAFF', 'SUPER_ADMIN']} />}><Route path="/staff/scanner" element={<StaffScannerPage />} /></Route>
    <Route path="/forbidden" element={<ForbiddenPage />} />
    <Route path="*" element={<Navigate to="/" replace />} />
  </Routes></AuthProvider>
}

export default App
