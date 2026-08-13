import { useCallback, useEffect, useMemo, useState } from "react";
import {
  Shield,
  MapPin,
  Bell,
  Save,
  Info,
  ChevronDown,
  Lock,
  AlertTriangle,
  MessageSquare,
  Route,
  UserCircle,
  Mail,
  Phone,
  BadgeCheck,
  Loader2,
  RotateCcw,
} from "lucide-react";
import { toast } from "react-toastify";
import DriverLayout from "../../components/layout/DriverLayout";
import { API_BASE_URL } from "../../utils/api";

const defaultSettings = {
  twoFactor: false,
  autoLocation: true,
  routeUpdates: true,
  emergencyAlerts: true,
  passengerMessages: false,
  trackingFrequency: "5",
};

export default function DriverSettingsPage() {
  const loggedInUser = useMemo(() => {
    try {
      return JSON.parse(
        localStorage.getItem("yatayatUser") || "null"
      );
    } catch {
      return null;
    }
  }, []);

  const [driver, setDriver] = useState(null);
  const [loadingDriver, setLoadingDriver] = useState(true);
  const [saving, setSaving] = useState(false);

  const [settings, setSettings] = useState(() => {
    try {
      const savedSettings = JSON.parse(
        localStorage.getItem("driverSettings") || "null"
      );

      return savedSettings
        ? {
            ...defaultSettings,
            ...savedSettings,
          }
        : defaultSettings;
    } catch {
      return defaultSettings;
    }
  });

  const fetchDriver = useCallback(async () => {
    if (!loggedInUser?.id) {
      setLoadingDriver(false);
      return;
    }

    try {
      setLoadingDriver(true);

      const response = await fetch(
        `${API_BASE_URL}/api/drivers/profile/${loggedInUser.id}`,
        { credentials: "include" }
      );

      const data = await response.json();

      if (!response.ok || !data.success) {
        throw new Error(
          data.message || "Unable to load driver account."
        );
      }

      setDriver(data.driver);
    } catch (error) {
      console.error("Driver settings profile error:", error);

      setDriver({
        userId: loggedInUser.id,
        fullName:
          loggedInUser.fullName || "Driver User",
        email: loggedInUser.email || "",
        phone: loggedInUser.phone || "",
        role: loggedInUser.role || "DRIVER",
      });

      toast.error(
        "Some driver account information could not be loaded."
      );
    } finally {
      setLoadingDriver(false);
    }
  }, [loggedInUser]);

  useEffect(() => {
    const timer = window.setTimeout(() => {
      void fetchDriver();
    }, 0);
    return () => window.clearTimeout(timer);
  }, [fetchDriver]);

  const updateSetting = (field, value) => {
    setSettings((current) => ({
      ...current,
      [field]: value,
    }));
  };

  const saveSettings = async () => {
    try {
      setSaving(true);

      // For now these preferences are stored locally.
      // Later this can be replaced with a backend settings endpoint.
      localStorage.setItem(
        "driverSettings",
        JSON.stringify(settings)
      );

      await new Promise((resolve) =>
        setTimeout(resolve, 500)
      );

      toast.success(
        "Driver preferences saved successfully."
      );
    } catch (error) {
      console.error(error);
      toast.error("Unable to save driver settings.");
    } finally {
      setSaving(false);
    }
  };

  const discardChanges = () => {
    try {
      const savedSettings = JSON.parse(
        localStorage.getItem("driverSettings") || "null"
      );

      setSettings(
        savedSettings
          ? {
              ...defaultSettings,
              ...savedSettings,
            }
          : defaultSettings
      );

      toast.info("Unsaved changes were discarded.");
    } catch {
      setSettings(defaultSettings);
    }
  };

  const resetSettings = () => {
    setSettings(defaultSettings);
    localStorage.removeItem("driverSettings");

    toast.info("Driver settings restored to defaults.");
  };

  return (
    <DriverLayout activePage="Settings">
      <header className="mb-5">
        <h1 className="text-3xl font-semibold text-slate-900">
          Driver Preferences
        </h1>

        <p className="mt-1 text-sm text-slate-600">
          Configure your account, operational tools, notifications
          and security preferences.
        </p>
      </header>

      {/* REAL DRIVER ACCOUNT */}

      <section className="mb-5 overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-sm">
        <div className="bg-[#08264a] p-4 text-white sm:p-5">
          <div className="flex flex-col gap-4 sm:flex-row sm:items-center">
            <div className="flex h-16 w-16 shrink-0 items-center justify-center rounded-2xl bg-white/15 text-xl font-semibold">
              {loadingDriver ? (
                <Loader2 size={26} className="animate-spin" />
              ) : (
                getInitials(driver?.fullName)
              )}
            </div>

            <div className="min-w-0 flex-1">
              <p className="text-xs font-semibold uppercase tracking-[0.18em] text-blue-200">
                Logged-in Driver Account
              </p>

              <h2 className="mt-1.5 truncate text-xl font-semibold">
                {loadingDriver
                  ? "Loading account..."
                  : driver?.fullName || "Driver User"}
              </h2>

              <p className="mt-1 truncate text-sm text-slate-300">
                {driver?.email || loggedInUser?.email || ""}
              </p>
            </div>

            <span className="w-fit rounded-full bg-emerald-100 px-3 py-1.5 text-xs font-semibold text-emerald-700">
              {driver?.verificationStatus === "APPROVED"
                ? "VERIFIED DRIVER"
                : driver?.verificationStatus || "DRIVER"}
            </span>
          </div>
        </div>

        <div className="grid grid-cols-1 gap-3 p-4 sm:grid-cols-2 xl:grid-cols-4">
          <AccountDetail
            icon={<Mail size={18} />}
            label="Email"
            value={driver?.email}
          />

          <AccountDetail
            icon={<Phone size={18} />}
            label="Phone"
            value={driver?.phone}
          />

          <AccountDetail
            icon={<BadgeCheck size={18} />}
            label="Licence Number"
            value={driver?.licenseNumber}
          />

          <AccountDetail
            icon={<MapPin size={18} />}
            label="Operating Area"
            value={driver?.preferredOperatingArea}
          />
        </div>
      </section>

      <section>
        <SettingCard icon={<Shield size={20} />} title="Account & Security">
          <div className="mt-4">
            <label className="text-xs font-semibold uppercase tracking-wider text-slate-500">Password</label>
            <div className="mt-2 flex items-center justify-between rounded-xl border border-slate-200 bg-slate-50 px-4 py-3">
              <span className="font-semibold tracking-widest text-slate-700">••••••••••</span>
              <button type="button" onClick={() => toast.info("Password change will be connected to the backend next.")} className="text-sm font-semibold text-[#1d3f6e] hover:underline">Change</button>
            </div>
          </div>

          <div className="mt-3 flex items-center justify-between gap-4 rounded-xl border border-slate-200 bg-slate-50 p-3">
            <div className="flex items-center gap-3">
              <Lock size={20} className="text-[#1d3f6e]" />
              <div><h3 className="font-semibold text-slate-900">Two-Factor Authentication</h3><p className="text-sm text-slate-500">Add an extra verification step during login.</p></div>
            </div>
            <Toggle enabled={settings.twoFactor} setEnabled={(value) => updateSetting("twoFactor", value)} />
          </div>

          <div className="mt-3 rounded-xl border border-blue-200 bg-blue-50 p-3">
            <div className="flex items-start gap-3">
              <UserCircle size={20} className="mt-0.5 shrink-0 text-[#1d3f6e]" />
              <div><p className="font-semibold text-slate-900">Account information</p><p className="mt-1 text-sm leading-5 text-slate-600">Personal and licence information is managed from your verified driver profile.</p></div>
            </div>
          </div>
        </SettingCard>
      </section>

      <section className="mt-5 grid grid-cols-1 items-start gap-4 lg:grid-cols-2">
        <SettingCard
          icon={<MapPin size={20} />}
          title="GPS & Tracking"
        >
          <div className="mt-4">
            <label className="text-xs font-semibold uppercase tracking-wider text-slate-500">
              Tracking Frequency
            </label>

            <div className="relative mt-2">
              <select
                value={settings.trackingFrequency}
                onChange={(event) =>
                  updateSetting(
                    "trackingFrequency",
                    event.target.value
                  )
                }
                className="w-full appearance-none rounded-xl border border-slate-200 bg-slate-50 h-11 px-3 pr-10 text-sm font-semibold text-slate-700 outline-none focus:border-[#08264a]"
              >
                <option value="5">
                  Real-time — Every 5 seconds
                </option>
                <option value="10">
                  Every 10 seconds
                </option>
                <option value="30">
                  Every 30 seconds
                </option>
                <option value="60">
                  Every 1 minute
                </option>
              </select>

              <ChevronDown
                size={18}
                className="pointer-events-none absolute right-4 top-1/2 -translate-y-1/2 text-slate-500"
              />
            </div>
          </div>

          <div className="mt-4 flex items-center justify-between gap-4">
            <div>
              <p className="font-semibold text-slate-800">
                Auto-Location Sharing
              </p>

              <p className="mt-1 text-xs text-slate-500">
                Share location automatically during active trips.
              </p>
            </div>

            <Toggle
              enabled={settings.autoLocation}
              setEnabled={(value) =>
                updateSetting("autoLocation", value)
              }
            />
          </div>
        </SettingCard>

        <SettingCard
          icon={<Bell size={20} />}
          title="Notifications"
        >
          <div className="mt-4 space-y-3">
            <CheckRow
              icon={<Route size={21} />}
              label="Route Updates"
              checked={settings.routeUpdates}
              setChecked={(value) =>
                updateSetting("routeUpdates", value)
              }
            />

            <CheckRow
              icon={<AlertTriangle size={21} />}
              label="Emergency Alerts"
              checked={settings.emergencyAlerts}
              setChecked={(value) =>
                updateSetting("emergencyAlerts", value)
              }
            />

            <CheckRow
              icon={<MessageSquare size={21} />}
              label="Passenger Messaging"
              checked={settings.passengerMessages}
              setChecked={(value) =>
                updateSetting("passengerMessages", value)
              }
            />
          </div>
        </SettingCard>
      </section>

      <section className="mt-5 rounded-2xl border border-slate-200 border-t-4 border-[#1d3f6e] bg-white p-4 shadow-sm">
        <div className="flex flex-col gap-3 lg:flex-row lg:items-center lg:justify-between">
          <p className="flex items-center gap-2 text-sm text-slate-500">
            <Info size={18} />
            Preferences are currently saved on this device.
          </p>

          <div className="flex flex-wrap gap-2">
            <button
              type="button"
              onClick={resetSettings}
              disabled={saving}
              className="inline-flex h-10 items-center justify-center gap-2 w-fit rounded-xl border border-slate-300 px-4 text-sm font-semibold text-slate-600 transition hover:bg-slate-50 disabled:opacity-60"
            >
              <RotateCcw size={17} />
              Reset Defaults
            </button>

            <button
              type="button"
              onClick={discardChanges}
              disabled={saving}
              className="h-10 w-fit rounded-xl border border-[#1d3f6e] px-4 text-sm font-semibold text-[#1d3f6e] transition hover:bg-blue-50 disabled:opacity-60"
            >
              Discard
            </button>

            <button
              type="button"
              onClick={saveSettings}
              disabled={saving}
              className="inline-flex h-10 items-center justify-center gap-2 w-fit rounded-xl bg-[#08264a] px-4 text-sm font-semibold text-white shadow-sm transition hover:bg-[#0d3566] disabled:cursor-not-allowed disabled:opacity-60"
            >
              {saving ? (
                <Loader2 size={18} className="animate-spin" />
              ) : (
                <Save size={18} />
              )}

              {saving ? "Saving..." : "Save Settings"}
            </button>
          </div>
        </div>
      </section>
    </DriverLayout>
  );
}

