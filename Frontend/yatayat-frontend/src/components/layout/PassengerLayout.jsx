import { useState } from "react";
import { toast } from "react-toastify";
import {
  LayoutDashboard,
  Route,
  Wallet,
  History,
  Settings,
  LogOut,
  Bell,
  Bus,
  Ticket,
  UserCircle,
  Menu,
  X,
} from "lucide-react";
import { useNavigate } from "react-router-dom";
import { logoutUser } from "../../services/authService";

export default function PassengerLayout({ children, activePage = "Dashboard" }) {
  const navigate = useNavigate();
  const [menuOpen, setMenuOpen] = useState(false);

  const storedUser = localStorage.getItem("yatayatUser");
  const user = storedUser ? JSON.parse(storedUser) : null;

  const fullName = user?.fullName || "User";
  const role = user?.role || "PASSENGER";

  const initials = fullName
    .split(" ")
    .map((name) => name[0])
    .join("")
    .substring(0, 2)
    .toUpperCase();

  const go = (path) => {
    navigate(path);
    setMenuOpen(false);
  };

  const handleLogout = async () => {
    await logoutUser();
    toast.success("Logged out successfully");
    navigate("/", { replace: true });
  };

  const menuItems = [
    {
      label: "Dashboard",
      icon: <LayoutDashboard size={20} />,
      path: "/passenger/dashboard",
    },
    {
      label: "Find Local Route",
      icon: <Route size={20} />,
      path: "/passenger/local-routes",
    },
    {
      label: "Wallet",
      icon: <Wallet size={20} />,
      path: "/wallet",
    },
    {
      label: "Book Out-of-Valley",
      icon: <Bus size={20} />,
      path: "/passenger/out-of-valley",
    },
    {
      label: "My Bookings",
      icon: <Ticket size={20} />,
      path: "/passenger/bookings",
    },
    {
      label: "History",
      icon: <History size={20} />,
      path: "/history",
    },
    {
      label: "Settings",
      icon: <Settings size={20} />,
      path: "/settings",
    },
  ];

  return (
    <div className="min-h-screen bg-[#f3f6fa] text-[#08264a]">
      <button
        onClick={() => setMenuOpen(true)}
        className="fixed left-4 top-4 z-50 rounded-xl bg-white p-3 shadow-md lg:hidden"
      >
        <Menu size={22} />
      </button>

      {menuOpen && (
        <div
          onClick={() => setMenuOpen(false)}
          className="fixed inset-0 z-40 bg-black/40 lg:hidden"
        />
      )}

      <aside
        className={`fixed left-0 top-0 z-50 flex h-screen w-64 flex-col justify-between border-r border-slate-200 bg-white p-4 transition-transform duration-300 lg:translate-x-0 ${
          menuOpen ? "translate-x-0" : "-translate-x-full"
        }`}
      >
        <div>
          <div className="flex items-start justify-between">
            <button onClick={() => go("/passenger/dashboard")} className="text-left">
              <h1 className="text-3xl font-black">Yatayat</h1>
              <p className="mt-2 text-sm text-slate-500">
                Smart Urban Transit
              </p>
            </button>

            <button onClick={() => setMenuOpen(false)} className="lg:hidden">
              <X size={22} />
            </button>
          </div>

          <nav className="mt-8 space-y-2">
            {menuItems.map((item) => (
              <button
                key={item.label}
                onClick={() => go(item.path)}
                className={`flex w-full items-center gap-3 rounded-lg px-4 py-3 text-sm font-semibold transition ${
                  activePage === item.label
                    ? "bg-[#1d3f6e] text-white"
                    : "text-slate-600 hover:bg-slate-100 hover:text-[#08264a]"
                }`}
              >
                {item.icon}
                {item.label}
              </button>
            ))}
          </nav>
        </div>

        <div className="border-t border-slate-200 pt-4">
          <button
            onClick={() => go("/profile")}
            className="flex w-full items-center gap-3 rounded-xl bg-slate-50 p-3 text-left transition hover:bg-slate-100"
          >
            <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-[#1d3f6e] text-sm font-bold text-white">
              {initials}
            </div>

            <div>
              <p className="text-sm font-bold">{fullName}</p>
              <p className="text-[10px] uppercase text-slate-500">
                {role}
              </p>
            </div>
          </button>

          <button
            onClick={handleLogout}
            className="mt-4 flex w-full items-center justify-center gap-2 rounded-xl border border-red-200 bg-red-50 py-3 text-sm font-black text-red-600 transition hover:bg-red-100"
            title="Logout"
          >
            <LogOut size={18} />
            Logout
          </button>
        </div>
      </aside>

      <main className="min-h-screen px-4 py-5 pt-20 sm:px-6 lg:ml-64 lg:px-7 lg:py-6 lg:pt-6">
        <div className="mb-5 flex justify-end gap-3 lg:hidden">
          <button
            onClick={() => go("/notifications")}
            className="rounded-full bg-white p-2 shadow-sm"
          >
            <Bell size={20} />
          </button>

          <button
            onClick={() => go("/profile")}
            className="rounded-xl bg-emerald-100 p-2 text-emerald-700"
          >
            <UserCircle size={22} />
          </button>
        </div>

        {children}
      </main>
    </div>
  );
}
