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

export default function ProfilePage() {
  const navigate = useNavigate();

  const [twoFA, setTwoFA] = useState(true);
  const [alerts, setAlerts] = useState(true);

  const [showPasswordModal, setShowPasswordModal] = useState(false);
  const [oldPassword, setOldPassword] = useState("");
  const [newPassword, setNewPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");

  const storedUser = localStorage.getItem("yatayatUser");
  const user = storedUser ? JSON.parse(storedUser) : null;

  const fullName = user?.fullName || "User";
  const firstName = fullName.split(" ")[0];
  const email = user?.email || "";
  const phone = user?.phone || "No phone available";
  const role = user?.role === "DRIVER" ? "Driver Account" : "Passenger Account";

  const handleLogout = () => {
    localStorage.removeItem("yatayatUser");
    localStorage.removeItem("loginTime");
    navigate("/login");
  };

  const handleChangePassword = async () => {
    if (!oldPassword || !newPassword || !confirmPassword) {
      toast.error("Please fill all password fields");
      return;
    }

    if (newPassword !== confirmPassword) {
      toast.error("New password and confirm password do not match");
      return;
    }

    try {
      const response = await fetch("http://localhost:8080/api/auth/change-password", {
        method: "POST",
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
        toast.success("Password changed successfully");
        setShowPasswordModal(false);
        setOldPassword("");
        setNewPassword("");
        setConfirmPassword("");
      } else {
        toast.error(message);
      }
    } catch (error) {
      console.error(error);
      toast.error("Failed to change password");
    }
  };

  return (
    <PassengerLayout activePage="Profile">
      <div className="mb-5">
        <h1 className="text-2xl font-black text-slate-900 sm:text-3xl">
          User Profile
        </h1>
        <p className="mt-1 text-sm text-slate-600">
          Manage your account, security, language, and notification settings.
        </p>
      </div>

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
              <ProfileStat label="Trips" value="0" />
              <ProfileStat label="QR Passes" value="0" />
            </div>
          </div>

          <div className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">
            <h2 className="text-lg font-black text-slate-900">
              Quick Actions
            </h2>

            <div className="mt-4 space-y-3">
              <QuickButton
                icon={<ShieldCheck size={18} />}
                label="Security Settings"
                onClick={() => navigate("/settings")}
              />
              <QuickButton
                icon={<Globe size={18} />}
                label="Language Preference"
                onClick={() => navigate("/settings")}
              />
              <QuickButton
                icon={<Bell size={18} />}
                label="Notification Settings"
                onClick={() => navigate("/settings")}
              />
            </div>

            <button
              onClick={handleLogout}
              className="mt-5 flex w-full items-center justify-center gap-2 rounded-xl bg-red-100 py-3 text-sm font-black text-red-700 hover:bg-red-200"
            >
              <LogOut size={17} />
              Logout
            </button>
          </div>
        </aside>

        <section className="space-y-5 xl:col-span-8">
          <div className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">
            <h2 className="text-xl font-black text-slate-900">
              Personal Information
            </h2>

            <div className="mt-5 grid grid-cols-1 gap-4 md:grid-cols-2">
              <InputBox icon={<UserCircle size={18} />} label="Full Name" value={fullName} />
              <InputBox icon={<Mail size={18} />} label="Email" value={email} />
              <InputBox icon={<Phone size={18} />} label="Phone" value={phone} />
              <InputBox icon={<Globe size={18} />} label="Preferred Language" value="English / नेपाली" />
            </div>

            <button className="mt-5 flex w-full items-center justify-center gap-2 rounded-xl bg-[#08264a] px-5 py-3 text-sm font-black text-white hover:bg-[#0d3566] sm:w-auto">
              <Save size={17} />
              Save Changes
            </button>
          </div>

          <div className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">
            <h2 className="text-xl font-black text-slate-900">Security</h2>

            <div className="mt-5 space-y-4">
              <SettingRow
                icon={<Lock size={20} />}
                title="Change Password"
                desc="Update your login password regularly."
                button="Change"
                onClick={() => setShowPasswordModal(true)}
              />

              <ToggleRow
                icon={<ShieldCheck size={20} />}
                title="Two-Factor Authentication"
                desc="Add extra security using OTP verification."
                enabled={twoFA}
                setEnabled={setTwoFA}
              />

              <ToggleRow
                icon={<Bell size={20} />}
                title="Trip and Payment Alerts"
                desc="Receive notifications for fare pass, wallet and bus delays."
                enabled={alerts}
                setEnabled={setAlerts}
              />
            </div>
          </div>

          <div className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">
            <h2 className="text-xl font-black text-slate-900">
              Device Login History
            </h2>

            <div className="mt-4 space-y-3">
              <DeviceRow
                device="Chrome on Windows"
                location="Kathmandu, Nepal"
                status="Current Device"
              />
            </div>
          </div>
        </section>
      </div>

      {showPasswordModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40 px-4">
          <div className="w-full max-w-md rounded-2xl bg-white p-6 shadow-xl">
            <h2 className="text-xl font-black text-slate-900">
              Change Password
            </h2>
            <p className="mt-1 text-sm text-slate-500">
              Enter your old password and create a new password.
            </p>

            <div className="mt-5 space-y-4">
              <input
                type="password"
                placeholder="Old password"
                value={oldPassword}
                onChange={(e) => setOldPassword(e.target.value)}
                className="w-full rounded-xl border border-slate-300 px-4 py-3 text-sm outline-none"
              />

              <input
                type="password"
                placeholder="New password"
                value={newPassword}
                onChange={(e) => setNewPassword(e.target.value)}
                className="w-full rounded-xl border border-slate-300 px-4 py-3 text-sm outline-none"
              />

              <input
                type="password"
                placeholder="Confirm new password"
                value={confirmPassword}
                onChange={(e) => setConfirmPassword(e.target.value)}
                className="w-full rounded-xl border border-slate-300 px-4 py-3 text-sm outline-none"
              />
            </div>

            <div className="mt-6 flex gap-3">
              <button
                onClick={() => setShowPasswordModal(false)}
                className="w-full rounded-xl border border-slate-300 py-3 text-sm font-bold text-slate-600 hover:bg-slate-50"
              >
                Cancel
              </button>

              <button
                onClick={handleChangePassword}
                className="w-full rounded-xl bg-[#08264a] py-3 text-sm font-bold text-white hover:bg-[#0d3566]"
              >
                Update
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