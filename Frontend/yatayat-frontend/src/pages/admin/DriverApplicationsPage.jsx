import { useEffect, useMemo, useState } from "react";
import {
  AlertTriangle,
  ArrowRight,
  CalendarDays,
  CheckCircle2,
  Clock3,
  FileCheck2,
  Loader2,
  Mail,
  Phone,
  RefreshCw,
  Search,
  UserCheck,
} from "lucide-react";
import { useNavigate } from "react-router-dom";
import AdminLayout from "../../components/layout/AdminLayout";

const API_BASE_URL = "http://localhost:8080";

export default function DriverApplicationsPage() {
  const navigate = useNavigate();

  const [applications, setApplications] = useState([]);
  const [searchText, setSearchText] = useState("");
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [error, setError] = useState("");

  const fetchApplications = async (manualRefresh = false) => {
    try {
      if (manualRefresh) {
        setRefreshing(true);
      } else {
        setLoading(true);
      }

      setError("");

      const response = await fetch(
        `${API_BASE_URL}/api/admin/drivers/pending`
      );

      if (!response.ok) {
        let message = "Unable to load driver applications.";

        try {
          const errorData = await response.json();
          message = errorData.message || message;
        } catch {
          // Keep the default message.
        }

        throw new Error(message);
      }

      const data = await response.json();

      setApplications(Array.isArray(data) ? data : []);
    } catch (fetchError) {
      console.error("Driver applications error:", fetchError);

      setError(
        fetchError.message ||
          "Unable to connect to the backend. Make sure Spring Boot is running."
      );

      setApplications([]);
    } finally {
      setLoading(false);
      setRefreshing(false);
    }
  };

  useEffect(() => {
    fetchApplications();
  }, []);

  const filteredApplications = useMemo(() => {
    const query = searchText.trim().toLowerCase();

    if (!query) {
      return applications;
    }

    return applications.filter((application) => {
      const searchableValues = [
        application.fullName,
        application.email,
        application.phone,
        application.licenseNumber,
        application.licenseCategory,
        application.preferredOperatingArea,
      ];

      return searchableValues.some((value) =>
        String(value || "")
          .toLowerCase()
          .includes(query)
      );
    });
  }, [applications, searchText]);

  const openApplication = (applicationId) => {
    navigate(`/admin/driver-applications/${applicationId}`);
  };

  return (
    <AdminLayout
      title="Driver Applications"
      subtitle="Review pending driver identities, licences, experience and submitted documents."
    >
      <div className="space-y-6">
        {/* SUMMARY HEADER */}

        <section className="overflow-hidden rounded-3xl bg-[#08264a] px-6 py-7 text-white shadow-sm sm:px-8">
          <div className="flex flex-col gap-6 lg:flex-row lg:items-center lg:justify-between">
            <div>
              <p className="text-xs font-black uppercase tracking-[0.22em] text-blue-200">
                Driver Verification Centre
              </p>

              <h1 className="mt-3 text-2xl font-black sm:text-3xl">
                Pending driver applications
              </h1>

              <p className="mt-3 max-w-2xl text-sm leading-6 text-slate-300">
                Check applicant identity, driving licence validity,
                experience and uploaded documents before approving access to
                driver operations.
              </p>
            </div>

            <div className="flex flex-col gap-3 sm:flex-row">
              <div className="rounded-2xl bg-white/10 px-5 py-4 backdrop-blur">
                <p className="text-xs font-black uppercase tracking-widest text-blue-200">
                  Awaiting Review
                </p>

                <p className="mt-2 text-3xl font-black">
                  {loading ? "—" : applications.length}
                </p>
              </div>

              <button
                type="button"
                onClick={() => fetchApplications(true)}
                disabled={refreshing}
                className="flex items-center justify-center gap-2 rounded-2xl bg-white px-5 py-3 text-sm font-black text-[#08264a] transition hover:bg-slate-100 disabled:cursor-not-allowed disabled:opacity-70"
              >
                <RefreshCw
                  size={18}
                  className={refreshing ? "animate-spin" : ""}
                />

                {refreshing ? "Refreshing..." : "Refresh"}
              </button>
            </div>
          </div>
        </section>

        {/* SEARCH AND INFORMATION */}

        <section className="grid grid-cols-1 gap-4 lg:grid-cols-12">
          <div className="rounded-2xl border border-slate-200 bg-white p-4 shadow-sm lg:col-span-8">
            <label className="text-xs font-black uppercase tracking-widest text-slate-500">
              Search applications
            </label>

            <div className="mt-2 flex items-center gap-3 rounded-xl border border-slate-300 bg-slate-50 px-4 py-3 focus-within:border-[#08264a] focus-within:bg-white">
              <Search size={19} className="shrink-0 text-slate-400" />

              <input
                type="search"
                value={searchText}
                onChange={(event) => setSearchText(event.target.value)}
                placeholder="Search name, email, phone, licence or area..."
                className="w-full bg-transparent text-sm font-semibold text-slate-900 outline-none placeholder:font-normal placeholder:text-slate-400"
              />

              {searchText && (
                <button
                  type="button"
                  onClick={() => setSearchText("")}
                  className="text-xs font-black text-[#08264a] hover:underline"
                >
                  Clear
                </button>
              )}
            </div>
          </div>

          <div className="flex items-center gap-4 rounded-2xl border border-amber-200 bg-amber-50 p-4 lg:col-span-4">
            <div className="flex h-11 w-11 shrink-0 items-center justify-center rounded-xl bg-amber-100 text-amber-700">
              <Clock3 size={22} />
            </div>

            <div>
              <p className="font-black text-amber-900">
                Manual verification required
              </p>

              <p className="mt-1 text-sm leading-5 text-amber-700">
                Approve only after reviewing all submitted information.
              </p>
            </div>
          </div>
        </section>

        {/* ERROR */}

        {error && (
          <section className="flex flex-col gap-4 rounded-2xl border border-red-200 bg-red-50 p-5 sm:flex-row sm:items-center sm:justify-between">
            <div className="flex items-start gap-3">
              <AlertTriangle
                size={21}
                className="mt-0.5 shrink-0 text-red-600"
              />

              <div>
                <p className="font-black text-red-800">
                  Applications could not be loaded
                </p>

                <p className="mt-1 text-sm text-red-700">
                  {error}
                </p>
              </div>
            </div>

            <button
              type="button"
              onClick={() => fetchApplications(true)}
              className="flex items-center justify-center gap-2 rounded-xl bg-red-600 px-5 py-3 text-sm font-black text-white transition hover:bg-red-700"
            >
              <RefreshCw size={17} />
              Try Again
            </button>
          </section>
        )}

        {/* CONTENT */}

        <section className="overflow-hidden rounded-3xl border border-slate-200 bg-white shadow-sm">
          <div className="flex flex-col gap-3 border-b border-slate-200 px-5 py-5 sm:flex-row sm:items-center sm:justify-between sm:px-6">
            <div>
              <h2 className="text-xl font-black text-slate-900">
                Applications awaiting review
              </h2>

              <p className="mt-1 text-sm text-slate-500">
                Showing {filteredApplications.length} of{" "}
                {applications.length} applications.
              </p>
            </div>

            <span className="w-fit rounded-full bg-amber-100 px-4 py-2 text-xs font-black text-amber-700">
              PENDING
            </span>
          </div>

          {loading ? (
            <LoadingApplications />
          ) : filteredApplications.length === 0 ? (
            <EmptyApplications hasSearch={Boolean(searchText.trim())} />
          ) : (
            <>
              {/* DESKTOP TABLE */}

              <div className="hidden overflow-x-auto lg:block">
                <table className="w-full min-w-237.5 text-left">
                  <thead className="bg-slate-50">
                    <tr className="text-xs font-black uppercase tracking-wider text-slate-500">
                      <th className="px-6 py-4">Applicant</th>
                      <th className="px-6 py-4">Contact</th>
                      <th className="px-6 py-4">Licence</th>
                      <th className="px-6 py-4">Experience</th>
                      <th className="px-6 py-4">Submitted</th>
                      <th className="px-6 py-4">Status</th>
                      <th className="px-6 py-4 text-right">Action</th>
                    </tr>
                  </thead>

                  <tbody className="divide-y divide-slate-100">
                    {filteredApplications.map((application) => (
                      <ApplicationTableRow
                        key={application.id}
                        application={application}
                        onReview={() =>
                          openApplication(application.id)
                        }
                      />
                    ))}
                  </tbody>
                </table>
              </div>

              {/* MOBILE AND TABLET CARDS */}

              <div className="grid grid-cols-1 gap-4 p-4 sm:grid-cols-2 lg:hidden">
                {filteredApplications.map((application) => (
                  <ApplicationCard
                    key={application.id}
                    application={application}
                    onReview={() =>
                      openApplication(application.id)
                    }
                  />
                ))}
              </div>
            </>
          )}
        </section>
      </div>
    </AdminLayout>
  );
}

