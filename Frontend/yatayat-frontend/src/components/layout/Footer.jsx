import { useLanguage } from "../../hooks/useLanguage";
import YatayatLogo from "../branding/YatayatLogo";

export default function Footer() {
  const { t } = useLanguage();

  return (
    <footer className="bg-[#04294f] px-6 py-14 text-white">
      <div className="mx-auto grid max-w-7xl gap-10 md:grid-cols-4">
        <div>
          <YatayatLogo variant="compact" size="md" light />

          <p className="mt-6 text-sm leading-6 text-slate-300">
            {t.footerText}
          </p>
        </div>

        <FooterColumn
          title={t.services}
          items={[t.busTracking, t.routePlanning, t.buyTickets, t.travelPass]}
        />

        <FooterColumn
          title={t.network}
          items={[t.kathmanduValley, t.pokharaTransit, t.interCity, t.systemMap]}
        />

        <FooterColumn
          title={t.support}
          items={[t.helpCenter, t.contact, t.privacy, t.terms]}
        />
      </div>

      <div className="mx-auto mt-12 max-w-7xl border-t border-white/10 pt-6 text-xs text-slate-300">
        {t.copyright}
      </div>
    </footer>
  );
}

function FooterColumn({ title, items }) {
  return (
    <div>
      <h3 className="font-bold">{title}</h3>

      <div className="mt-5 space-y-3 text-sm text-slate-300">
        {items.map((item) => (
          <p key={item}>{item}</p>
        ))}
      </div>
    </div>
  );
}
