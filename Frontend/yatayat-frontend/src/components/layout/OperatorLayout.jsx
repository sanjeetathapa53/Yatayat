import { Bus, CalendarDays, LayoutDashboard, LogOut, MapPinned, Menu, Users, X } from "lucide-react";
import { useEffect, useState } from "react";
import { useLocation, useNavigate } from "react-router-dom";
import { useAuth } from "../../hooks/useAuth";
import { logoutUser } from "../../services/authService";
import { apiFetch } from "../../utils/api";
import YatayatLogo from "../branding/YatayatLogo";

export default function OperatorLayout({ children }) {
  const navigate = useNavigate();
  const location = useLocation();
  const { user } = useAuth();
  const [sidebarOpen, setSidebarOpen] = useState(false);
  const [organizationName, setOrganizationName] = useState("");

  useEffect(() => {
    let active = true;

    apiFetch("/api/operator/dashboard")
      .then(async (response) => (response.ok ? response.json() : null))
      .then((dashboard) => {
        const name = dashboard?.organizationName?.trim();
        if (active && name) setOrganizationName(name);
      })
      .catch(() => {
        // The authenticated-user fallback keeps the sidebar stable.
      });

    return () => {
      active = false;
    };
  }, []);

  const authenticatedName = user?.fullName?.trim();
  const emailUsername = user?.email?.split("@")[0]?.trim();
  const operatorName = organizationName
    || user?.organizationName?.trim()
    || authenticatedName
    || emailUsername
    || "Operator";

  const logout = async () => {
    await logoutUser();
    navigate("/", { replace: true });
  };

  return (
    <div className="min-h-screen bg-[#f4f7fb] text-[#08264a]">
      {sidebarOpen && (
        <button
          type="button"
          aria-label="Close sidebar"
          onClick={() => setSidebarOpen(false)}
          className="fixed inset-0 z-40 bg-black/40 lg:hidden"
        />
      )}

      <aside className={`fixed left-0 top-0 z-50 flex h-dvh w-[min(19rem,calc(100vw-2rem))] flex-col border-r border-slate-200 bg-white transition-transform lg:h-screen lg:w-64 lg:translate-x-0 ${sidebarOpen ? "translate-x-0" : "-translate-x-full"}`}>
        <div className="flex items-center justify-between border-b border-slate-200 px-5 py-4">
          <button type="button" onClick={() => { navigate("/operator/dashboard"); setSidebarOpen(false); }} aria-label="Yatayat operator dashboard" className="rounded-lg text-left focus-visible:outline-2 focus-visible:outline-offset-4 focus-visible:outline-[#08264a]">
            <YatayatLogo variant="compact" size="md" />
            <span className="mt-1 block pl-[3.125rem] text-xs font-semibold text-slate-500">Operations Portal</span>
          </button>
          <button type="button" onClick={() => setSidebarOpen(false)} className="p-2 lg:hidden">
            <X size={20} />
          </button>
        </div>

        <nav className="min-h-0 flex-1 space-y-1 overflow-y-auto p-3">
          <NavItem icon={<LayoutDashboard size={19} />} label="Dashboard" path="/operator/dashboard" active={location.pathname === "/operator/dashboard"} onNavigate={(path) => { navigate(path); setSidebarOpen(false); }} />
          <NavItem icon={<Bus size={19} />} label="Buses" path="/operator/buses" active={location.pathname.startsWith("/operator/buses")} onNavigate={(path) => { navigate(path); setSidebarOpen(false); }} />
          <NavItem icon={<Users size={19} />} label="Drivers" path="/operator/drivers" active={location.pathname.startsWith("/operator/drivers")} onNavigate={(path) => { navigate(path); setSidebarOpen(false); }} />
          <NavItem icon={<CalendarDays size={19} />} label="Local Services" path="/operator/local-services" active={location.pathname.startsWith("/operator/local-services")} onNavigate={(path) => { navigate(path); setSidebarOpen(false); }} />
          <NavItem icon={<CalendarDays size={19} />} label="Trips" path="/operator/trips" active={location.pathname.startsWith("/operator/trips")} onNavigate={(path) => { navigate(path); setSidebarOpen(false); }} />
          <NavItem icon={<MapPinned size={19} />} label="Live Fleet" path="/operator/live-fleet" active={location.pathname === "/operator/live-fleet"} onNavigate={(path) => { navigate(path); setSidebarOpen(false); }} />
        </nav>

        <div className="border-t border-slate-200 p-4">
          <div className="mb-3 flex items-center gap-3 rounded-2xl bg-slate-50 p-3">
            <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-xl bg-[#08264a] text-sm font-semibold text-white">
              {getInitials(operatorName)}
            </div>
            <div className="min-w-0">
              <p className="truncate text-sm font-semibold text-slate-900" title={operatorName}>
                {operatorName}
              </p>
              <p className="mt-0.5 text-[11px] font-medium uppercase tracking-wide text-slate-500">OPERATOR</p>
            </div>
          </div>

          <button type="button" onClick={logout} className="flex w-full items-center justify-center gap-2 rounded-xl border border-red-200 bg-red-50 h-10 text-sm font-semibold text-red-600">
            <LogOut size={18} /> Logout
          </button>
        </div>
      </aside>

      <div className="min-h-screen lg:ml-64">
        <header className="flex items-center gap-3 border-b border-slate-200 bg-white px-4 py-3 sm:px-6 lg:px-8">
          <button type="button" aria-label="Open operator menu" onClick={() => setSidebarOpen(true)} className="tap-target rounded-xl border border-slate-200 p-2.5 lg:hidden">
            <Menu size={20} />
          </button>
          <YatayatLogo variant="icon" size="sm" />
          <h2 className="truncate text-lg font-semibold sm:text-xl">Operator Dashboard</h2>
        </header>
        <main className="px-4 py-5 sm:px-6 lg:px-8"><div className="responsive-shell">{children}</div></main>
      </div>
    </div>
  );
}

function NavItem({ icon, label, path, active, onNavigate }) {
  return (
    <button type="button" onClick={() => onNavigate(path)} className={`tap-target flex w-full items-center gap-3 rounded-xl px-4 h-10 text-sm ${active ? "bg-[#08264a] font-semibold text-white" : "font-medium text-slate-600 hover:bg-slate-100"}`}>
      {icon} {label}
    </button>
  );
}

function getInitials(name) {
  const words = String(name || "")
    .trim()
    .split(/\s+/)
    .filter(Boolean);

  if (words.length >= 2) {
    return `${words[0][0]}${words[1][0]}`.toUpperCase();
  }

  return words[0]?.slice(0, 2).toUpperCase() || "OP";
}
