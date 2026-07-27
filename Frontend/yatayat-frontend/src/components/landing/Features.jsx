import { CheckCircle, Bus, CreditCard, Radio } from "lucide-react";
import { useLanguage } from "../../hooks/useLanguage";

export default function Features() {
  const { t } = useLanguage();

  const features = [
    {
      icon: Bus,
      title: t.liveTracking,
      description: t.liveTrackingText,
      points: [t.preciseEtas, t.capacityAlerts],
    },
    {
      icon: CreditCard,
      title: t.securePayments,
      description: t.securePaymentsText,
      points: [t.qrTickets, t.autoWallet],
    },
    {
      icon: Radio,
      title: t.realtimeUpdates,
      description: t.realtimeUpdatesText,
      points: [t.routeDiversions, t.trafficReports],
    },
  ];

  return (
    <section className="bg-[#fbfbff] px-6 py-20">
      <div className="mx-auto grid max-w-7xl gap-8 md:grid-cols-3">
        {features.map((feature) => {
          const Icon = feature.icon;

          return (
            <div key={feature.title} className="rounded-3xl bg-white p-9 shadow-lg">
              <div className="mb-7 flex h-12 w-12 items-center justify-center rounded-xl bg-[#08264a] text-white">
                <Icon size={24} />
              </div>

              <h3 className="text-xl font-extrabold text-[#08264a]">
                {feature.title}
              </h3>

              <p className="mt-5 leading-7 text-slate-600">
                {feature.description}
              </p>

              <ul className="mt-6 space-y-2">
                {feature.points.map((point) => (
                  <li key={point} className="flex items-center gap-2 text-sm text-slate-700">
                    <CheckCircle size={16} className="text-emerald-600" />
                    {point}
                  </li>
                ))}
              </ul>
            </div>
          );
        })}
      </div>
    </section>
  );
}
