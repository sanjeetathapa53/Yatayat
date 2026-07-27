import { useContext } from "react";
import { LanguageContext } from "../context/languageContextValue";

export function useLanguage() {
  return useContext(LanguageContext);
}
