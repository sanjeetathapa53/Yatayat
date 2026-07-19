import { createContext, useContext, useEffect, useMemo, useState } from "react";
import { passengerDriverTranslations } from "../data/passengerDriverTranslations";
import { translations } from "../data/translations";

const LanguageContext = createContext();

export function LanguageProvider({ children }) {
  const [language, setLanguageState] = useState(() =>
    normalizeLanguage(localStorage.getItem("yatayat_language") || "en")
  );

  const setLanguage = (nextLanguage) => {
    setLanguageState(normalizeLanguage(nextLanguage));
  };

  useEffect(() => {
    localStorage.setItem("yatayat_language", language);
    document.documentElement.lang = language;
  }, [language]);

  const toggleLanguage = () => {
    setLanguageState((prev) => (prev === "en" ? "ne" : "en"));
  };

  const t = useMemo(() => {
    const dictionaries = mergeDictionaries(translations, passengerDriverTranslations);

    const translate = (key, values = {}) => {
      const currentValue = getNestedValue(dictionaries[language], key);
      const fallbackValue = getNestedValue(dictionaries.en, key);
      const translatedValue = currentValue ?? fallbackValue ?? key;

      return interpolate(String(translatedValue), values);
    };

    Object.assign(translate, dictionaries[language] || dictionaries.en);

    return translate;
  }, [language]);

  return (
    <LanguageContext.Provider
      value={{ language, setLanguage, toggleLanguage, t }}
    >
      {children}
    </LanguageContext.Provider>
  );
}

export function useLanguage() {
  return useContext(LanguageContext);
}

function normalizeLanguage(language) {
  return language === "ne" || language === "np" ? "ne" : "en";
}

function getNestedValue(source, key) {
  if (!source || !key) return undefined;

  return String(key)
    .split(".")
    .reduce((value, part) => value?.[part], source);
}

function interpolate(value, variables) {
  return value.replace(/\{\{(\w+)\}\}/g, (_, key) =>
    variables[key] === undefined || variables[key] === null
      ? ""
      : String(variables[key])
  );
}

function mergeDictionaries(base, extension) {
  return {
    en: deepMerge(base.en || {}, extension.en || {}),
    ne: deepMerge(base.ne || base.np || {}, extension.ne || {}),
  };
}

function deepMerge(base, extension) {
  const result = { ...base };

  Object.entries(extension).forEach(([key, value]) => {
    if (
      value &&
      typeof value === "object" &&
      !Array.isArray(value) &&
      result[key] &&
      typeof result[key] === "object" &&
      !Array.isArray(result[key])
    ) {
      result[key] = deepMerge(result[key], value);
    } else {
      result[key] = value;
    }
  });

  return result;
}
