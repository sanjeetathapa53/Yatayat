import { Link } from "react-router-dom";
import YatayatLogo from "../branding/YatayatLogo";

export default function AuthLayout({ children }) {
  return (
    <div className="min-h-screen bg-linear-to-br from-slate-50 via-slate-100 to-emerald-50">
      <header className="h-14 bg-white/90 border-b border-slate-200">
        <div className="mx-auto flex h-full max-w-7xl items-center justify-between px-6">
          <Link to="/" aria-label="Yatayat home" className="rounded-lg focus-visible:outline-2 focus-visible:outline-offset-4 focus-visible:outline-[#08264a]">
            <YatayatLogo variant="compact" size="sm" />
          </Link>

          <div className="hidden gap-6 text-sm font-medium text-slate-600 sm:flex">
            <Link to="/routes">Route Directory</Link>
            <a href="#">Help Center</a>
          </div>
        </div>
      </header>

      <main className="flex min-h-[calc(100vh-104px)] items-center justify-center px-5 py-4">
        {children}
      </main>

      <footer className="h-12 bg-[#04294f] px-6 text-slate-200">
        <div className="mx-auto flex h-full max-w-7xl items-center justify-between text-xs">
          <p>© 2026 Yatayat Nepal Transit Authority.</p>
          <div className="hidden gap-5 sm:flex">
            <span>Privacy</span>
            <span>Terms</span>
            <span>Contact</span>
          </div>
        </div>
      </footer>
    </div>
  );
}
