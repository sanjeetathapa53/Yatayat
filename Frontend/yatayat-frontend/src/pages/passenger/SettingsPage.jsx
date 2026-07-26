import { useState } from "react";
import {
  Settings,
  Globe,
  ShieldCheck,
  Mail,
  Smartphone,
  Lock,
  CreditCard,
  Save,
  Bell,
} from "lucide-react";
import PassengerLayout from "../../components/layout/PassengerLayout";
import { useLanguage } from "../../context/LanguageContext";

export default function SettingsPage() {
  const { language, setLanguage, t } = useLanguage();
  const [emailTickets, setEmailTickets] = useState(true);
  const [tripAlerts, setTripAlerts] = useState(true);
  const [paymentAlerts, setPaymentAlerts] = useState(true);
  const [twoFactor, setTwoFactor] = useState(true);
  const [autoWallet, setAutoWallet] = useState(false);

  const saveSettings = () => {
    alert(t("passenger.settings.saved"));
  };

  return (
    <PassengerLayout activePage="Settings" title={t("passenger.settings.title")} subtitle={t("passenger.settings.subtitle")}>
      <div className="mb-7 flex justify-end">
        <button
          type="button"
          onClick={saveSettings}
          className="flex w-full items-center justify-center gap-2 rounded-xl bg-[#08264a] px-5 py-3 text-sm font-black text-white hover:bg-[#0d3566] sm:w-auto"
        >
          <Save size={18} />
          {t("passenger.settings.saveChanges")}
        </button>
      </div>

      <div className="grid grid-cols-1 gap-6 xl:grid-cols-12">
        <section className="space-y-5 xl:col-span-8">
          <SettingsCard
            icon={<Globe size={22} />}
            title={t("passenger.settings.languagePreference")}
            desc={t("passenger.settings.languageDesc")}
          >
            <div className="mt-4 grid grid-cols-1 gap-3 sm:grid-cols-2">
              {[
                ["en", t("passenger.settings.english")],
                ["ne", t("passenger.settings.nepali")],
              ].map(([value, label]) => (
                <button
                  type="button"
                  key={value}
                  onClick={() => setLanguage(value)}
                  className={`rounded-xl border px-5 py-4 text-sm font-black transition ${
                    language === value
                      ? "border-[#08264a] bg-[#08264a] text-white"
                      : "border-slate-300 bg-slate-50 text-slate-600 hover:border-[#08264a]"
                  }`}
                >
                  {label}
                </button>
              ))}
            </div>
          </SettingsCard>

          <SettingsCard
            icon={<Bell size={22} />}
            title={t("passenger.settings.notificationPreferences")}
            desc={t("passenger.settings.notificationDesc")}
          >
            <div className="mt-4 space-y-3">
              <ToggleRow
                title={t("passenger.settings.tripAlerts")}
                enabled={tripAlerts}
                setEnabled={setTripAlerts}
              />
              <ToggleRow
                title={t("passenger.settings.paymentAlerts")}
                enabled={paymentAlerts}
                setEnabled={setPaymentAlerts}
              />
              <ToggleRow
                title={t("passenger.settings.emailTicketAlerts")}
                enabled={emailTickets}
                setEnabled={setEmailTickets}
              />
            </div>
          </SettingsCard>

          <SettingsCard
            icon={<Lock size={22} />}
            title={t("passenger.settings.walletSecurity")}
            desc={t("passenger.settings.walletSecurityDesc")}
          >
            <div className="mt-4 space-y-3">
              <SettingAction
                title={t("passenger.settings.changeWalletPin")}
                desc={t("passenger.settings.changeWalletPinDesc")}
                button={t("passenger.settings.changePin")}
              />
              <ToggleRow
                title={t("passenger.settings.requireOtpLargePayments")}
                enabled={twoFactor}
                setEnabled={setTwoFactor}
              />
            </div>
          </SettingsCard>

          <SettingsCard
            icon={<CreditCard size={22} />}
            title={t("passenger.settings.paymentPreferences")}
            desc={t("passenger.settings.paymentPreferencesDesc")}
          >
            <div className="mt-4 space-y-3">
              <ToggleRow
                title={t("passenger.settings.defaultWallet")}
                enabled={autoWallet}
                setEnabled={setAutoWallet}
              />
              <SettingAction
                title={t("passenger.settings.savedPaymentMethods")}
                desc={t("passenger.settings.savedPaymentMethodsDesc")}
                button={t("passenger.settings.manage")}
              />
            </div>
          </SettingsCard>
        </section>

        <aside className="space-y-5 xl:col-span-4">
          <div className="rounded-2xl bg-[#08264a] p-6 text-white shadow-sm">
            <div className="flex h-12 w-12 items-center justify-center rounded-xl bg-white/15">
              <ShieldCheck size={26} />
            </div>

            <h2 className="mt-5 text-xl font-black">{t("passenger.settings.accountProtected")}</h2>
            <p className="mt-2 text-sm leading-6 text-slate-300">
              {t("passenger.settings.accountProtectedDesc")}
            </p>
          </div>

          <div className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">
            <h2 className="text-xl font-black text-slate-900">
              {t("passenger.settings.accountSummary")}
            </h2>

            <div className="mt-4 space-y-3 text-sm">
              <SummaryRow label={t("passenger.settings.name")} value="Nischal P." />
              <SummaryRow label={t("passenger.settings.role")} value={t("passenger.settings.passenger")} />
              <SummaryRow label={t("passenger.settings.email")} value="nischal@example.com" />
              <SummaryRow label={t("passenger.settings.phone")} value="+977 9841234567" />
            </div>
          </div>

          <div className="rounded-2xl border border-emerald-200 bg-emerald-50 p-5 text-sm text-emerald-800">
            <div className="flex gap-3">
              <Mail size={20} />
              <p>{t("passenger.settings.ticketEmailInfo")}</p>
            </div>
          </div>

          <div className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">
            <h2 className="text-xl font-black text-slate-900">{t("passenger.settings.device")}</h2>

            <div className="mt-4 flex items-center gap-3 rounded-xl bg-slate-50 p-4">
              <Smartphone size={22} />
              <div>
                <p className="text-sm font-black text-slate-900">
                  Chrome on Windows
                </p>
                <p className="text-xs text-slate-500">
                  {t("passenger.settings.currentActiveSession")}
                </p>
              </div>
            </div>
          </div>
        </aside>
      </div>
    </PassengerLayout>
  );
}

