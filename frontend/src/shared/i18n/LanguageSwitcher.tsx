import { ChevronDown, Languages } from 'lucide-react'
import { useTranslation } from 'react-i18next'
import vietnamFlag from '../../assets/logo-banners/vietnam.png'
import englandFlag from '../../assets/logo-banners/england.png'

const languages = [
  { code: 'vi', label: 'Tiếng Việt', flag: vietnamFlag },
  { code: 'en', label: 'English', flag: englandFlag },
] as const

export function LanguageSwitcher() {
  const { t, i18n } = useTranslation()
  const language = i18n.resolvedLanguage === 'en' ? 'en' : 'vi'
  const current = languages.find((item) => item.code === language) ?? languages[0]

  return <details className="group relative z-30 shrink-0">
    <summary className="flex min-h-11 cursor-pointer list-none items-center gap-1.5 rounded-lg px-2 text-sm font-semibold text-text-secondary hover:bg-primary-soft hover:text-text-primary focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-primary [&::-webkit-details-marker]:hidden" aria-label={t('language')}>
      <Languages size={18} aria-hidden="true" />
      <img src={current.flag} alt="" aria-hidden="true" className="size-5 rounded-full object-cover" />
      <span className="hidden sm:inline">{current.code.toUpperCase()}</span>
      <ChevronDown className="transition-transform group-open:rotate-180" size={16} aria-hidden="true" />
    </summary>
    <div className="absolute right-0 top-full z-30 mt-2 min-w-44 rounded-xl border border-border bg-surface p-1.5 shadow-lg shadow-slate-900/10">
      <p className="px-3 py-2 text-xs font-semibold uppercase tracking-wide text-text-muted">{t('language')}</p>
      {languages.map((item) => <button key={item.code} type="button" aria-pressed={language === item.code} onClick={(event) => { void i18n.changeLanguage(item.code); event.currentTarget.closest('details')?.removeAttribute('open') }} className={`flex min-h-11 w-full items-center gap-2 rounded-lg px-3 text-left text-sm font-medium transition-colors ${language === item.code ? 'bg-primary-soft text-primary' : 'text-text-primary hover:bg-primary-soft'}`}><img src={item.flag} alt="" aria-hidden="true" className="size-5 rounded-full object-cover" /><span>{item.label}</span></button>)}
    </div>
  </details>
}
