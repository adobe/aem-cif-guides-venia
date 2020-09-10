import i18n from 'i18next';
import {initReactI18next} from 'react-i18next';

import Backend from 'i18next-xhr-backend';
import LanguageDetector from 'i18next-browser-languagedetector';

i18n.use(Backend)
    .use(LanguageDetector)
    .use(initReactI18next)

    .init({
        fallbackLng: 'en-US',
        debug: true,

        load: 'currentOnly',
        defaultNS: 'common',
        ns: [],

        interpolation: {
            escapeValue: false,
            format: (value, format, lng) => {
                if (format === 'price') {
                    return new Intl.NumberFormat(lng, {
                        style: 'currency',
                        currency: value.currency,
                    }).format(value.value);
                }
                return value;
            },
        },

        detection: {
            order: ['htmlTag', 'path', 'subdomain'],

            lookupFromPathIndex: 1,
            lookupFromSubdomainIndex: 0,
        },

        backend: {
            loadPath:
                '/etc.clientlibs/venia/clientlibs/clientlib-site/resources/i18n/{{lng}}/{{ns}}.json',
            allowMultiLoading: false,
            withCredentials: true,
        },
    });

export default i18n;
