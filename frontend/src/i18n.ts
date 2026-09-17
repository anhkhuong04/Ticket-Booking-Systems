import i18n from 'i18next'
import { initReactI18next } from 'react-i18next'

export const LANGUAGE_KEY = 'lak-language'
export type LanguageCode = 'vi' | 'en'

const translations = {
  vi: {
    translation: {
      appTitle: 'LAK Cinema | Đặt vé xem phim', language: 'Ngôn ngữ', movies: 'Phim', cinemas: 'Rạp', search: 'Tìm phim, rạp...', searchLabel: 'Tìm phim hoặc rạp', myTickets: 'Vé của tôi', account: 'Tài khoản', profile: 'Hồ sơ', billing: 'Thông tin hóa đơn', signIn: 'Đăng nhập', signOut: 'Đăng xuất', home: 'Trang chủ', allRights: 'Tất cả quyền được bảo lưu.', tagline: 'Điện ảnh kết nối cảm xúc, đặt vé an toàn và rõ ràng.',
      featured: 'Phim nổi bật tuần này', bookNow: 'Đặt vé ngay', nowShowing: 'Phim đang chiếu', comingSoon: 'Phim sắp chiếu', lakCinemas: 'Rạp LAK', all: 'Xem tất cả', minutes: '{{count}} phút', genresPending: 'Đang cập nhật thể loại', loadError: 'Không thể tải dữ liệu.', retry: 'Thử lại', noNowShowing: 'Chưa có phim đang chiếu.', noComingSoon: 'Chưa có phim sắp chiếu.', noCinemas: 'Chưa có chi nhánh đang hoạt động.',
      searchMovie: 'Tìm theo tên phim', genre: 'Thể loại', allGenres: 'Tất cả thể loại', apply: 'Áp dụng', noMovies: 'Không tìm thấy phim phù hợp', clearFilters: 'Xóa bộ lọc', allMovies: 'Tất cả phim', releaseDate: 'Khởi chiếu {{date}}', country: 'Quốc gia', director: 'Đạo diễn', cast: 'Diễn viên', synopsis: 'Nội dung phim', synopsisPending: 'Thông tin phim đang được cập nhật.', chooseShowtime: 'Chọn suất chiếu', trailer: 'Xem trailer', chooseCinemaHint: 'Chọn chi nhánh thuận tiện để xem lịch chiếu.',
      explore: 'Khám phá', offers: 'Ưu đãi', support: 'Hỗ trợ', faq: 'Câu hỏi thường gặp', terms: 'Điều khoản sử dụng', privacy: 'Chính sách bảo mật', contact: 'Liên hệ', connect: 'Kết nối với chúng tôi', downloadApp: 'Tải ứng dụng LAK', comingUpdate: 'sắp cập nhật',
      quickBooking: 'Đặt vé nhanh', quickBookingHint: 'Chỉ hiển thị rạp, ngày và suất chiếu đang mở bán.', chooseMovie: 'Chọn phim', chooseCinema: 'Chọn rạp', chooseDate: 'Chọn ngày', chooseSession: 'Chọn suất chiếu', loadingCinemas: 'Đang tải rạp…', loadingSessions: 'Đang tải suất…', quickBuy: 'Mua vé nhanh', moviesError: 'Không thể tải danh sách phim.', availabilityError: 'Không thể tải rạp và ngày chiếu.', noOpenSessions: 'Phim này chưa có suất chiếu đang mở bán.', showtimesError: 'Không thể tải suất chiếu.', noSessionsDate: 'Không còn suất chiếu mở bán trong ngày đã chọn.',
    },
  },
  en: {
    translation: {
      appTitle: 'LAK Cinema | Book movie tickets', language: 'Language', movies: 'Movies', cinemas: 'Cinemas', search: 'Search movies, cinemas...', searchLabel: 'Search movies or cinemas', myTickets: 'My tickets', account: 'Account', profile: 'Profile', billing: 'Billing information', signIn: 'Sign in', signOut: 'Sign out', home: 'Home', allRights: 'All rights reserved.', tagline: 'Cinema connects us. Book tickets safely and clearly.',
      featured: 'Featured this week', bookNow: 'Book now', nowShowing: 'Now showing', comingSoon: 'Coming soon', lakCinemas: 'LAK cinemas', all: 'View all', minutes: '{{count}} min', genresPending: 'Genres coming soon', loadError: 'Unable to load data.', retry: 'Try again', noNowShowing: 'No movies showing now.', noComingSoon: 'No upcoming movies.', noCinemas: 'No active cinemas yet.',
      searchMovie: 'Search by movie title', genre: 'Genre', allGenres: 'All genres', apply: 'Apply', noMovies: 'No matching movies found', clearFilters: 'Clear filters', allMovies: 'All movies', releaseDate: 'Release date {{date}}', country: 'Country', director: 'Director', cast: 'Cast', synopsis: 'Synopsis', synopsisPending: 'Movie information is being updated.', chooseShowtime: 'Choose a showtime', trailer: 'Watch trailer', chooseCinemaHint: 'Choose a convenient cinema to view showtimes.',
      explore: 'Explore', offers: 'Offers', support: 'Support', faq: 'FAQ', terms: 'Terms of use', privacy: 'Privacy policy', contact: 'Contact', connect: 'Connect with us', downloadApp: 'Get the LAK app', comingUpdate: 'coming soon',
      quickBooking: 'Quick booking', quickBookingHint: 'Only cinemas, dates and showtimes currently on sale are shown.', chooseMovie: 'Choose a movie', chooseCinema: 'Choose a cinema', chooseDate: 'Choose a date', chooseSession: 'Choose a showtime', loadingCinemas: 'Loading cinemas…', loadingSessions: 'Loading showtimes…', quickBuy: 'Book quickly', moviesError: 'Unable to load movies.', availabilityError: 'Unable to load cinemas and dates.', noOpenSessions: 'No showtimes are on sale for this movie.', showtimesError: 'Unable to load showtimes.', noSessionsDate: 'No showtimes are on sale for the selected date.',
    },
  },
} as const

function initialLanguage(): LanguageCode {
  try { return window.localStorage.getItem(LANGUAGE_KEY) === 'en' ? 'en' : 'vi' } catch { return 'vi' }
}

void i18n.use(initReactI18next).init({ resources: translations, lng: initialLanguage(), fallbackLng: 'vi', supportedLngs: ['vi', 'en'], interpolation: { escapeValue: false } })

i18n.on('languageChanged', (language) => {
  document.documentElement.lang = language === 'en' ? 'en' : 'vi'
  document.title = i18n.t('appTitle')
  try { window.localStorage.setItem(LANGUAGE_KEY, document.documentElement.lang) } catch { /* Storage can be unavailable. */ }
})
document.documentElement.lang = i18n.language === 'en' ? 'en' : 'vi'
document.title = i18n.t('appTitle')

export function displayLocale(language: string): string { return language === 'en' ? 'en-US' : 'vi-VN' }
export default i18n
