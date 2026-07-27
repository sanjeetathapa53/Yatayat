export default function OtpModal({
  otp, setOtp, onVerify, onClose, onResend, resendSeconds = 0, loading = false,
}) {
    return (
      <div className="responsive-modal-backdrop fixed inset-0 z-50 flex justify-center bg-black/40 sm:items-center">
        <div className="responsive-modal-panel w-full max-w-sm rounded-3xl bg-white p-5 shadow-xl sm:p-6">
          <h2 className="text-2xl font-black text-[#08264a]">Verify OTP</h2>
  
          <p className="mt-2 text-sm text-slate-500">
            Enter the OTP sent to your email.
          </p>
  
          <input
            type="text"
            value={otp}
            onChange={(e) => setOtp(e.target.value.replace(/\D/g, "").slice(0, 6))}
            placeholder="Enter OTP"
            inputMode="numeric"
            autoComplete="one-time-code"
            maxLength={6}
            className="mt-5 w-full rounded-2xl border border-slate-200 bg-slate-50 px-4 py-3 text-sm outline-none"
          />
  
          <button
            type="button"
            onClick={onVerify}
            disabled={loading || otp.length !== 6}
            className="mt-5 w-full rounded-2xl bg-[#08264a] py-3 text-sm font-bold text-white"
          >
            Verify OTP
          </button>

          {onResend && (
            <button
              type="button"
              onClick={onResend}
              disabled={loading || resendSeconds > 0}
              className="mt-3 w-full text-sm font-bold text-[#08264a] disabled:text-slate-400"
            >
              {resendSeconds > 0 ? `Resend OTP in ${resendSeconds}s` : "Resend OTP"}
            </button>
          )}
  
          <button
            type="button"
            onClick={onClose}
            className="mt-3 w-full rounded-2xl border border-slate-200 py-3 text-sm font-bold text-slate-600"
          >
            Cancel
          </button>
        </div>
      </div>
    );
  }