function AccountDetail({ icon, label, value }) {
  return (
    <div className="rounded-xl bg-slate-50 p-3">
      <div className="flex items-center gap-2 text-slate-500">
        {icon}

        <p className="text-[10px] font-semibold uppercase tracking-wider">
          {label}
        </p>
      </div>

      <p className="mt-1.5 wrap-break-word text-sm font-semibold text-slate-900">
        {value || "Not available"}
      </p>
    </div>
  );
}

function SettingCard({ icon, title, children }) {
  return (
    <div className="rounded-2xl border border-slate-200 bg-white p-4 shadow-sm">
      <div className="flex items-center gap-3">
        <div className="text-[#08264a]">{icon}</div>

        <h2 className="text-lg font-semibold text-slate-900">
          {title}
        </h2>
      </div>

      {children}
    </div>
  );
}

function Toggle({ enabled, setEnabled }) {
  return (
    <button
      type="button"
      onClick={() => setEnabled(!enabled)}
      className={`flex h-7 w-12 shrink-0 items-center rounded-full p-1 transition ${
        enabled
          ? "justify-end bg-emerald-600"
          : "justify-start bg-slate-300"
      }`}
      aria-pressed={enabled}
    >
      <span className="h-5 w-5 rounded-full bg-white shadow-sm" />
    </button>
  );
}

function CheckRow({
  icon,
  label,
  checked,
  setChecked,
}) {
  return (
    <div className="flex items-center justify-between gap-4">
      <div className="flex items-center gap-3 text-slate-700">
        <span className="text-[#1d3f6e]">{icon}</span>
        <span className="font-semibold">{label}</span>
      </div>

      <button
        type="button"
        onClick={() => setChecked(!checked)}
        className={`flex h-6 w-6 items-center justify-center rounded border text-sm font-black transition ${
          checked
            ? "border-[#1d3f6e] bg-[#1d3f6e] text-white"
            : "border-slate-300 bg-white text-transparent"
        }`}
        aria-pressed={checked}
      >
        ✓
      </button>
    </div>
  );
}

function getInitials(name) {
  return String(name || "Driver")
    .split(" ")
    .filter(Boolean)
    .slice(0, 2)
    .map((part) => part[0]?.toUpperCase())
    .join("");
}
