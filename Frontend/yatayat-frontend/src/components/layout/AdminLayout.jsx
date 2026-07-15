import { useState } from "react";
import {
  Activity,
  Bus,
  Route,
  Users,
  UserCheck,
  Map,
  Ticket,
  Wallet,
  BarChart3,
  Settings,
  LogOut,
  Menu,
  X,
  Bell,
  Search,
  ChevronRight,
  Building2 ,
} from "lucide-react";
import { useLocation, useNavigate } from "react-router-dom";
import { apiFetch } from "../../utils/api";

const menuItems = [
  {
    label: "Dashboard",
    icon: Activity,
    path: "/admin/dashboard",
  },
  {
    label: "Driver Applications",
    icon: UserCheck,
    path: "/admin/driver-applications",
  },
  {
    label: "Drivers",
    icon: Users,
    path: "/admin/drivers",
  },
  {
    label: "Transport Operators",
    icon: Building2,
    path: "/admin/operators",
  },
  {
    label: "Buses",
    icon: Bus,
    path: "/admin/buses",
  },
  {
    label: "Routes & Stops",
    icon: Route,
    path: "/admin/routes",
  },
  {
    label: "Trips",
    icon: ChevronRight,
    path: "/admin/trips",
  },
  {
    label: "Live Tracking",
    icon: Map,
    path: "/admin/live-tracking",
  },
  {
    label: "Bookings",
    icon: Ticket,
    path: "/admin/bookings",
  },
  {
    label: "Passengers",
    icon: Users,
    path: "/admin/passengers",
  },
  {
    label: "Wallet & Payments",
    icon: Wallet,
    path: "/admin/payments",
  },
  {
    label: "Reports",
    icon: BarChart3,
    path: "/admin/reports",
  },
  {
    label: "Settings",
    icon: Settings,
    path: "/admin/settings",
  },
];

export default function AdminLayout({
  children,
  title = "Admin Dashboard",
  subtitle = "Manage and monitor the Yatayat platform.",
}) {
  const navigate = useNavigate();
  const location = useLocation();

  const [sidebarOpen, setSidebarOpen] = useState(false);
  const [search, setSearch] = useState("");

  const admin = JSON.parse(
    localStorage.getItem("yatayatAdmin") || "null"
  );

  const handleLogout = async () => {
    await apiFetch("/api/admin/auth/logout", { method: "POST" }).catch(() => {});
    localStorage.removeItem("yatayatAdmin");
    localStorage.removeItem("adminAuthenticated");
    navigate("/admin/login");
  };

  const goTo = (path) => {
    navigate(path);
    setSidebarOpen(false);
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

      <aside
        className={`fixed left-0 top-0 z-50 flex h-screen w-72 flex-col border-r border-slate-200 bg-white transition-transform duration-300 lg:translate-x-0 ${
          sidebarOpen ? "translate-x-0" : "-translate-x-full"
        }`}
      >
        <div className="flex items-center justify-between border-b border-slate-200 px-6 py-5">
          <button
            type="button"
            onClick={() => goTo("/admin/dashboard")}
            className="text-left"
          >
            <h1 className="text-2xl font-black leading-tight text-[#08264a]">
              Yatayat Admin
            </h1>
            <p className="mt-1 text-xs font-semibold text-slate-500">
              Central Control Hub
            </p>
          </button>

          <button
            type="button"
            onClick={() => setSidebarOpen(false)}
            className="rounded-lg p-2 text-slate-500 hover:bg-slate-100 lg:hidden"
          >
            <X size={20} />
          </button>
        </div>

        <nav className="flex-1 overflow-y-auto px-3 py-5">
          <div className="space-y-1">
            {menuItems.map((item) => {
              const Icon = item.icon;

              const active =
                location.pathname === item.path ||
                location.pathname.startsWith(`${item.path}/`);

              return (
                <button
                  type="button"
                  key={item.path}
                  onClick={() => goTo(item.path)}
                  className={`flex w-full items-center gap-3 rounded-xl px-4 py-3 text-left text-sm font-black transition ${
                    active
                      ? "bg-[#08264a] text-white shadow-sm"
                      : "text-slate-600 hover:bg-slate-100 hover:text-[#08264a]"
                  }`}
                >
                  <Icon size={19} />
                  <span>{item.label}</span>
                </button>
              );
            })}
          </div>
        </nav>

        <div className="border-t border-slate-200 p-5">
          <div className="mb-4 flex items-center gap-3 rounded-2xl bg-slate-50 p-3">
            <div className="flex h-11 w-11 shrink-0 items-center justify-center rounded-full bg-[#08264a] text-sm font-black text-white">
              {getInitials(admin?.fullName || "Admin User")}
            </div>

            <div className="min-w-0">
              <p className="truncate text-sm font-black text-slate-900">
                {admin?.fullName || "Admin User"}
              </p>

              <p className="truncate text-xs text-slate-500">
                {admin?.email || "admin@yatayat.com"}
              </p>
            </div>
          </div>

          <button
            type="button"
            onClick={handleLogout}
            className="flex w-full items-center justify-center gap-2 rounded-xl border border-red-200 bg-red-50 py-3 text-sm font-black text-red-600 transition hover:bg-red-100"
          >
            <LogOut size={18} />
            Logout
          </button>
        </div>
      </aside>

      <div className="min-h-screen lg:ml-72">
        <header className="sticky top-0 z-30 border-b border-slate-200 bg-white/95 px-4 py-4 backdrop-blur sm:px-6 lg:px-8">
          <div className="flex items-center gap-3">
            <button
              type="button"
              onClick={() => setSidebarOpen(true)}
              className="rounded-xl border border-slate-200 p-3 text-slate-600 hover:bg-slate-100 lg:hidden"
            >
              <Menu size={20} />
            </button>

            <div className="min-w-0 flex-1">
              <h2 className="truncate text-xl font-black text-[#08264a] sm:text-2xl">
                {title}
              </h2>

              <p className="mt-1 hidden text-sm text-slate-500 sm:block">
                {subtitle}
              </p>
            </div>

            <div className="hidden w-full max-w-sm items-center gap-3 rounded-xl border border-slate-300 bg-slate-50 px-4 py-3 md:flex">
              <Search size={18} className="shrink-0 text-slate-500" />

              <input
                value={search}
                onChange={(event) => setSearch(event.target.value)}
                type="search"
                placeholder="Search users, drivers, buses..."
                className="w-full bg-transparent text-sm outline-none"
              />
            </div>

            <button
              type="button"
              onClick={() => navigate("/admin/notifications")}
              className="relative rounded-xl border border-slate-200 bg-white p-3 text-slate-600 transition hover:bg-slate-100"
            >
              <Bell size={19} />

              <span className="absolute right-2 top-2 h-2 w-2 rounded-full bg-red-500" />
            </button>
          </div>
        </header>

        <main className="px-4 py-6 sm:px-6 lg:px-8">
          {children}
        </main>
      </div>
    </div>
  );
}

function getInitials(name) {
  return name
    .split(" ")
    .filter(Boolean)
    .slice(0, 2)
    .map((part) => part[0]?.toUpperCase())
    .join("");
}
