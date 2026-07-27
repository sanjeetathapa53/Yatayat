import { Network } from "lucide-react";
import { useLanguage } from "../../hooks/useLanguage";

export default function Connectivity() {
  const { t } = useLanguage();

  return (
    <section className="bg-[#fbfbff] px-6 py-20 text-center">
      <h2 className="text-4xl font-extrabold text-[#08264a]">
        {t.connectivityTitle}
      </h2>

      <p className="mx-auto mt-5 max-w-2xl text-slate-600">
        {t.connectivitySubtitle}
      </p>

      <div className="mx-auto mt-12 flex h-96 max-w-7xl items-center justify-center rounded-3xl bg-[#183f69] shadow-2xl">
        <div className="w-full max-w-lg rounded-2xl bg-white/20 p-12 text-center backdrop-blur">
          <Network className="mx-auto text-emerald-400" size={48} />

          <h3 className="mt-4 text-xl font-bold text-white">
            {t.hubTitle}
          </h3>

          <p className="mt-3 text-sm text-slate-200">
            {t.hubText}
          </p>
        </div>
      </div>
    </section>
  );
}