function ApplicationTableRow({ application, onReview }) {
  return (
    <tr className="transition hover:bg-slate-50">
      <td className="px-6 py-5">
        <div className="flex items-center gap-3">
          <Avatar name={application.fullName} />

          <div className="min-w-0">
            <p className="max-w-47.5 truncate font-black text-slate-900">
              {application.fullName || "Driver Applicant"}
            </p>

            <p className="mt-1 text-xs font-semibold text-slate-500">
              Application DRV-{application.id}
            </p>
          </div>
        </div>
      </td>

      <td className="px-6 py-5">
        <div className="space-y-1">
          <p className="flex items-center gap-2 text-sm text-slate-700">
            <Mail size={14} className="shrink-0 text-slate-400" />
            <span className="max-w-45 truncate">
              {application.email || "N/A"}
            </span>
          </p>

          <p className="flex items-center gap-2 text-sm text-slate-500">
            <Phone size={14} className="shrink-0 text-slate-400" />
            {application.phone || "N/A"}
          </p>
        </div>
      </td>

      <td className="px-6 py-5">
        <p className="font-black text-slate-900">
          {application.licenseNumber || "N/A"}
        </p>

        <p className="mt-1 text-xs text-slate-500">
          Category {application.licenseCategory || "N/A"}
        </p>
      </td>

      <td className="px-6 py-5">
        <p className="font-black text-slate-900">
          {application.yearsOfExperience ?? 0} year(s)
        </p>

        <p className="mt-1 max-w-37.5 truncate text-xs text-slate-500">
          {application.preferredOperatingArea || "Area not specified"}
        </p>
      </td>

      <td className="px-6 py-5">
        <p className="flex items-center gap-2 text-sm font-bold text-slate-700">
          <CalendarDays size={15} className="text-slate-400" />
          {formatDate(application.submittedAt)}
        </p>

        <p className="mt-1 text-xs text-slate-500">
          {formatTime(application.submittedAt)}
        </p>
      </td>

      <td className="px-6 py-5">
        <StatusBadge />
      </td>

      <td className="px-6 py-5 text-right">
        <button
          type="button"
          onClick={onReview}
          className="inline-flex items-center justify-center gap-2 rounded-xl bg-[#08264a] px-4 py-2.5 text-sm font-black text-white transition hover:bg-[#0d3566]"
        >
          Review
          <ArrowRight size={16} />
        </button>
      </td>
    </tr>
  );
}

