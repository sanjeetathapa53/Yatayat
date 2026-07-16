import { useEffect, useMemo, useState } from "react";
import {
  LayoutDashboard,
  QrCode,
  Users,
  Route,
  Bell,
  UserCircle,
  Settings,
  LogOut,
  Menu,
  X,
  Bus,
  Loader2,
  ShieldCheck,
} from "lucide-react";
import { useLocation, useNavigate } from "react-router-dom";

const API_BASE_URL = "http://localhost:8080";

export default function DriverLayout({
  children,
  activePage = "Dashboard",
}) {
  const navigate = useNavigate();
  const location = useLocation();

  const [menuOpen, setMenuOpen] = useState(false);
  const [driver, setDriver] = useState(null);
  const [loadingDriver, setLoadingDriver] = useState(true);

  const loggedInUser = useMemo(() => {
    try {
      return JSON.parse(
        localStorage.getItem("yatayatUser") || "null"
      );
    } catch (error) {
      console.error("Invalid driver data:", error);
      return null;
    }
  }, []);

  const fetchDriverProfile = async () => {
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
          data.message || "Unable to load driver profile."
        );
      }

      setDriver(data.driver);

      // Keep login storage updated with the latest basic information.
      localStorage.setItem(
        "yatayatUser",
        JSON.stringify({
          ...loggedInUser,
          fullName:
            data.driver?.fullName || loggedInUser.fullName,
          email: data.driver?.email || loggedInUser.email,
          phone: data.driver?.phone || loggedInUser.phone,
        })
      );

      localStorage.setItem(
        "driverApplicationStatus",
        data.driver?.verificationStatus || "NOT_SUBMITTED"
      );
    } catch (error) {
      console.error("Driver profile loading error:", error);

      // Use login response as a fallback so the layout still works.
      setDriver({
        userId: loggedInUser.id,
        fullName:
          loggedInUser.fullName || "Driver User",
        email: loggedInUser.email || "",
        phone: loggedInUser.phone || "",
        role: loggedInUser.role || "DRIVER",
        verificationStatus:
          localStorage.getItem("driverApplicationStatus") ||
          "UNKNOWN",
      });
    } finally {
      setLoadingDriver(false);
    }
  };

  useEffect(() => {
    fetchDriverProfile();
  }, []);

  const go = (path) => {
    navigate(path);
    setMenuOpen(false);
  };

  const handleLogout = () => {
    localStorage.removeItem("yatayatUser");
    localStorage.removeItem("loginTime");
    localStorage.removeItem("driverApplicationStatus");

    navigate("/login", {
      replace: true,
    });
  };

  const menuItems = [
    {
      label: "Dashboard",
      icon: <LayoutDashboard size={20} />,
      path: "/driver/dashboard",
    },
    {
      label: "Scanner",
      icon: <QrCode size={20} />,
      path: "/driver/scanner",
    },
    {
      label: "Passenger List",
      icon: <Users size={20} />,
      path: "/driver/passengers",
    },
    {
      label: "Trip Management",
      icon: <Route size={20} />,
      path: "/driver/trip",
    },
    {
      label: "Notifications",
      icon: <Bell size={20} />,
      path: "/driver/notifications",
    },
    {
      label: "Profile",
      icon: <UserCircle size={20} />,
      path: "/driver/profile",
    },
    {
      label: "Settings",
      icon: <Settings size={20} />,
      path: "/driver/settings",
    },
  ];

  const currentDriverName =
    driver?.fullName ||
    loggedInUser?.fullName ||
    "Driver User";

  const currentDriverEmail =
    driver?.email ||
    loggedInUser?.email ||
    "No email available";

  const applicationCode = driver?.applicationId
    ? `DRV-${driver.applicationId}`
    : "Application unavailable";

  const verificationStatus =
    driver?.verificationStatus || "UNKNOWN";

  return (
    <div className="min-h-screen bg-[#f3f6fa] text-[#08264a]">
      <header className="fixed left-0 top-0 z-40 h-16 w-full border-b border-slate-200 bg-white">
        <div className="flex h-full items-center justify-between px-4 lg:px-6">
          <div className="flex items-center gap-4">
            <button
              type="button"
              onClick={() => setMenuOpen(true)}
              className="rounded-xl p-2 transition hover:bg-slate-100 lg:hidden"
              aria-label="Open driver menu"
            >
              <Menu size={22} />
            </button>

            <button
              type="button"
              onClick={() => go("/driver/dashboard")}
              className="text-left"
            >
              <h1 className="text-2xl font-black text-[#08264a]">
                Yatayat
              </h1>
            </button>

            <div className="hidden items-center gap-6 text-sm font-bold lg:flex">
              <button
                type="button"
                onClick={() => go("/driver/dashboard")}
                className={
                  location.pathname === "/driver/dashboard"
                    ? "border-b-2 border-[#08264a] pb-5 text-[#08264a]"
                    : "text-slate-500 transition hover:text-[#08264a]"
                }
              >
                Dashboard
              </button>

              <button
                type="button"
                onClick={() => go("/driver/trip")}
                className={
                  location.pathname === "/driver/trip"
                    ? "border-b-2 border-[#08264a] pb-5 text-[#08264a]"
                    : "text-slate-500 transition hover:text-[#08264a]"
                }
              >
                Current Trip
              </button>

              <button
                type="button"
                className="cursor-not-allowed text-slate-400"
                title="Performance reports will be connected later"
              >
                Performance
              </button>
            </div>
          </div>

          <div className="flex items-center gap-3">
            <StatusBadge status={verificationStatus} />

            <button
              type="button"
              onClick={() => go("/driver/notifications")}
              className="relative rounded-full p-2 transition hover:bg-slate-100"
              aria-label="Open notifications"
            >
              <Bell size={20} />

              <span className="absolute right-1.5 top-1.5 h-2 w-2 rounded-full bg-red-500" />
            </button>

            <button
              type="button"
              onClick={() => go("/driver/profile")}
              className="flex h-10 w-10 items-center justify-center rounded-full bg-[#08264a] text-xs font-black text-white transition hover:bg-[#0d3566]"
              title={currentDriverName}
            >
              {loadingDriver ? (
                <Loader2 size={19} className="animate-spin" />
              ) : (
                getInitials(currentDriverName)
              )}
            </button>
          </div>
        </div>
      </header>

      {menuOpen && (
        <button
          type="button"
          aria-label="Close driver menu"
          onClick={() => setMenuOpen(false)}
          className="fixed inset-0 z-40 bg-black/40 lg:hidden"
        />
      )}

      <aside
        className={`fixed left-0 top-16 z-50 flex h-[calc(100vh-64px)] w-64 flex-col justify-between border-r border-slate-200 bg-[#061a33] p-4 text-white transition-transform duration-300 lg:translate-x-0 ${
          menuOpen ? "translate-x-0" : "-translate-x-full"
        }`}
      >
        <div>
          <div className="mb-6 flex items-center justify-between lg:hidden">
            <h2 className="text-xl font-black">
              Driver Menu
            </h2>

            <button
              type="button"
              onClick={() => setMenuOpen(false)}
            >
              <X size={22} />
            </button>
          </div>

          <nav className="space-y-2">
            {menuItems.map((item) => {
              const isActive =
                activePage === item.label ||
                location.pathname === item.path;

              return (
                <button
                  type="button"
                  key={item.label}
                  onClick={() => go(item.path)}
                  className={`flex w-full items-center gap-3 rounded-xl px-4 py-3 text-sm font-semibold transition ${
                    isActive
                      ? "bg-white/15 text-white"
                      : "text-slate-300 hover:bg-white/10 hover:text-white"
                  }`}
                >
                  {item.icon}
                  {item.label}
                </button>
              );
            })}
          </nav>
        </div>

        <div className="border-t border-white/10 pt-4">
          <button
            type="button"
            onClick={() => go("/driver/profile")}
            className="mb-4 flex w-full items-center gap-3 rounded-xl bg-white/10 p-3 text-left transition hover:bg-white/15"
          >
            <div className="flex h-11 w-11 shrink-0 items-center justify-center rounded-xl bg-blue-200 text-sm font-black text-[#08264a]">
              {loadingDriver ? (
                <Loader2 size={18} className="animate-spin" />
              ) : (
                getInitials(currentDriverName)
              )}
            </div>

            <div className="min-w-0">
              <p className="truncate text-sm font-black">
                {loadingDriver
                  ? "Loading driver..."
                  : currentDriverName}
              </p>

              <p className="mt-0.5 truncate text-xs text-slate-300">
                {applicationCode}
              </p>

              <p className="mt-0.5 truncate text-[11px] text-slate-400">
                {currentDriverEmail}
              </p>
            </div>
          </button>

          <div className="mb-3 flex items-center gap-2 rounded-xl bg-emerald-500/10 px-3 py-2 text-xs font-bold text-emerald-300">
            <ShieldCheck size={15} />
            {formatStatus(verificationStatus)}
          </div>

          <button
            type="button"
            onClick={handleLogout}
            className="flex w-full items-center gap-3 rounded-xl px-4 py-3 text-sm font-semibold text-slate-300 transition hover:bg-red-500/10 hover:text-red-300"
          >
            <LogOut size={20} />
            Logout
          </button>
        </div>
      </aside>

      <main className="min-h-screen px-4 pb-6 pt-20 sm:px-6 lg:ml-64 lg:px-7">
        {children}
      </main>
    </div>
  );
}

function StatusBadge({ status }) {
  const statusStyles = {
    APPROVED:
      "bg-emerald-100 text-emerald-700",
    PENDING:
      "bg-amber-100 text-amber-700",
    REJECTED:
      "bg-red-100 text-red-700",
    SUSPENDED:
      "bg-slate-200 text-slate-700",
  };

  return (
    <span
      className={`hidden rounded-full px-3 py-1 text-xs font-black sm:inline-flex ${
        statusStyles[status] ||
        "bg-slate-100 text-slate-600"
      }`}
    >
      {formatStatus(status)}
    </span>
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

function formatStatus(status) {
  switch (status) {
    case "APPROVED":
      return "Verified Driver";
    case "PENDING":
      return "Approval Pending";
    case "REJECTED":
      return "Application Rejected";
    case "SUSPENDED":
      return "Driver Suspended";
    case "NOT_SUBMITTED":
      return "Profile Incomplete";
    default:
      return "Driver Account";
  }
}
