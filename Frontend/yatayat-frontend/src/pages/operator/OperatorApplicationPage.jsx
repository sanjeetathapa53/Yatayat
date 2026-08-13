import { useMemo, useState } from "react";
import {
  ArrowRight,
  Building2,
  FileText,
  Hash,
  Landmark,
  Loader2,
  MapPin,
  Phone,
  ShieldCheck,
  UserRound,
} from "lucide-react";
import {
  useLocation,
  useNavigate,
} from "react-router-dom";
import { toast } from "react-toastify";

import AuthLayout from "../../components/auth/AuthLayout";
import { API_BASE_URL } from "../../utils/api";

export default function OperatorApplicationPage() {
  const navigate = useNavigate();
  const location = useLocation();

  const loggedInUser = useMemo(() => {
    try {
      return JSON.parse(
        localStorage.getItem("yatayatUser") || "null"
      );
    } catch (error) {
      console.error("Invalid logged-in user data:", error);
      return null;
    }
  }, []);

  const existingApplication =
    location.state?.application || null;

  const isResubmission =
    location.state?.mode === "RESUBMIT";

  const [organizationName, setOrganizationName] =
    useState(existingApplication?.name || "");

  const [operatorType, setOperatorType] = useState(
    existingApplication?.operatorType ||
      "PRIVATE_COMPANY"
  );

  const [
    registrationNumber,
    setRegistrationNumber,
  ] = useState(
    existingApplication?.registrationNumber || ""
  );

  const [permitNumber, setPermitNumber] = useState(
    existingApplication?.permitNumber || ""
  );

  const [contactPerson, setContactPerson] = useState(
    existingApplication?.contactPerson ||
      loggedInUser?.fullName ||
      ""
  );

  const [phone, setPhone] = useState(
    existingApplication?.phone ||
      loggedInUser?.phone ||
      ""
  );

  const [address, setAddress] = useState(
    existingApplication?.address || ""
  );

  const [submitting, setSubmitting] =
    useState(false);

  const handleSubmit = async (event) => {
    event.preventDefault();

    if (!loggedInUser?.id) {
      toast.error(
        "Operator account information was not found. Please log in again."
      );
      return;
    }

    if (
      !organizationName.trim() ||
      !operatorType ||
      !registrationNumber.trim() ||
      !contactPerson.trim() ||
      !phone.trim() ||
      !address.trim()
    ) {
      toast.error("Please complete all required fields.");
      return;
    }

    const endpoint = isResubmission
      ? `${API_BASE_URL}/api/operators/application/resubmit`
      : `${API_BASE_URL}/api/operators/application`;

    try {
      setSubmitting(true);

      const response = await fetch(endpoint, {
        method: isResubmission ? "PUT" : "POST",
        credentials: "include",
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify({
          userId: loggedInUser.id,
          organizationName:
            organizationName.trim(),
          operatorType,
          registrationNumber:
            registrationNumber.trim(),
          permitNumber: permitNumber.trim(),
          contactPerson: contactPerson.trim(),
          phone: phone.trim(),
          address: address.trim(),
        }),
      });

      const data = await response.json();

      if (!response.ok || !data.success) {
        throw new Error(
          data.message ||
            "Unable to submit operator application."
        );
      }

      const latestStatus = String(
        data.status || "PENDING"
      ).toUpperCase();

      localStorage.setItem(
        "operatorApplicationStatus",
        latestStatus
      );

      toast.success(
        isResubmission
          ? "Operator application resubmitted successfully."
          : "Operator application submitted successfully."
      );

      navigate("/operator/application-status", {
        replace: true,
        state: {
          status: latestStatus,
          message: data.message,
          operator: data.operator || null,
        },
      });
    } catch (error) {
      console.error(
        "Operator application error:",
        error
      );

      toast.error(
        error.message ||
          "Unable to submit operator application."
      );
    } finally {
      setSubmitting(false);
    }
  };

  const handleLogout = () => {
    localStorage.removeItem("yatayatUser");
    localStorage.removeItem("loginTime");
    localStorage.removeItem(
      "operatorApplicationStatus"
    );

    navigate("/login", {
      replace: true,
    });
  };

  return (
    <AuthLayout>
      <div className="w-full max-w-4xl overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-xl">
        <div className="bg-[#08264a] px-6 py-8 text-white sm:px-9">
          <div className="flex flex-col gap-4 sm:flex-row sm:items-center">
            <div className="flex h-16 w-16 shrink-0 items-center justify-center rounded-2xl bg-white/15">
              <Building2 size={32} />
            </div>

            <div>
              <p className="text-xs font-semibold uppercase tracking-[0.22em] text-blue-200">
                Transport Operator Verification
              </p>

              <h1 className="mt-2 text-2xl font-semibold sm:text-3xl">
                {isResubmission
                  ? "Correct and Resubmit Application"
                  : "Submit Organization Application"}
              </h1>

              <p className="mt-2 max-w-2xl text-sm leading-6 text-slate-300">
                Register your government operator, private
                company, cooperative, or independently owned
                public transport business for Yatayat
                verification.
              </p>
            </div>
          </div>
        </div>

        <form
          onSubmit={handleSubmit}
          className="space-y-5 px-5 py-5 sm:px-6"
        >
          {isResubmission &&
            existingApplication?.rejectionReason && (
              <section className="rounded-2xl border border-red-200 bg-red-50 p-5">
                <h2 className="font-semibold text-red-800">
                  Admin rejection reason
                </h2>

                <p className="mt-2 text-sm leading-6 text-red-700">
                  {
                    existingApplication.rejectionReason
                  }
                </p>
              </section>
            )}

          <section>
            <SectionTitle
              icon={<Building2 size={20} />}
              title="Organization Information"
            />

            <div className="mt-4 grid grid-cols-1 gap-4 sm:grid-cols-2">
              <FormField
                label="Organization Name"
                icon={<Building2 size={18} />}
                value={organizationName}
                onChange={setOrganizationName}
                placeholder="Example: Himalayan Transport Pvt. Ltd."
                required
              />

              <SelectField
                label="Operator Type"
                icon={<Landmark size={18} />}
                value={operatorType}
                onChange={setOperatorType}
                options={[
                  {
                    value: "GOVERNMENT",
                    label: "Government Operator",
                  },
                  {
                    value: "PRIVATE_COMPANY",
                    label: "Private Company",
                  },
                  {
                    value: "COOPERATIVE",
                    label: "Transport Cooperative",
                  },
                  {
                    value: "INDIVIDUAL_OWNER",
                    label: "Independent Owner",
                  },
                ]}
              />

              <FormField
                label="Registration Number"
                icon={<Hash size={18} />}
                value={registrationNumber}
                onChange={setRegistrationNumber}
                placeholder="Example: REG-OP-001"
                required
              />

              <FormField
                label="Transport Permit Number"
                icon={<FileText size={18} />}
                value={permitNumber}
                onChange={setPermitNumber}
                placeholder="Optional permit number"
              />
            </div>
          </section>

          <section>
            <SectionTitle
              icon={<UserRound size={20} />}
              title="Contact Information"
            />

            <div className="mt-4 grid grid-cols-1 gap-4 sm:grid-cols-2">
              <FormField
                label="Contact Person"
                icon={<UserRound size={18} />}
                value={contactPerson}
                onChange={setContactPerson}
                placeholder="Owner or operations manager"
                required
              />

              <FormField
                label="Phone Number"
                icon={<Phone size={18} />}
                value={phone}
                onChange={setPhone}
                placeholder="+977 98XXXXXXXX"
                required
                type="tel"
              />

              <div className="sm:col-span-2">
                <FormField
                  label="Office Address"
                  icon={<MapPin size={18} />}
                  value={address}
                  onChange={setAddress}
                  placeholder="Office address, district and province"
                  required
                />
              </div>
            </div>
          </section>

          <section className="rounded-2xl border border-blue-200 bg-blue-50 p-5">
            <div className="flex items-start gap-3">
              <ShieldCheck
                size={22}
                className="mt-0.5 shrink-0 text-[#08264a]"
              />

              <div>
                <h2 className="font-semibold text-slate-900">
                  Admin verification required
                </h2>

                <p className="mt-2 text-sm leading-6 text-slate-600">
                  Your operator dashboard and bus-management
                  features remain locked until the Yatayat
                  administrator verifies and approves this
                  application.
                </p>
              </div>
            </div>
          </section>

          <section className="rounded-2xl border border-slate-200 bg-slate-50 p-5">
            <p className="text-xs font-semibold uppercase tracking-widest text-slate-500">
              Account Information
            </p>

            <div className="mt-4 grid grid-cols-1 gap-4 sm:grid-cols-2">
              <AccountDetail
                label="Account Holder"
                value={loggedInUser?.fullName}
              />

              <AccountDetail
                label="Account Email"
                value={loggedInUser?.email}
              />
            </div>
          </section>

          <label className="flex items-start gap-3 text-sm leading-6 text-slate-600">
            <input
              type="checkbox"
              required
              className="mt-1 h-4 w-4 accent-[#08264a]"
            />

            <span>
              I confirm that this information is accurate and
              that Yatayat may verify the organization,
              registration and permit details.
            </span>
          </label>

          <div className="flex flex-col gap-3 border-t border-slate-200 pt-6 sm:flex-row sm:justify-between">
            <button
              type="button"
              onClick={handleLogout}
              disabled={submitting}
              className="rounded-2xl border border-slate-300 px-6 py-2.5 text-sm font-semibold text-slate-700 transition hover:bg-slate-50 disabled:opacity-60"
            >
              Logout
            </button>

            <button
              type="submit"
              disabled={submitting}
              className="flex items-center justify-center gap-2 rounded-2xl bg-[#08264a] px-8 py-2.5.5 text-sm font-semibold text-white transition hover:bg-[#0d3566] disabled:cursor-not-allowed disabled:opacity-60"
            >
              {submitting ? (
                <>
                  <Loader2
                    size={18}
                    className="animate-spin"
                  />
                  {isResubmission
                    ? "Resubmitting..."
                    : "Submitting..."}
                </>
              ) : (
                <>
                  {isResubmission
                    ? "Resubmit Application"
                    : "Submit for Verification"}
                  <ArrowRight size={18} />
                </>
              )}
            </button>
          </div>
        </form>
      </div>
    </AuthLayout>
  );
}

