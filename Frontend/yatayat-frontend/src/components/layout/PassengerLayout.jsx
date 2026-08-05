import { useState } from "react";
import { toast } from "react-toastify";
import {
  LayoutDashboard,
  Route,
  Wallet,
  Settings,
  LogOut,
  Bell,
  Bus,
  Ticket,
  UserCircle,
  ChevronDown,
  Menu,
  X,
} from "lucide-react";
import { useLocation, useNavigate } from "react-router-dom";
import { logoutUser } from "../../services/authService";
import { useLanguage } from "../../hooks/useLanguage";
import { useAuth } from "../../hooks/useAuth";
import { useNotifications } from "../../context/NotificationContext";
import YatayatLogo from "../branding/YatayatLogo";

export default function PassengerLayout({ children, activePage = "Dashboard", title, subtitle }) {
  const navigate = useNavigate();
  const { pathname } = useLocation();
  const { language, setLanguage, t } = useLanguage();
  const { user } = useAuth();
  const { unreadCount } = useNotifications();
  const [menuOpen, setMenuOpen] = useState(false);
  const [profileMenuOpen, setProfileMenuOpen] = useState(false);

  const fullName = user?.fullName || "User";
  const role = user?.role || "PASSENGER";
  const firstName = fullName.split(" ")[0];
  const fallbackHeader = getPageHeader(pathname, activePage, firstName, language);
  const pageHeader = {
    title: title || fallbackHeader.title,
    subtitle: subtitle || fallbackHeader.subtitle,
  };

  const initials = fullName
    .split(" ")
    .map((name) => name[0])
    .join("")
    .substring(0, 2)
    .toUpperCase();

  const go = (path) => {
    navigate(path);
    setMenuOpen(false);
    setProfileMenuOpen(false);
  };

  const handleLogout = async () => {
    await logoutUser();
    toast.success(t("common.loggedOut"));
    navigate("/", { replace: true });
  };

  const menuItems = [
    {
      label: t("common.dashboard"),
      activeKey: "Dashboard",
      icon: <LayoutDashboard size={20} />,
      path: "/passenger/dashboard",
    },
    {
      label: t("passenger.layout.findLocalRoute"),
      activeKey: "Find Local Route",
      icon: <Route size={20} />,
      path: "/passenger/local-routes",
    },
    {
      label: t("common.wallet"),
      activeKey: "Wallet",
      icon: <Wallet size={20} />,
      path: "/wallet",
    },
    {
      label: t("passenger.layout.bookOutOfValley"),
      activeKey: "Book Out-of-Valley",
      icon: <Bus size={20} />,
      path: "/passenger/out-of-valley",
    },
    {
      label: t("passenger.layout.myBookings"),
      activeKey: "My Bookings",
      icon: <Ticket size={20} />,
      path: "/passenger/bookings",
    },
    {
      label: t("common.settings"),
      activeKey: "Settings",
      icon: <Settings size={20} />,
      path: "/settings",
    },
  ];

  return (
    <div className="min-h-screen bg-[#f3f6fa] text-[#08264a]">
      <button
        type="button"
        onClick={() => setMenuOpen(true)}
        aria-label={t("passenger.layout.openMenu")}
        className="tap-target fixed left-4 top-4 z-50 rounded-xl bg-white p-3 shadow-md lg:hidden"
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
        className={`fixed left-0 top-0 z-50 flex h-dvh w-[min(18rem,calc(100vw-2rem))] flex-col justify-between overflow-y-auto border-r border-slate-200 bg-white p-4 transition-transform duration-300 lg:h-screen lg:w-64 lg:translate-x-0 ${
          menuOpen ? "translate-x-0" : "-translate-x-full"
        }`}
      >
        <div>
          <div className="flex items-start justify-between">
            <button type="button" onClick={() => go("/passenger/dashboard")} aria-label="Yatayat passenger dashboard" className="rounded-lg text-left focus-visible:outline-2 focus-visible:outline-offset-4 focus-visible:outline-[#08264a]">
              <YatayatLogo variant="full" size="md" />
            </button>

            <button type="button" aria-label={t("passenger.layout.closeMenu")} onClick={() => setMenuOpen(false)} className="rounded-lg p-2 lg:hidden">
              <X size={22} />
            </button>
          </div>

          <nav className="mt-8 space-y-2">
            {menuItems.map((item) => (
              <button
                type="button"
                key={item.activeKey}
                onClick={() => go(item.path)}
                className={`flex w-full items-center gap-3 rounded-lg px-4 py-3 text-sm transition ${
                  activePage === item.activeKey
                    ? "bg-[#1d3f6e] font-semibold text-white"
                    : "font-medium text-slate-600 hover:bg-slate-100 hover:text-[#08264a]"
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

            <div className="min-w-0">
              <p className="text-sm font-bold">{fullName}</p>
              <p className="text-[10px] uppercase text-slate-500">
                {t(`common.${String(role).toLowerCase()}`)}
              </p>
            </div>
          </button>

          <button
            onClick={handleLogout}
            className="mt-4 flex w-full items-center justify-center gap-2 rounded-xl border border-red-200 bg-red-50 py-3 text-sm font-black text-red-600 transition hover:bg-red-100"
            title={t("common.logout")}
          >
            <LogOut size={18} />
            {t("common.logout")}
          </button>
        </div>
      </aside>

      <main className="min-h-screen px-4 py-5 pt-20 sm:px-6 lg:ml-64 lg:px-7 lg:py-6 lg:pt-6">
        <header className="mb-6 flex min-h-20 flex-col gap-4 border-b border-slate-200 pb-5 sm:flex-row sm:items-center sm:justify-between">
          <div className="min-w-0">
            <h1 className="safe-wrap text-2xl font-black tracking-tight text-slate-900 sm:text-3xl">
              {pageHeader.title}
            </h1>
            <p className="mt-1 text-sm text-slate-500">{pageHeader.subtitle}</p>
          </div>

          <div className="flex shrink-0 items-center justify-end gap-3">
            <LanguageToggle
              language={language}
              setLanguage={setLanguage}
              label={t("common.changeLanguage")}
            />

            <button
              type="button"
              onClick={() => go("/notifications")}
              aria-label={t("common.openNotifications")}
              className="relative flex h-10 w-10 items-center justify-center rounded-full bg-white text-[#08264a] shadow-sm transition hover:bg-slate-50"
            >
              <Bell size={20} />
              {unreadCount > 0 && (
                <span className="absolute -right-1 -top-1 flex min-h-5 min-w-5 items-center justify-center rounded-full bg-red-600 px-1 text-[10px] font-black text-white">
                  {unreadCount > 99 ? "99+" : unreadCount}
                </span>
              )}
            </button>

            <div className="relative">
              <button
                type="button"
                onClick={() => setProfileMenuOpen((open) => !open)}
                aria-label={t("common.openProfile")}
                aria-expanded={profileMenuOpen}
                title={fullName}
                className="flex h-10 items-center gap-2 rounded-xl bg-emerald-100 px-2.5 text-emerald-700 transition hover:bg-emerald-200"
              >
                <UserCircle size={22} />
                <ChevronDown size={15} />
              </button>

              {profileMenuOpen && (
                <div className="absolute right-0 top-12 z-30 w-56 rounded-2xl border border-slate-200 bg-white p-2 shadow-xl">
                  <div className="border-b border-slate-100 px-3 py-2">
                    <p className="truncate text-sm font-black text-slate-900">{fullName}</p>
                    <p className="text-xs text-slate-500">{t(`common.${String(role).toLowerCase()}`)}</p>
                  </div>
                  <button
                    type="button"
                    onClick={() => go("/profile")}
                    className="mt-1 flex w-full items-center gap-2 rounded-xl px-3 py-2 text-left text-sm font-bold text-slate-700 hover:bg-slate-100"
                  >
                    <UserCircle size={17} />
                    {t("common.profile")}
                  </button>
                  <button
                    type="button"
                    onClick={handleLogout}
                    className="flex w-full items-center gap-2 rounded-xl px-3 py-2 text-left text-sm font-bold text-red-600 hover:bg-red-50"
                  >
                    <LogOut size={17} />
                    {t("common.logout")}
                  </button>
                </div>
              )}
            </div>
          </div>
        </header>

        {children}
      </main>
    </div>
  );
}

function getPageHeader(pathname, activePage, firstName, language) {
  const nepali = language === "ne";
  const page = resolvePage(pathname, activePage);
  const copy = {
    dashboard: {
      title: nepali ? `फेरि स्वागत छ, ${firstName} 👋` : `Welcome back, ${firstName} 👋`,
      subtitle: nepali ? "आफ्नो यात्रा योजना र व्यवस्थापन गर्नुहोस्।" : "Plan and manage your journeys.",
    },
    wallet: {
      title: nepali ? "वालेट" : "Wallet",
      subtitle: nepali ? "आफ्नो मौज्दात र कारोबार व्यवस्थापन गर्नुहोस्।" : "Manage your balance and transactions.",
    },
    routes: {
      title: nepali ? "रुटहरू" : "Routes",
      subtitle: nepali ? "उपलब्ध रुटहरू हेर्नुहोस्।" : "Explore available routes.",
    },
    tracking: {
      title: nepali ? "बस ट्र्याक गर्नुहोस्" : "Track Bus",
      subtitle: nepali ? "बसहरू वास्तविक समयमा ट्र्याक गर्नुहोस्।" : "Track buses in real time.",
    },
    notifications: {
      title: nepali ? "सूचनाहरू" : "Notifications",
      subtitle: nepali ? "हालका गतिविधिबारे जानकारी लिनुहोस्।" : "Stay updated with recent activity.",
    },
    profile: {
      title: nepali ? "प्रोफाइल" : "Profile",
      subtitle: nepali ? "आफ्नो खाता व्यवस्थापन गर्नुहोस्।" : "Manage your account.",
    },
    bookings: {
      title: nepali ? "मेरा बुकिङहरू" : "My Bookings",
      subtitle: nepali ? "आफ्ना बुकिङ र टिकटहरू हेर्नुहोस्।" : "Review your bookings and tickets.",
    },
    booking: {
      title: nepali ? "टिकट बुक गर्नुहोस्" : "Book Tickets",
      subtitle: nepali ? "आफ्नो अर्को यात्रा खोज्नुहोस् र बुक गर्नुहोस्।" : "Find and book your next journey.",
    },
    farePass: {
      title: nepali ? "भाडा पास" : "FarePass / QR",
      subtitle: nepali ? "आफ्नो QR भाडा पास व्यवस्थापन गर्नुहोस्।" : "Manage your QR fare pass.",
    },
    history: {
      title: nepali ? "इतिहास" : "History",
      subtitle: nepali ? "आफ्नो अघिल्लो यात्रा गतिविधि हेर्नुहोस्।" : "Review your previous travel activity.",
    },
    settings: {
      title: nepali ? "सेटिङहरू" : "Settings",
      subtitle: nepali ? "आफ्नो प्राथमिकताहरू व्यवस्थापन गर्नुहोस्।" : "Manage your preferences.",
    },
  };
  return copy[page] || copy.dashboard;
}

function resolvePage(pathname, activePage) {
  if (pathname === "/passenger/dashboard") return "dashboard";
  if (pathname.startsWith("/wallet")) return "wallet";
  if (pathname === "/notifications") return "notifications";
  if (pathname === "/profile") return "profile";
  if (pathname.startsWith("/track-bus")) return "tracking";
  if (pathname.startsWith("/passenger/local-routes") || pathname === "/routes") return "routes";
  if (pathname === "/fare-pass") return "farePass";
  if (pathname === "/settings") return "settings";
  if (
    pathname.startsWith("/passenger/bookings")
    || pathname.startsWith("/passenger/tickets")
    || pathname.startsWith("/passenger/payments")
    || pathname === "/my-bookings"
    || pathname === "/ticket"
  ) return "bookings";
  if (
    pathname.startsWith("/passenger/trips")
    || pathname === "/passenger/out-of-valley"
    || pathname === "/book-ticket"
    || pathname === "/seat-selection"
    || pathname === "/booking-summary"
    || pathname === "/payment"
  ) return "booking";
  if (activePage === "Wallet") return "wallet";
  if (activePage === "Settings") return "settings";
  return "dashboard";
}

function LanguageToggle({ language, setLanguage, label }) {
  return (
    <div
      role="group"
      aria-label={label}
      className="flex rounded-full bg-slate-200 p-1 text-xs font-bold"
    >
      <button
        type="button"
        aria-pressed={language === "en"}
        onClick={() => setLanguage("en")}
        className={`rounded-full px-3 py-1 transition focus:outline-none focus:ring-2 focus:ring-[#08264a] focus:ring-offset-2 ${
          language === "en"
            ? "bg-[#08264a] text-white"
            : "text-slate-600 hover:text-[#08264a]"
        }`}
      >
        EN
      </button>
      <button
        type="button"
        aria-pressed={language === "ne"}
        onClick={() => setLanguage("ne")}
        className={`rounded-full px-3 py-1 transition focus:outline-none focus:ring-2 focus:ring-[#08264a] focus:ring-offset-2 ${
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
