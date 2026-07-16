import { Building2, Bus, LayoutDashboard, LogOut, Menu, Users, X } from "lucide-react";
import { useState } from "react";
import { useLocation, useNavigate } from "react-router-dom";
import { logoutUser } from "../../services/authService";

export default function OperatorLayout({ children }) {
  const navigate = useNavigate();
  const location = useLocation();
  const [sidebarOpen, setSidebarOpen] = useState(false);

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

      <aside className={`fixed left-0 top-0 z-50 flex h-screen w-72 flex-col border-r border-slate-200 bg-white transition-transform lg:translate-x-0 ${sidebarOpen ? "translate-x-0" : "-translate-x-full"}`}>
        <div className="flex items-center justify-between border-b border-slate-200 px-6 py-5">
          <div>
            <h1 className="text-2xl font-black">Yatayat Operator</h1>
            <p className="mt-1 text-xs font-semibold text-slate-500">Operations Portal</p>
          </div>
          <button type="button" onClick={() => setSidebarOpen(false)} className="p-2 lg:hidden">
            <X size={20} />
          </button>
        </div>

        <nav className="flex-1 space-y-2 p-4">
          <NavItem icon={<LayoutDashboard size={19} />} label="Dashboard" path="/operator/dashboard" active={location.pathname === "/operator/dashboard"} navigate={navigate} />
          <NavItem icon={<Bus size={19} />} label="Buses" path="/operator/buses" active={location.pathname.startsWith("/operator/buses")} navigate={navigate} />
          <NavItem icon={<Users size={19} />} label="Drivers" path="/operator/drivers" active={location.pathname.startsWith("/operator/drivers")} navigate={navigate} />
        </nav>

        <div className="border-t border-slate-200 p-5">
          <button type="button" onClick={logout} className="flex w-full items-center justify-center gap-2 rounded-xl border border-red-200 bg-red-50 py-3 text-sm font-black text-red-600">
            <LogOut size={18} /> Logout
          </button>
        </div>
      </aside>

      <div className="min-h-screen lg:ml-72">
        <header className="flex items-center gap-3 border-b border-slate-200 bg-white px-4 py-4 sm:px-6 lg:px-8">
          <button type="button" onClick={() => setSidebarOpen(true)} className="rounded-xl border border-slate-200 p-3 lg:hidden">
            <Menu size={20} />
          </button>
          <Building2 size={23} />
          <h2 className="text-xl font-black">Operator Dashboard</h2>
        </header>
        <main className="px-4 py-6 sm:px-6 lg:px-8">{children}</main>
      </div>
    </div>
  );
}

function NavItem({ icon, label, path, active, navigate }) {
  return (
    <button type="button" onClick={() => navigate(path)} className={`flex w-full items-center gap-3 rounded-xl px-4 py-3 text-sm font-black ${active ? "bg-[#08264a] text-white" : "text-slate-600 hover:bg-slate-100"}`}>
      {icon} {label}
    </button>
  );
}
