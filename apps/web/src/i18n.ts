import i18n from 'i18next';
import { initReactI18next } from 'react-i18next';
import es from '@content/i18n/es.json';

declare module 'i18next' {
  interface CustomTypeOptions {
    resources: { translation: typeof es };
  }
}

void i18n.use(initReactI18next).init({
  lng: 'es',
  fallbackLng: 'es',
  resources: { es: { translation: es } },
  interpolation: { escapeValue: false },
});

export default i18n;