function ApplicationCard({ application, onReview }) {
  return (
    <article className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">
      <div className="flex items-start justify-between gap-3">
        <div className="flex min-w-0 items-center gap-3">
          <Avatar name={application.fullName} />

          <div className="min-w-0">
            <h3 className="truncate font-black text-slate-900">
              {application.fullName || "Driver Applicant"}
            </h3>

            <p className="mt-1 text-xs font-semibold text-slate-500">
              DRV-{application.id}
            </p>
          </div>
        </div>

        <StatusBadge />
      </div>

      <div className="mt-5 space-y-3">
        <MobileInfo
          icon={<Mail size={16} />}
          label="Email"
          value={application.email}
        />

        <MobileInfo
          icon={<Phone size={16} />}
          label="Phone"
          value={application.phone}
        />

        <MobileInfo
          icon={<FileCheck2 size={16} />}
          label="Licence"
          value={`${application.licenseNumber || "N/A"} · Category ${
            application.licenseCategory || "N/A"
          }`}
        />

        <MobileInfo
          icon={<UserCheck size={16} />}
          label="Experience"
          value={`${application.yearsOfExperience ?? 0} year(s)`}
        />

        <MobileInfo
          icon={<CalendarDays size={16} />}
          label="Submitted"
          value={formatDateTime(application.submittedAt)}
        />
      </div>

      <button
        type="button"
        onClick={onReview}
        className="mt-5 flex w-full items-center justify-center gap-2 rounded-xl bg-[#08264a] py-3 text-sm font-black text-white transition hover:bg-[#0d3566]"
      >
        Review Application
        <ArrowRight size={17} />
      </button>
    </article>
  );
}

