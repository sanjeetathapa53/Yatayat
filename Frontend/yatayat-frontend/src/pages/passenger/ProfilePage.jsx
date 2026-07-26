import { useState } from "react";
import {
  UserCircle,
  Mail,
  Phone,
  ShieldCheck,
  Lock,
  Globe,
  Bell,
  Smartphone,
  LogOut,
  Save,
} from "lucide-react";
import { useNavigate } from "react-router-dom";
import { toast } from "react-toastify";
import PassengerLayout from "../../components/layout/PassengerLayout";
import { useLanguage } from "../../context/LanguageContext";
import { logoutUser } from "../../services/authService";

export default function ProfilePage() {
  const navigate = useNavigate();
  const { t } = useLanguage();

  const [twoFA, setTwoFA] = useState(true);
  const [alerts, setAlerts] = useState(true);

  const [showPasswordModal, setShowPasswordModal] = useState(false);
  const [oldPassword, setOldPassword] = useState("");
  const [newPassword, setNewPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");

  const storedUser = localStorage.getItem("yatayatUser");
  const user = storedUser ? JSON.parse(storedUser) : null;

  const fullName = user?.fullName || t("passenger.profile.userFallback");
  const firstName = fullName.split(" ")[0];
  const email = user?.email || "";
  const phone = user?.phone || t("passenger.profile.noPhone");
  const role = user?.role === "DRIVER" ? t("passenger.profile.driverAccount") : t("passenger.profile.passengerAccount");

  const handleLogout = async () => {
    await logoutUser();
    toast.success(t("common.loggedOut"));
    navigate("/", { replace: true });
  };

  const handleChangePassword = async () => {
    if (!oldPassword || !newPassword || !confirmPassword) {
      toast.error(t("passenger.profile.fillPasswordFields"));
      return;
    }

    if (newPassword !== confirmPassword) {
      toast.error(t("passenger.profile.passwordMismatch"));
      return;
    }

    try {
      const response = await fetch("http://localhost:8080/api/auth/change-password", {
        method: "POST",
        credentials: "include",
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify({
          email,
          oldPassword,
          newPassword,
        }),
      });

      const message = await response.text();

      if (message === "Password changed successfully") {
        toast.success(t("passenger.profile.passwordChanged"));
        setShowPasswordModal(false);
        setOldPassword("");
        setNewPassword("");
        setConfirmPassword("");
      } else {
        toast.error(message);
      }
    } catch (error) {
      console.error(error);
      toast.error(t("passenger.profile.passwordChangeFailed"));
    }
  };

  return (
    <PassengerLayout activePage="Profile" title={t("passenger.profile.title")} subtitle={t("passenger.profile.subtitle")}>
      <div className="grid grid-cols-1 gap-5 xl:grid-cols-12">
        <aside className="space-y-5 xl:col-span-4">
          <div className="rounded-2xl bg-[#08264a] p-6 text-white shadow-sm">
            <div className="flex items-center gap-4">
              <div className="flex h-16 w-16 shrink-0 items-center justify-center rounded-2xl bg-white/15">
                <UserCircle size={42} />
              </div>

              <div>
                <h2 className="text-xl font-black">{firstName}</h2>
                <p className="text-sm text-slate-300">{role}</p>
              </div>
            </div>

            <div className="mt-6 grid grid-cols-2 gap-3">
              <ProfileStat label={t("passenger.profile.trips")} value="0" />
              <ProfileStat label={t("passenger.profile.qrPasses")} value="0" />
            </div>
          </div>

          <div className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">
            <h2 className="text-lg font-black text-slate-900">
              {t("passenger.profile.quickActions")}
            </h2>

            <div className="mt-4 space-y-3">
              <QuickButton
                icon={<ShieldCheck size={18} />}
                label={t("passenger.profile.securitySettings")}
                onClick={() => navigate("/settings")}
              />
              <QuickButton
                icon={<Globe size={18} />}
                label={t("passenger.profile.languagePreference")}
                onClick={() => navigate("/settings")}
              />
              <QuickButton
                icon={<Bell size={18} />}
                label={t("passenger.profile.notificationSettings")}
                onClick={() => navigate("/settings")}
              />
            </div>

            <button
              type="button"
              onClick={handleLogout}
              className="mt-5 flex w-full items-center justify-center gap-2 rounded-xl bg-red-100 py-3 text-sm font-black text-red-700 hover:bg-red-200"
            >
              <LogOut size={17} />
              {t("passenger.profile.logout")}
            </button>
          </div>
        </aside>

        <section className="space-y-5 xl:col-span-8">
          <div className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">
            <h2 className="text-xl font-black text-slate-900">
              {t("passenger.profile.personalInformation")}
            </h2>

            <div className="mt-5 grid grid-cols-1 gap-4 md:grid-cols-2">
              <InputBox icon={<UserCircle size={18} />} label={t("passenger.profile.fullName")} value={fullName} />
              <InputBox icon={<Mail size={18} />} label={t("passenger.profile.email")} value={email} />
              <InputBox icon={<Phone size={18} />} label={t("passenger.profile.phone")} value={phone} />
              <InputBox icon={<Globe size={18} />} label={t("passenger.profile.preferredLanguage")} value={t("passenger.profile.languageValue")} />
            </div>

            <button type="button" className="mt-5 flex w-full items-center justify-center gap-2 rounded-xl bg-[#08264a] px-5 py-3 text-sm font-black text-white hover:bg-[#0d3566] sm:w-auto">
              <Save size={17} />
              {t("passenger.profile.saveChanges")}
            </button>
          </div>

          <div className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">
            <h2 className="text-xl font-black text-slate-900">{t("passenger.profile.security")}</h2>

            <div className="mt-5 space-y-4">
              <SettingRow
                icon={<Lock size={20} />}
                title={t("passenger.profile.changePassword")}
                desc={t("passenger.profile.changePasswordDesc")}
                button={t("passenger.profile.change")}
                onClick={() => setShowPasswordModal(true)}
              />

              <ToggleRow
                icon={<ShieldCheck size={20} />}
                title={t("passenger.profile.twoFactor")}
                desc={t("passenger.profile.twoFactorDesc")}
                enabled={twoFA}
                setEnabled={setTwoFA}
              />

              <ToggleRow
                icon={<Bell size={20} />}
                title={t("passenger.profile.tripPaymentAlerts")}
                desc={t("passenger.profile.tripPaymentAlertsDesc")}
                enabled={alerts}
                setEnabled={setAlerts}
              />
            </div>
          </div>

          <div className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">
            <h2 className="text-xl font-black text-slate-900">
              {t("passenger.profile.deviceLoginHistory")}
            </h2>

            <div className="mt-4 space-y-3">
              <DeviceRow
                device="Chrome on Windows"
                location="Kathmandu, Nepal"
                status={t("passenger.profile.currentDevice")}
              />
            </div>
          </div>
        </section>
      </div>

      {showPasswordModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40 px-4">
          <div className="w-full max-w-md rounded-2xl bg-white p-6 shadow-xl">
            <h2 className="text-xl font-black text-slate-900">
              {t("passenger.profile.changePassword")}
            </h2>
            <p className="mt-1 text-sm text-slate-500">
              {t("passenger.profile.changePasswordHelp")}
            </p>

            <div className="mt-5 space-y-4">
              <input
                type="password"
                placeholder={t("passenger.profile.oldPassword")}
                value={oldPassword}
                onChange={(e) => setOldPassword(e.target.value)}
                className="w-full rounded-xl border border-slate-300 px-4 py-3 text-sm outline-none"
              />

              <input
                type="password"
                placeholder={t("passenger.profile.newPassword")}
                value={newPassword}
                onChange={(e) => setNewPassword(e.target.value)}
                className="w-full rounded-xl border border-slate-300 px-4 py-3 text-sm outline-none"
              />

              <input
                type="password"
                placeholder={t("passenger.profile.confirmNewPassword")}
                value={confirmPassword}
                onChange={(e) => setConfirmPassword(e.target.value)}
                className="w-full rounded-xl border border-slate-300 px-4 py-3 text-sm outline-none"
              />
            </div>

            <div className="mt-6 flex gap-3">
              <button
                type="button"
                onClick={() => setShowPasswordModal(false)}
                className="w-full rounded-xl border border-slate-300 py-3 text-sm font-bold text-slate-600 hover:bg-slate-50"
              >
                {t("passenger.profile.cancel")}
              </button>

              <button
                type="button"
                onClick={handleChangePassword}
                className="w-full rounded-xl bg-[#08264a] py-3 text-sm font-bold text-white hover:bg-[#0d3566]"
              >
                {t("passenger.profile.update")}
              </button>
            </div>
          </div>
        </div>
      )}
    </PassengerLayout>
  );
}