function SettingsCard({ icon, title, desc, children }) {
  return (
    <div className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">
      <div className="flex items-start gap-3">
        <div className="flex h-11 w-11 shrink-0 items-center justify-center rounded-xl bg-emerald-100 text-emerald-700">
          {icon}
        </div>

        <div>
          <h2 className="text-xl font-black text-slate-900">{title}</h2>
          <p className="mt-1 text-sm text-slate-500">{desc}</p>
        </div>
      </div>

      {children}
    </div>
  );
}

function ToggleRow({ title, enabled, setEnabled }) {
  return (
    <div className="flex items-center justify-between gap-4 rounded-xl bg-slate-50 p-4">
      <p className="text-sm font-bold text-slate-700">{title}</p>

      <button
        type="button"
        onClick={() => setEnabled(!enabled)}
        className={`flex h-7 w-12 shrink-0 items-center rounded-full p-1 transition ${
          enabled ? "justify-end bg-emerald-600" : "justify-start bg-slate-300"
        }`}
      >
        <span className="h-5 w-5 rounded-full bg-white"></span>
      </button>
    </div>
  );
}

function SettingAction({ title, desc, button }) {
  return (
    <div className="flex flex-col gap-3 rounded-xl bg-slate-50 p-4 sm:flex-row sm:items-center sm:justify-between">
      <div>
        <p className="text-sm font-black text-slate-900">{title}</p>
        <p className="mt-1 text-xs text-slate-500">{desc}</p>
      </div>

      <button type="button" className="rounded-lg bg-[#08264a] px-4 py-2 text-sm font-bold text-white hover:bg-[#0d3566]">
        {button}
      </button>
    </div>
  );
}

function SummaryRow({ label, value }) {
  return (
    <div className="flex justify-between gap-4 border-b border-slate-100 pb-2">
      <span className="text-slate-500">{label}</span>
      <span className="text-right font-black text-slate-900">{value}</span>
    </div>
  );
}
