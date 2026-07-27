import { useCallback, useEffect, useMemo, useState } from "react";
import {
  AlertTriangle,
  Building2,
  CheckCircle2,
  Clock3,
  Eye,
  FileCheck2,
  Loader2,
  Mail,
  MapPin,
  Phone,
  RefreshCw,
  Search,
  ShieldAlert,
  X,
  XCircle,
} from "lucide-react";
import { toast } from "react-toastify";

import AdminLayout from "../../components/layout/AdminLayout";
import { API_BASE_URL } from "../../utils/api";

export default function TransportOperatorsPage() {
  const [operators, setOperators] = useState([]);
  const [searchText, setSearchText] = useState("");
  const [statusFilter, setStatusFilter] =
    useState("ALL");

  const [selectedOperator, setSelectedOperator] =
    useState(null);

  const [showRejectModal, setShowRejectModal] =
    useState(false);

  const [rejectionReason, setRejectionReason] =
    useState("");

  const [loading, setLoading] = useState(true);
  const [actionLoading, setActionLoading] =
    useState(false);

  const [error, setError] = useState("");

  const loadOperators = useCallback(async () => {
    try {
      setLoading(true);
      setError("");

      const response = await fetch(
        `${API_BASE_URL}/api/admin/operators`,
        { credentials: "include" }
      );

      if (!response.ok) {
        const errorData = await response.json().catch(() => ({}));
        throw new Error(
          errorData.message ||
            "Unable to load transport operators."
        );
      }

      const data = await response.json();

      setOperators(Array.isArray(data) ? data : []);
    } catch (loadError) {
      console.error(loadError);

      setError(
        loadError.message ||
          "Unable to load transport operators."
      );
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    const timer = window.setTimeout(() => {
      void loadOperators();
    }, 0);
    return () => window.clearTimeout(timer);
  }, [loadOperators]);

  const filteredOperators = useMemo(() => {
    const query = searchText.trim().toLowerCase();

    return operators.filter((operator) => {
      const matchesSearch =
        !query ||
        [
          operator.name,
          operator.registrationNumber,
          operator.email,
          operator.phone,
          operator.contactPerson,
          operator.address,
        ].some((value) =>
          String(value || "")
            .toLowerCase()
            .includes(query)
        );

      const matchesStatus =
        statusFilter === "ALL" ||
        operator.verificationStatus ===
          statusFilter;

      return matchesSearch && matchesStatus;
    });
  }, [operators, searchText, statusFilter]);

  const counts = useMemo(() => {
    return {
      total: operators.length,
      pending: operators.filter(
        (item) =>
          item.verificationStatus === "PENDING"
      ).length,
      approved: operators.filter(
        (item) =>
          item.verificationStatus === "APPROVED"
      ).length,
      rejected: operators.filter(
        (item) =>
          item.verificationStatus === "REJECTED"
      ).length,
    };
  }, [operators]);

  const approveOperator = async (operatorId) => {
    try {
      setActionLoading(true);

      const response = await fetch(
        `${API_BASE_URL}/api/admin/operators/${operatorId}/approve`,
        {
          method: "PUT",
          credentials: "include",
        }
      );

      const data = await response.json();

      if (!response.ok || !data.success) {
        throw new Error(
          data.message ||
            "Unable to approve operator."
        );
      }

      toast.success(
        "Transport operator approved successfully."
      );

      setSelectedOperator(null);
      await loadOperators();
    } catch (approveError) {
      toast.error(
        approveError.message ||
          "Unable to approve operator."
      );
    } finally {
      setActionLoading(false);
    }
  };

  const rejectOperator = async () => {
    if (!selectedOperator?.id) {
      return;
    }

    if (!rejectionReason.trim()) {
      toast.error(
        "Please enter a rejection reason."
      );
      return;
    }

    try {
      setActionLoading(true);

      const response = await fetch(
        `${API_BASE_URL}/api/admin/operators/${selectedOperator.id}/reject`,
        {
          method: "PUT",
          credentials: "include",
          headers: {
            "Content-Type": "application/json",
          },
          body: JSON.stringify({
            reason: rejectionReason.trim(),
          }),
        }
      );

      const data = await response.json();

      if (!response.ok || !data.success) {
        throw new Error(
          data.message ||
            "Unable to reject operator."
        );
      }

      toast.success(
        "Operator application rejected."
      );

      setShowRejectModal(false);
      setSelectedOperator(null);
      setRejectionReason("");

      await loadOperators();
    } catch (rejectError) {
      toast.error(
        rejectError.message ||
          "Unable to reject operator."
      );
    } finally {
      setActionLoading(false);
    }
  };

  return (
    <AdminLayout
      title="Transport Operators"
      subtitle="Review and manage public transport companies, cooperatives and independent operators."
    >
      <div className="space-y-6">
        <section className="grid grid-cols-2 gap-4 xl:grid-cols-4">
          <SummaryCard
            label="Total Operators"
            value={counts.total}
            icon={<Building2 size={22} />}
          />

          <SummaryCard
            label="Pending"
            value={counts.pending}
            icon={<Clock3 size={22} />}
            amber
          />

          <SummaryCard
            label="Approved"
            value={counts.approved}
            icon={<CheckCircle2 size={22} />}
            green
          />

          <SummaryCard
            label="Rejected"
            value={counts.rejected}
            icon={<XCircle size={22} />}
            red
          />
        </section>

        <section className="rounded-3xl border border-slate-200 bg-white p-5 shadow-sm">
          <div className="grid grid-cols-1 gap-4 lg:grid-cols-12">
            <div className="lg:col-span-8">
              <label className="text-xs font-black uppercase tracking-widest text-slate-500">
                Search operators
              </label>

              <div className="mt-2 flex items-center gap-3 rounded-xl border border-slate-300 bg-slate-50 px-4 py-3">
                <Search
                  size={18}
                  className="text-slate-400"
                />

                <input
                  type="search"
                  value={searchText}
                  onChange={(event) =>
                    setSearchText(event.target.value)
                  }
                  placeholder="Search organization, registration number, email..."
                  className="w-full bg-transparent text-sm outline-none"
                />
              </div>
            </div>

            <div className="lg:col-span-4">
              <label className="text-xs font-black uppercase tracking-widest text-slate-500">
                Status
              </label>

              <select
                value={statusFilter}
                onChange={(event) =>
                  setStatusFilter(event.target.value)
                }
                className="mt-2 w-full rounded-xl border border-slate-300 bg-slate-50 px-4 py-3 text-sm font-bold outline-none"
              >
                <option value="ALL">
                  All statuses
                </option>
                <option value="PENDING">
                  Pending
                </option>
                <option value="APPROVED">
                  Approved
                </option>
                <option value="REJECTED">
                  Rejected
                </option>
                <option value="SUSPENDED">
                  Suspended
                </option>
              </select>
            </div>
          </div>
        </section>

        {error && (
          <div className="flex items-start gap-3 rounded-2xl border border-red-200 bg-red-50 p-4 text-sm font-bold text-red-700">
            <AlertTriangle
              size={19}
              className="mt-0.5 shrink-0"
            />
            <p>{error}</p>
          </div>
        )}

        <section className="overflow-hidden rounded-3xl border border-slate-200 bg-white shadow-sm">
          <div className="flex items-center justify-between border-b border-slate-200 px-5 py-5">
            <div>
              <h2 className="text-xl font-black text-slate-900">
                Operator Applications
              </h2>

              <p className="mt-1 text-sm text-slate-500">
                {filteredOperators.length} operator(s)
                shown
              </p>
            </div>

            <button
              type="button"
              onClick={loadOperators}
              className="flex items-center gap-2 rounded-xl border border-slate-300 px-4 py-2 text-sm font-black text-slate-700 hover:bg-slate-50"
            >
              <RefreshCw size={17} />
              Refresh
            </button>
          </div>

          {loading ? (
            <div className="flex min-h-[300px] items-center justify-center">
              <Loader2
                size={38}
                className="animate-spin text-[#08264a]"
              />
            </div>
          ) : filteredOperators.length === 0 ? (
            <div className="px-6 py-14 text-center">
              <Building2
                size={42}
                className="mx-auto text-slate-300"
              />

              <h3 className="mt-4 text-xl font-black text-slate-900">
                No operators found
              </h3>
            </div>
          ) : (
            <>
              <div className="hidden overflow-x-auto lg:block">
                <table className="w-full min-w-[1000px] text-left">
                  <thead className="bg-slate-50">
                    <tr className="text-xs font-black uppercase tracking-wider text-slate-500">
                      <th className="px-6 py-4">
                        Organization
                      </th>
                      <th className="px-6 py-4">
                        Contact
                      </th>
                      <th className="px-6 py-4">
                        Registration
                      </th>
                      <th className="px-6 py-4">
                        Type
                      </th>
                      <th className="px-6 py-4">
                        Status
                      </th>
                      <th className="px-6 py-4 text-right">
                        Action
                      </th>
                    </tr>
                  </thead>

                  <tbody className="divide-y divide-slate-100">
                    {filteredOperators.map(
                      (operator) => (
                        <tr
                          key={operator.id}
                          className="hover:bg-slate-50"
                        >
                          <td className="px-6 py-5">
                            <p className="font-black text-slate-900">
                              {operator.name}
                            </p>

                            <p className="mt-1 text-xs text-slate-500">
                              OP-{operator.id}
                            </p>
                          </td>

                          <td className="px-6 py-5">
                            <p className="text-sm font-bold text-slate-700">
                              {operator.contactPerson}
                            </p>

                            <p className="mt-1 text-xs text-slate-500">
                              {operator.email}
                            </p>
                          </td>

                          <td className="px-6 py-5">
                            <p className="font-black text-slate-900">
                              {
                                operator.registrationNumber
                              }
                            </p>

                            <p className="mt-1 text-xs text-slate-500">
                              Permit:{" "}
                              {operator.permitNumber ||
                                "N/A"}
                            </p>
                          </td>

                          <td className="px-6 py-5 text-sm font-bold text-slate-700">
                            {formatOperatorType(
                              operator.operatorType
                            )}
                          </td>

                          <td className="px-6 py-5">
                            <StatusBadge
                              status={
                                operator.verificationStatus
                              }
                            />
                          </td>

                          <td className="px-6 py-5 text-right">
                            <button
                              type="button"
                              onClick={() =>
                                setSelectedOperator(
                                  operator
                                )
                              }
                              className="inline-flex items-center gap-2 rounded-xl bg-[#08264a] px-4 py-2.5 text-sm font-black text-white hover:bg-[#0d3566]"
                            >
                              <Eye size={16} />
                              Review
                            </button>
                          </td>
                        </tr>
                      )
                    )}
                  </tbody>
                </table>
              </div>

              <div className="grid grid-cols-1 gap-4 p-4 sm:grid-cols-2 lg:hidden">
                {filteredOperators.map((operator) => (
                  <article
                    key={operator.id}
                    className="rounded-2xl border border-slate-200 p-5"
                  >
                    <div className="flex items-start justify-between gap-3">
                      <div>
                        <h3 className="font-black text-slate-900">
                          {operator.name}
                        </h3>

                        <p className="mt-1 text-xs text-slate-500">
                          {operator.registrationNumber}
                        </p>
                      </div>

                      <StatusBadge
                        status={
                          operator.verificationStatus
                        }
                      />
                    </div>

                    <p className="mt-4 text-sm text-slate-600">
                      {operator.contactPerson}
                    </p>

                    <p className="mt-1 text-sm text-slate-500">
                      {operator.email}
                    </p>

                    <button
                      type="button"
                      onClick={() =>
                        setSelectedOperator(operator)
                      }
                      className="mt-5 flex w-full items-center justify-center gap-2 rounded-xl bg-[#08264a] py-3 text-sm font-black text-white"
                    >
                      <Eye size={17} />
                      Review Application
                    </button>
                  </article>
                ))}
              </div>
            </>
          )}
        </section>
      </div>

      {selectedOperator && !showRejectModal && (
        <OperatorDetailsModal
          operator={selectedOperator}
          loading={actionLoading}
          onClose={() =>
            setSelectedOperator(null)
          }
          onApprove={() =>
            approveOperator(selectedOperator.id)
          }
          onReject={() => {
            setRejectionReason("");
            setShowRejectModal(true);
          }}
        />
      )}

      {selectedOperator && showRejectModal && (
        <RejectModal
          reason={rejectionReason}
          setReason={setRejectionReason}
          loading={actionLoading}
          onCancel={() => {
            setShowRejectModal(false);
            setRejectionReason("");
          }}
          onReject={rejectOperator}
        />
      )}
    </AdminLayout>
  );
}

function OperatorDetailsModal({
  operator,
  loading,
  onClose,
  onApprove,
  onReject,
}) {
  return (
    <div className="fixed inset-0 z-[999] flex items-center justify-center overflow-y-auto bg-black/50 px-4 py-6 backdrop-blur-sm">
      <div className="w-full max-w-2xl rounded-3xl bg-white shadow-2xl">
        <div className="flex items-center justify-between border-b border-slate-200 p-6">
          <div>
            <p className="text-xs font-black uppercase tracking-widest text-slate-500">
              Operator Application
            </p>

            <h2 className="mt-1 text-2xl font-black text-slate-900">
              {operator.name}
            </h2>
          </div>

          <button
            type="button"
            onClick={onClose}
            className="rounded-xl p-2 hover:bg-slate-100"
          >
            <X size={22} />
          </button>
        </div>

        <div className="grid grid-cols-1 gap-4 p-6 sm:grid-cols-2">
          <ModalDetail
            icon={<Building2 size={17} />}
            label="Operator Type"
            value={formatOperatorType(
              operator.operatorType
            )}
          />

          <ModalDetail
            icon={<FileCheck2 size={17} />}
            label="Registration Number"
            value={operator.registrationNumber}
          />

          <ModalDetail
            icon={<Mail size={17} />}
            label="Email"
            value={operator.email}
          />

          <ModalDetail
            icon={<Phone size={17} />}
            label="Phone"
            value={operator.phone}
          />

          <ModalDetail
            icon={<MapPin size={17} />}
            label="Address"
            value={operator.address}
          />

          <ModalDetail
            icon={<FileCheck2 size={17} />}
            label="Permit Number"
            value={operator.permitNumber}
          />
        </div>

        {operator.verificationStatus ===
          "PENDING" && (
          <div className="flex flex-col gap-3 border-t border-slate-200 bg-slate-50 p-5 sm:flex-row">
            <button
              type="button"
              onClick={onReject}
              disabled={loading}
              className="flex flex-1 items-center justify-center gap-2 rounded-xl border border-red-300 bg-red-50 py-3 text-sm font-black text-red-700 hover:bg-red-100"
            >
              <XCircle size={18} />
              Reject
            </button>

            <button
              type="button"
              onClick={onApprove}
              disabled={loading}
              className="flex flex-1 items-center justify-center gap-2 rounded-xl bg-emerald-600 py-3 text-sm font-black text-white hover:bg-emerald-700"
            >
              {loading ? (
                <Loader2
                  size={18}
                  className="animate-spin"
                />
              ) : (
                <CheckCircle2 size={18} />
              )}
              Approve Operator
            </button>
          </div>
        )}
      </div>
    </div>
  );
}

function RejectModal({
  reason,
  setReason,
  loading,
  onCancel,
  onReject,
}) {
  return (
    <div className="fixed inset-0 z-[1000] flex items-center justify-center bg-black/50 px-4 backdrop-blur-sm">
      <div className="w-full max-w-md rounded-3xl bg-white shadow-2xl">
        <div className="p-6 text-center">
          <div className="mx-auto flex h-16 w-16 items-center justify-center rounded-2xl bg-red-100 text-red-600">
            <ShieldAlert size={31} />
          </div>

          <h2 className="mt-5 text-2xl font-black text-slate-900">
            Reject operator?
          </h2>

          <p className="mt-2 text-sm text-slate-500">
            Enter a clear reason so the operator can
            correct the application.
          </p>

          <textarea
            rows={5}
            value={reason}
            onChange={(event) =>
              setReason(event.target.value)
            }
            placeholder="Enter rejection reason..."
            className="mt-5 w-full resize-none rounded-2xl border border-slate-300 bg-slate-50 p-4 text-left text-sm outline-none focus:border-red-500"
          />
        </div>

        <div className="flex gap-3 border-t border-slate-200 bg-slate-50 p-5">
          <button
            type="button"
            onClick={onCancel}
            disabled={loading}
            className="flex-1 rounded-xl border border-slate-300 bg-white py-3 text-sm font-black text-slate-700"
          >
            Cancel
          </button>

          <button
            type="button"
            onClick={onReject}
            disabled={loading || !reason.trim()}
            className="flex flex-1 items-center justify-center gap-2 rounded-xl bg-red-600 py-3 text-sm font-black text-white disabled:opacity-50"
          >
            {loading && (
              <Loader2
                size={17}
                className="animate-spin"
              />
            )}
            Reject
          </button>
        </div>
      </div>
    </div>
  );
}

function SummaryCard({
  label,
  value,
  icon,
  green,
  amber,
  red,
}) {
  return (
    <div className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">
      <div
        className={`flex h-11 w-11 items-center justify-center rounded-xl ${
          green
            ? "bg-emerald-100 text-emerald-700"
            : amber
            ? "bg-amber-100 text-amber-700"
            : red
            ? "bg-red-100 text-red-700"
            : "bg-blue-100 text-[#08264a]"
        }`}
      >
        {icon}
      </div>

      <p className="mt-4 text-xs font-black uppercase tracking-widest text-slate-500">
        {label}
      </p>

      <p className="mt-2 text-3xl font-black text-slate-900">
        {value}
      </p>
    </div>
  );
}

function StatusBadge({ status }) {
  const styles = {
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
      className={`inline-flex rounded-full px-3 py-1.5 text-[10px] font-black ${
        styles[status] ||
        "bg-slate-100 text-slate-600"
      }`}
    >
      {status || "UNKNOWN"}
    </span>
  );
}

function ModalDetail({ icon, label, value }) {
  return (
    <div className="rounded-2xl bg-slate-50 p-4">
      <div className="flex items-center gap-2 text-slate-500">
        {icon}
        <p className="text-[10px] font-black uppercase tracking-widest">
          {label}
        </p>
      </div>

      <p className="mt-2 break-words text-sm font-black text-slate-900">
        {value || "Not available"}
      </p>
    </div>
  );
}

function formatOperatorType(type) {
  return String(type || "")
    .toLowerCase()
    .split("_")
    .map(
      (part) =>
        part.charAt(0).toUpperCase() +
        part.slice(1)
    )
    .join(" ");
}