function MobileInfo({ icon, label, value }) {
  return (
    <div className="flex items-start gap-3 rounded-xl bg-slate-50 p-3">
      <div className="mt-0.5 text-slate-400">{icon}</div>

      <div className="min-w-0">
        <p className="text-[10px] font-black uppercase tracking-widest text-slate-500">
          {label}
        </p>

        <p className="mt-1 wrap-break-word text-sm font-bold text-slate-800">
          {value || "N/A"}
        </p>
      </div>
    </div>
  );
}

function StatusBadge() {
  return (
    <span className="inline-flex items-center gap-1.5 rounded-full bg-amber-100 px-3 py-1.5 text-[10px] font-black uppercase tracking-wide text-amber-700">
      <Clock3 size={13} />
      Pending
    </span>
  );
}

function Avatar({ name }) {
  return (
    <div className="flex h-11 w-11 shrink-0 items-center justify-center rounded-2xl bg-blue-100 text-sm font-black text-[#08264a]">
      {getInitials(name || "Driver")}
    </div>
  );
}

function LoadingApplications() {
  return (
    <div className="p-5 sm:p-6">
      <div className="flex items-center justify-center py-8">
        <div className="text-center">
          <Loader2
            size={36}
            className="mx-auto animate-spin text-[#08264a]"
          />

          <p className="mt-3 text-sm font-black text-slate-700">
            Loading driver applications...
          </p>
        </div>
      </div>

      <div className="mt-2 space-y-3">
        {[1, 2, 3].map((item) => (
          <div
            key={item}
            className="flex animate-pulse items-center gap-4 rounded-2xl border border-slate-100 p-4"
          >
            <div className="h-11 w-11 rounded-2xl bg-slate-200" />

            <div className="flex-1">
              <div className="h-4 w-40 rounded bg-slate-200" />
              <div className="mt-2 h-3 w-56 max-w-full rounded bg-slate-100" />
            </div>

            <div className="hidden h-10 w-24 rounded-xl bg-slate-200 sm:block" />
          </div>
        ))}
      </div>
    </div>
  );
}

function EmptyApplications({ hasSearch }) {
  return (
    <div className="px-6 py-14 text-center">
      <div className="mx-auto flex h-16 w-16 items-center justify-center rounded-2xl bg-emerald-100 text-emerald-700">
        <CheckCircle2 size={31} />
      </div>

      <h3 className="mt-4 text-xl font-black text-slate-900">
        {hasSearch
          ? "No matching applications"
          : "No pending applications"}
      </h3>

      <p className="mx-auto mt-2 max-w-md text-sm leading-6 text-slate-500">
        {hasSearch
          ? "Try searching with another name, email, phone number or licence number."
          : "All submitted driver applications have already been reviewed."}
      </p>
    </div>
  );
}

function getInitials(name) {
  return String(name)
    .split(" ")
    .filter(Boolean)
    .slice(0, 2)
    .map((part) => part[0]?.toUpperCase())
    .join("");
}

function formatDate(dateValue) {
  if (!dateValue) return "Not available";

  const date = new Date(dateValue);

  if (Number.isNaN(date.getTime())) {
    return dateValue;
  }

  return date.toLocaleDateString("en-US", {
    year: "numeric",
    month: "short",
    day: "numeric",
  });
}

function formatTime(dateValue) {
  if (!dateValue) return "";

  const date = new Date(dateValue);

  if (Number.isNaN(date.getTime())) {
    return "";
  }

  return date.toLocaleTimeString("en-US", {
    hour: "2-digit",
    minute: "2-digit",
  });
}

function formatDateTime(dateValue) {
  const date = formatDate(dateValue);
  const time = formatTime(dateValue);

  return time ? `${date} · ${time}` : date;
}