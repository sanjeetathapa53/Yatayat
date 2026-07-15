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

export default function SettingsPage() {
  const [language, setLanguage] = useState("English");
  const [emailTickets, setEmailTickets] = useState(true);
  const [tripAlerts, setTripAlerts] = useState(true);
  const [paymentAlerts, setPaymentAlerts] = useState(true);
  const [twoFactor, setTwoFactor] = useState(true);
  const [autoWallet, setAutoWallet] = useState(false);

  const saveSettings = () => {
    alert("Settings saved successfully.");
  };

  return (
    <PassengerLayout activePage="Settings">
      <header className="mb-7 flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <h1 className="text-2xl font-black text-slate-900 sm:text-3xl">
            Settings
          </h1>
          <p className="mt-1 text-sm text-slate-600">
            Manage account preferences, notifications, wallet security, and email tickets.
          </p>
        </div>

        <button
          onClick={saveSettings}
          className="flex w-full items-center justify-center gap-2 rounded-xl bg-[#08264a] px-5 py-3 text-sm font-black text-white hover:bg-[#0d3566] sm:w-auto"
        >
          <Save size={18} />
          Save Changes
        </button>
      </header>

      <div className="grid grid-cols-1 gap-6 xl:grid-cols-12">
        <section className="space-y-5 xl:col-span-8">
          <SettingsCard
            icon={<Globe size={22} />}
            title="Language Preference"
            desc="Choose your preferred interface language."
          >
            <div className="mt-4 grid grid-cols-1 gap-3 sm:grid-cols-2">
              {["English", "नेपाली"].map((item) => (
                <button
                  key={item}
                  onClick={() => setLanguage(item)}
                  className={`rounded-xl border px-5 py-4 text-sm font-black transition ${
                    language === item
                      ? "border-[#08264a] bg-[#08264a] text-white"
                      : "border-slate-300 bg-slate-50 text-slate-600 hover:border-[#08264a]"
                  }`}
                >
                  {item}
                </button>
              ))}
            </div>
          </SettingsCard>

          <SettingsCard
            icon={<Bell size={22} />}
            title="Notification Preferences"
            desc="Control which alerts you want to receive."
          >
            <div className="mt-4 space-y-3">
              <ToggleRow
                title="Trip delay and live bus alerts"
                enabled={tripAlerts}
                setEnabled={setTripAlerts}
              />
              <ToggleRow
                title="Payment and wallet alerts"
                enabled={paymentAlerts}
                setEnabled={setPaymentAlerts}
              />
              <ToggleRow
                title="Email ticket delivery alerts"
                enabled={emailTickets}
                setEnabled={setEmailTickets}
              />
            </div>
          </SettingsCard>

          <SettingsCard
            icon={<Lock size={22} />}
            title="Wallet Security"
            desc="Manage PIN and protection for wallet payments."
          >
            <div className="mt-4 space-y-3">
              <SettingAction
                title="Change Wallet PIN"
                desc="Update your 4-digit wallet payment PIN."
                button="Change PIN"
              />
              <ToggleRow
                title="Require OTP for large wallet payments"
                enabled={twoFactor}
                setEnabled={setTwoFactor}
              />
            </div>
          </SettingsCard>

          <SettingsCard
            icon={<CreditCard size={22} />}
            title="Payment Preferences"
            desc="Set default payment behavior for ticket bookings."
          >
            <div className="mt-4 space-y-3">
              <ToggleRow
                title="Use Yatayat Wallet as default payment method"
                enabled={autoWallet}
                setEnabled={setAutoWallet}
              />
              <SettingAction
                title="Saved Payment Methods"
                desc="eSewa, Khalti, Mobile Banking integration will be added later."
                button="Manage"
              />
            </div>
          </SettingsCard>
        </section>

        <aside className="space-y-5 xl:col-span-4">
          <div className="rounded-2xl bg-[#08264a] p-6 text-white shadow-sm">
            <div className="flex h-12 w-12 items-center justify-center rounded-xl bg-white/15">
              <ShieldCheck size={26} />
            </div>

            <h2 className="mt-5 text-xl font-black">Account Protected</h2>
            <p className="mt-2 text-sm leading-6 text-slate-300">
              Two-factor authentication and wallet PIN are currently enabled.
            </p>
          </div>

          <div className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">
            <h2 className="text-xl font-black text-slate-900">
              Account Summary
            </h2>

            <div className="mt-4 space-y-3 text-sm">
              <SummaryRow label="Name" value="Nischal P." />
              <SummaryRow label="Role" value="Passenger" />
              <SummaryRow label="Email" value="nischal@example.com" />
              <SummaryRow label="Phone" value="+977 9841234567" />
            </div>
          </div>

          <div className="rounded-2xl border border-emerald-200 bg-emerald-50 p-5 text-sm text-emerald-800">
            <div className="flex gap-3">
              <Mail size={20} />
              <p>
                Ticket PDFs and QR tickets will automatically be sent to your registered email after payment.
              </p>
            </div>
          </div>

          <div className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">
            <h2 className="text-xl font-black text-slate-900">Device</h2>

            <div className="mt-4 flex items-center gap-3 rounded-xl bg-slate-50 p-4">
              <Smartphone size={22} />
              <div>
                <p className="text-sm font-black text-slate-900">
                  Chrome on Windows
                </p>
                <p className="text-xs text-slate-500">
                  Current active session
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

      <button className="rounded-lg bg-[#08264a] px-4 py-2 text-sm font-bold text-white hover:bg-[#0d3566]">
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