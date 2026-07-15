import Button from "../common/Button";
import Badge from "../common/Badge";
import { useLanguage } from "../../context/LanguageContext";
import mapBg from "../../assets/images/map-bg.png";

export default function Hero() {
  const { t } = useLanguage();

  return (
    <section
      className="relative min-h-screen overflow-hidden bg-cover bg-right bg-no-repeat"
      style={{
        backgroundImage: `url(${mapBg})`,
      }}
    >
      {/* Overlay: makes image visible but keeps text readable */}
      <div className="absolute inset-0 bg-white/2"></div>

      {/* Left gradient: keeps left text area clean */}
      <div className="absolute inset-0 bg-linear-to-r from-white/85 via-white/50 to-transparent"></div>

      {/* Soft blue glow */}
      <div className="absolute inset-0 bg-[radial-gradient(circle_at_center,#7dd3fc25,transparent_60%)]"></div>

      <div className="relative z-10 mx-auto max-w-7xl px-6 pt-40 pb-24">
        <div className="max-w-xl">
          <Badge>{t.liveBadge}</Badge>

          <h1 className="mt-8 text-6xl font-black leading-none text-[#08264a]">
            {t.heroTitle1}
            <br />
            <span className="text-emerald-600">{t.heroTitle2}</span>
          </h1>

          <p className="mt-8 text-lg leading-8 text-slate-600">
            {t.heroSubtitle}
          </p>

          <div className="mt-10 max-w-md rounded-3xl bg-white p-5 shadow-2xl">
            <div className="flex gap-3">
              <input
                type="text"
                placeholder={t.trackPlaceholder}
                className="flex-1 bg-transparent outline-none"
              />

              <Button>{t.trackButton}</Button>
            </div>

            <div className="mt-3 flex justify-between text-xs text-slate-500">
              <span>{t.popularRoute}</span>

              <a href="#" className="font-semibold text-emerald-600">
                {t.viewFullMap}
              </a>
            </div>
          </div>

          <div className="mt-8 flex flex-wrap gap-4">
            <Button>{t.ticketButton}</Button>
            <Button variant="secondary">{t.learnMore}</Button>
          </div>
        </div>
      </div>
    </section>
  );
}