function SectionTitle({ icon, title }) {
  return (
    <div className="flex items-center gap-3">
      <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-blue-100 text-[#08264a]">
        {icon}
      </div>

      <h2 className="text-xl font-semibold text-slate-900">
        {title}
      </h2>
    </div>
  );
}

function FormField({
  label,
  icon,
  value,
  onChange,
  placeholder,
  required = false,
  type = "text",
}) {
  return (
    <div>
      <label className="mb-2 block text-xs font-semibold uppercase tracking-wider text-slate-500">
        {label}
        {required ? " *" : ""}
      </label>

      <div className="flex items-center gap-3 rounded-2xl border border-slate-200 bg-slate-50 px-4 py-2.5 transition focus-within:border-[#08264a] focus-within:bg-white">
        <span className="shrink-0 text-slate-400">
          {icon}
        </span>

        <input
          type={type}
          value={value}
          onChange={(event) =>
            onChange(event.target.value)
          }
          placeholder={placeholder}
          required={required}
          className="w-full bg-transparent text-sm text-slate-900 outline-none placeholder:text-slate-400"
        />
      </div>
    </div>
  );
}

function SelectField({
  label,
  icon,
  value,
  onChange,
  options,
}) {
  return (
    <div>
      <label className="mb-2 block text-xs font-semibold uppercase tracking-wider text-slate-500">
        {label} *
      </label>

      <div className="flex items-center gap-3 rounded-2xl border border-slate-200 bg-slate-50 px-4 py-2.5 transition focus-within:border-[#08264a] focus-within:bg-white">
        <span className="shrink-0 text-slate-400">
          {icon}
        </span>

        <select
          value={value}
          onChange={(event) =>
            onChange(event.target.value)
          }
          required
          className="w-full bg-transparent text-sm text-slate-900 outline-none"
        >
          {options.map((option) => (
            <option
              key={option.value}
              value={option.value}
            >
              {option.label}
            </option>
          ))}
        </select>
      </div>
    </div>
  );
}

function AccountDetail({ label, value }) {
  return (
    <div className="rounded-xl border border-slate-200 bg-white p-4">
      <p className="text-[10px] font-semibold uppercase tracking-widest text-slate-500">
        {label}
      </p>

      <p className="mt-2 wrap-break-word text-sm font-semibold text-slate-900">
        {value || "Not available"}
      </p>
    </div>
  );
}
