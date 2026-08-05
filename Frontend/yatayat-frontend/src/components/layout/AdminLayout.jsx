import { useState } from "react";
import {
  Activity,
  Bus,
  Route,
  UserCheck,
  Map,
  LogOut,
  Menu,
  X,
  Building2,
  Users,
  TicketCheck,
  Banknote,
  ChartNoAxesCombined,
} from "lucide-react";
import { useLocation, useNavigate } from "react-router-dom";
import { logoutUser } from "../../services/authService";
import YatayatLogo from "../branding/YatayatLogo";

const menuSections = [
  {
    label: "Dashboard",
    items: [
      {
        label: "Dashboard",
        icon: Activity,
        path: "/admin/dashboard",
      },
    ],
  },
  {
    label: "Operations",
    items: [
      {
        label: "Driver Applications",
        icon: UserCheck,
        path: "/admin/driver-applications",
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
        label: "Live Monitoring",
        icon: Map,
        path: "/admin/live-monitoring",
      },
    ],
  },
  {
    label: "Analytics",
    items: [
      {
        label: "Users",
        icon: Users,
        path: "/admin/analytics/users",
      },
      {
        label: "Bookings",
        icon: TicketCheck,
        path: "/admin/analytics/bookings",
      },
      {
        label: "Revenue",
        icon: Banknote,
        path: "/admin/analytics/revenue",
      },
      {
        label: "Fleet",
        icon: ChartNoAxesCombined,
        path: "/admin/analytics/operations",
      },
    ],
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

  const admin = JSON.parse(
    localStorage.getItem("yatayatAdmin") || "null"
  );

  const handleLogout = async () => {
    await logoutUser({ admin: true });
    navigate("/admin/login", { replace: true });
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
        className={`fixed left-0 top-0 z-50 flex h-dvh w-[min(19rem,calc(100vw-2rem))] flex-col border-r border-slate-200 bg-white transition-transform duration-300 lg:h-screen lg:w-72 lg:translate-x-0 ${
          sidebarOpen ? "translate-x-0" : "-translate-x-full"
        }`}
      >
        <div className="flex items-center justify-between border-b border-slate-200 px-6 py-5">
          <button
            type="button"
            onClick={() => goTo("/admin/dashboard")}
            aria-label="Yatayat admin dashboard"
            className="rounded-lg text-left focus-visible:outline-2 focus-visible:outline-offset-4 focus-visible:outline-[#08264a]"
          >
            <YatayatLogo variant="compact" size="md" />
            <p className="mt-1 pl-[3.125rem] text-xs font-semibold text-slate-500">
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

        <nav className="min-h-0 flex-1 overflow-y-auto px-3 py-4">
          <div className="space-y-5">
            {menuSections.map((section, sectionIndex) => (
              <div key={section.label || `primary-${sectionIndex}`}>
                {section.label && (
                  <p className="mb-2 px-4 text-[10px] font-black uppercase tracking-[0.18em] text-slate-400">
                    {section.label}
                  </p>
                )}
                <div className="space-y-1">
                  {section.items.map((item) => {
                    const Icon = item.icon;
                    const active = isActiveRoute(location.pathname, item.path);

                    return (
                      <button
                        type="button"
                        key={item.path}
                        onClick={() => goTo(item.path)}
                        aria-current={active ? "page" : undefined}
                        className={`flex w-full items-center gap-3 rounded-xl px-4 py-2.5 text-left text-sm font-black transition focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-[#08264a] ${
                          active
                            ? "bg-[#08264a] text-white shadow-sm"
                            : "text-slate-600 hover:bg-slate-100 hover:text-[#08264a]"
                        }`}
                      >
                        <Icon size={18} aria-hidden="true" />
                        <span className="truncate">{item.label}</span>
                      </button>
                    );
                  })}
                </div>
              </div>
            ))}
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
                {admin?.email || "Administrator"}
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
              aria-label="Open admin menu"
              className="tap-target rounded-xl border border-slate-200 p-3 text-slate-600 hover:bg-slate-100 lg:hidden"
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

          </div>
        </header>

        <main className="px-4 py-6 sm:px-6 lg:px-8">
          <div className="responsive-shell">{children}</div>
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

function isActiveRoute(currentPath, itemPath) {
  return currentPath === itemPath || currentPath.startsWith(`${itemPath}/`);
}
