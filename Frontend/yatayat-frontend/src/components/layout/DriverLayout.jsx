import { useCallback, useEffect, useMemo, useState } from "react";
import {
  LayoutDashboard,
  QrCode,
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
import { toast } from "react-toastify";
import { logoutUser } from "../../services/authService";
import { useLanguage } from "../../hooks/useLanguage";
import { API_BASE_URL } from "../../utils/api";

export default function DriverLayout({
  children,
  activePage = "Dashboard",
}) {
  const navigate = useNavigate();
  const location = useLocation();
  const { language, setLanguage, t } = useLanguage();

  const [menuOpen, setMenuOpen] = useState(false);
  const [driver, setDriver] = useState(null);
  const [loadingDriver, setLoadingDriver] = useState(true);
  const [loggingOut, setLoggingOut] = useState(false);

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

  const fetchDriverProfile = useCallback(async () => {
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
          data.message || t("driver.layout.unableProfile")
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
          loggedInUser.fullName || t("driver.layout.driverUser"),
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
  }, [loggedInUser, t]);

  useEffect(() => {
    const timer = window.setTimeout(() => {
      void fetchDriverProfile();
    }, 0);
    return () => window.clearTimeout(timer);
  }, [fetchDriverProfile]);

  const go = (path) => {
    navigate(path);
    setMenuOpen(false);
  };

  const handleLogout = async () => {
    if (!window.confirm(t("driver.layout.logoutConfirm"))) return;

    setLoggingOut(true);
    await logoutUser();
    toast.success(t("common.loggedOut"));
    navigate("/", { replace: true });
  };

  const menuItems = [
    {
      label: t("driver.layout.dashboard"),
      activeKey: "Dashboard",
      icon: <LayoutDashboard size={20} />,
      path: "/driver/dashboard",
    },
    {
      label: t("driver.layout.scanner"),
      activeKey: "Scanner",
      icon: <QrCode size={20} />,
      path: "/driver/scanner",
    },
    {
      label: t("driver.layout.tripManagement"),
      activeKey: "Trip Management",
      icon: <Route size={20} />,
      path: "/driver/trip",
    },
    {
      label: t("driver.layout.localServices"),
      activeKey: "Local Services",
      icon: <Bus size={20} />,
      path: "/driver/local-services",
    },
    {
      label: t("common.notifications"),
      activeKey: "Notifications",
      icon: <Bell size={20} />,
      path: "/driver/notifications",
    },
    {
      label: t("common.profile"),
      activeKey: "Profile",
      icon: <UserCircle size={20} />,
      path: "/driver/profile",
    },
    {
      label: t("common.settings"),
      activeKey: "Settings",
      icon: <Settings size={20} />,
      path: "/driver/settings",
    },
  ];

  const currentDriverName =
    driver?.fullName ||
    loggedInUser?.fullName ||
    t("driver.layout.driverUser");

  const currentDriverEmail =
    driver?.email ||
    loggedInUser?.email ||
    t("common.noEmail");

  const applicationCode = driver?.applicationId
    ? `DRV-${driver.applicationId}`
    : t("common.applicationUnavailable");

  const verificationStatus =
    driver?.verificationStatus || "UNKNOWN";

  return (
    <div className="min-h-screen bg-[#f3f6fa] text-[#08264a]">
      <header className="fixed left-0 top-0 z-40 h-16 w-full border-b border-slate-200 bg-white">
        <div className="flex h-full items-center justify-between gap-3 px-3 sm:px-4 lg:px-6">
          <div className="flex min-w-0 items-center gap-3 sm:gap-4">
            <button
              type="button"
              onClick={() => setMenuOpen(true)}
              className="rounded-xl p-2 transition hover:bg-slate-100 lg:hidden"
              aria-label={t("driver.layout.openMenu")}
            >
              <Menu size={22} />
            </button>

            <button
              type="button"
              onClick={() => go("/driver/dashboard")}
              className="text-left"
            >
              <h1 className="truncate text-xl font-black text-[#08264a] sm:text-2xl">
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
                {t("driver.layout.dashboard")}
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
                {t("driver.layout.currentTrip")}
              </button>

            </div>
          </div>

          <div className="flex shrink-0 items-center gap-2 sm:gap-3">
            <LanguageToggle
              language={language}
              setLanguage={setLanguage}
              label={t("common.changeLanguage")}
            />

            <StatusBadge status={verificationStatus} t={t} />

            <button
              type="button"
              onClick={() => go("/driver/notifications")}
              className="relative rounded-full p-2 transition hover:bg-slate-100"
              aria-label={t("common.openNotifications")}
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
        className={`fixed left-0 top-16 z-50 flex h-[calc(100dvh-64px)] w-[min(18rem,calc(100vw-2rem))] flex-col justify-between overflow-y-auto border-r border-slate-200 bg-[#061a33] p-3 text-white transition-transform duration-300 lg:h-[calc(100vh-64px)] lg:w-64 lg:translate-x-0 ${
          menuOpen ? "translate-x-0" : "-translate-x-full"
        }`}
      >
        <div>
          <div className="mb-3 flex items-center justify-between lg:hidden">
            <h2 className="text-xl font-black">
              {t("driver.layout.menu")}
            </h2>

            <button
              type="button"
              onClick={() => setMenuOpen(false)}
            >
              <X size={22} />
            </button>
          </div>

          <nav className="space-y-1">
            {menuItems.map((item) => {
              const isActive =
                activePage === item.activeKey ||
                location.pathname === item.path;

              return (
                <button
                  type="button"
                  key={item.activeKey}
                  onClick={() => go(item.path)}
                  className={`flex w-full items-center gap-3 rounded-xl px-4 py-2.5 text-sm font-semibold transition ${
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

        <div className="mt-2 border-t border-white/10 pt-3">
          <button
            type="button"
            onClick={() => go("/driver/profile")}
            className="mb-2 flex w-full items-center gap-3 rounded-xl bg-white/10 p-2.5 text-left transition hover:bg-white/15"
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
                  ? t("driver.layout.loadingDriver")
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

          <div className="mb-2 flex items-center gap-2 rounded-xl bg-emerald-500/10 px-3 py-2 text-xs font-bold text-emerald-300">
            <ShieldCheck size={15} />
            {t(`status.${verificationStatus}`, { defaultValue: formatStatus(verificationStatus) })}
          </div>

          <button
            type="button"
            onClick={handleLogout}
            disabled={loggingOut}
            className="flex w-full items-center justify-center gap-2 rounded-xl border border-red-400/30 bg-red-500/10 py-2.5 text-sm font-black text-red-300 transition hover:bg-red-500/20 disabled:cursor-not-allowed disabled:opacity-60"
          >
            {loggingOut ? (
              <Loader2 size={20} className="animate-spin" />
            ) : (
              <LogOut size={20} />
            )}
            {loggingOut ? t("driver.layout.loggingOut") : t("common.logout")}
          </button>
        </div>
      </aside>

      <main className="min-h-screen px-4 pb-6 pt-20 sm:px-6 lg:ml-64 lg:px-7">
        <div className="responsive-shell">{children}</div>
      </main>
    </div>
  );
}

function StatusBadge({ status, t }) {
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
      {t(`status.${status}`)}
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

function LanguageToggle({ language, setLanguage, label }) {
  return (
    <div
      role="group"
      aria-label={label}
      className="flex rounded-full bg-slate-100 p-1 text-xs font-bold"
    >
      <button
        type="button"
        onClick={() => setLanguage("en")}
        className={`rounded-full px-2.5 py-1 transition ${
          language === "en"
            ? "bg-[#08264a] text-white"
            : "text-slate-600 hover:text-[#08264a]"
        }`}
      >
        EN
      </button>
      <button
        type="button"
        onClick={() => setLanguage("ne")}
        className={`rounded-full px-2.5 py-1 transition ${
          language === "ne"
            ? "bg-[#08264a] text-white"
            : "text-slate-600 hover:text-[#08264a]"
        }`}
      >
        नेपाली
      </button>
    </div>
  );
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