function ProfileStat({ label, value }) {
  return (
    <div className="rounded-xl bg-white/10 p-4">
      <p className="text-xs font-bold uppercase text-slate-300">{label}</p>
      <h3 className="mt-1 text-2xl font-black">{value}</h3>
    </div>
  );
}

function QuickButton({ icon, label, onClick }) {
  return (
    <button
      type="button"
      onClick={onClick}
      className="flex w-full items-center gap-3 rounded-xl bg-slate-50 p-3 text-sm font-bold text-slate-700 hover:bg-slate-100"
    >
      {icon}
      {label}
    </button>
  );
}

function InputBox({ icon, label, value }) {
  return (
    <div>
      <label className="text-xs font-black uppercase tracking-widest text-slate-500">
        {label}
      </label>

      <div className="mt-2 flex items-center gap-3 rounded-xl border border-slate-300 bg-slate-50 px-4 py-3">
        <span className="shrink-0 text-slate-500">{icon}</span>
        <input
          value={value}
          readOnly
          className="w-full min-w-0 bg-transparent text-sm font-bold outline-none"
        />
      </div>
    </div>
  );
}

function SettingRow({ icon, title, desc, button, onClick }) {
  return (
    <div className="flex flex-col gap-4 rounded-xl bg-slate-50 p-4 sm:flex-row sm:items-center sm:justify-between">
      <div className="flex items-center gap-3">
        <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-xl bg-white text-[#08264a]">
          {icon}
        </div>

        <div>
          <h3 className="font-black text-slate-900">{title}</h3>
          <p className="text-sm text-slate-500">{desc}</p>
        </div>
      </div>

      <button
        type="button"
        onClick={onClick}
        className="rounded-lg bg-[#08264a] px-4 py-2 text-sm font-bold text-white hover:bg-[#0d3566]"
      >
        {button}
      </button>
    </div>
  );
}

function ToggleRow({ icon, title, desc, enabled, setEnabled }) {
  return (
    <div className="flex items-center justify-between gap-4 rounded-xl bg-slate-50 p-4">
      <div className="flex items-center gap-3">
        <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-xl bg-white text-[#08264a]">
          {icon}
        </div>

        <div>
          <h3 className="font-black text-slate-900">{title}</h3>
          <p className="text-sm text-slate-500">{desc}</p>
        </div>
      </div>

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

function DeviceRow({ device, location, status }) {
  return (
    <div className="flex flex-col gap-3 rounded-xl bg-slate-50 p-4 sm:flex-row sm:items-center sm:justify-between">
      <div className="flex items-center gap-3">
        <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-xl bg-white text-[#08264a]">
          <Smartphone size={19} />
        </div>

        <div>
          <h3 className="font-black text-slate-900">{device}</h3>
          <p className="text-sm text-slate-500">{location}</p>
        </div>
      </div>

      <span className="w-fit rounded-full bg-emerald-100 px-3 py-1 text-xs font-black text-emerald-700">
        {status}
      </span>
    </div>
  );
}
