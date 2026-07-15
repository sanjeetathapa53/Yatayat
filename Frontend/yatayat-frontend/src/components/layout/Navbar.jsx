import { NavLink } from "react-router-dom";
import { Bell, UserCircle } from "lucide-react";
import { useLanguage } from "../../context/LanguageContext";

export default function Navbar() {
  const { language, setLanguage, t } = useLanguage();

  const isLoggedIn = false;

  const navItems = [
    { path: "/routes", label: t.navRoutes || "Routes" },
    { path: "/track-bus", label: t.navTrackBus || "Track Bus" },
    { path: "/fare-pass", label: "Fare Pass" },
    { path: "/wallet", label: "Wallet" },
  ];

  return (
    <header className="fixed left-0 top-0 z-50 w-full border-b border-slate-200 bg-white">
      <nav className="mx-auto flex h-14 max-w-7xl items-center justify-between px-6">
        <div className="flex items-center gap-8">
          <NavLink to="/" className="text-xl font-black text-[#08264a]">
            Yatayat
          </NavLink>

          <div className="hidden items-center gap-6 md:flex">
            {navItems.map((item) => (
              <NavLink
                key={item.path}
                to={item.path}
                className={({ isActive }) =>
                  `relative text-sm font-medium transition ${
                    isActive
                      ? "text-[#08264a]"
                      : "text-slate-600 hover:text-[#08264a]"
                  }`
                }
              >
                {({ isActive }) => (
                  <>
                    {item.label}

                    {isActive && (
                      <span className="absolute -bottom-4 left-0 h-0.75 w-full rounded-full bg-[#08264a]" />
                    )}
                  </>
                )}
              </NavLink>
            ))}
          </div>
        </div>

        <div className="flex items-center gap-5">
          <div className="flex rounded-full border border-slate-300 bg-slate-100 p-1 text-xs font-bold">
            <button
              type="button"
              onClick={() => setLanguage("en")}
              className={`rounded-full px-3 py-1 transition ${
                language === "en"
                  ? "bg-[#08264a] text-white"
                  : "text-slate-500"
              }`}
            >
              EN
            </button>

            <button
              type="button"
              onClick={() => setLanguage("np")}
              className={`rounded-full px-3 py-1 transition ${
                language === "np"
                  ? "bg-[#08264a] text-white"
                  : "text-slate-500"
              }`}
            >
              NE
            </button>
          </div>

          <NavLink
            to="/login"
            className="text-sm font-medium text-slate-700 hover:text-[#08264a]"
          >
            {t.login || "Login"}
          </NavLink>

          <NavLink
            to="/register"
            className="rounded-md bg-[#08264a] px-4 py-2 text-sm font-bold text-white hover:bg-[#0d3566]"
          >
            {t.register || "Register"}
          </NavLink>

          <NavLink
            to="/notifications"
            className={({ isActive }) =>
              `rounded-full p-1.5 transition ${
                isActive
                  ? "bg-[#08264a] text-white"
                  : "text-slate-800 hover:bg-slate-100"
              }`
            }
          >
            <Bell size={20} />
          </NavLink>

          <NavLink
            to={isLoggedIn ? "/profile" : "/login"}
            className={({ isActive }) =>
              `rounded-full p-1.5 transition ${
                isActive
                  ? "bg-[#08264a] text-white"
                  : "text-slate-800 hover:bg-slate-100"
              }`
            }
          >
            <UserCircle size={21} />
          </NavLink>
        </div>
      </nav>
    </header>
  );
}