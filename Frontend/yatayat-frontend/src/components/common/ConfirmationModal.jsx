import { useEffect, useRef } from "react";

export default function ConfirmationModal({
  open,
  title,
  message,
  confirmLabel,
  cancelLabel = "Cancel",
  destructive = false,
  busy = false,
  onConfirm,
  onClose,
}) {
  const cancelButtonRef = useRef(null);

  useEffect(() => {
    if (!open) return undefined;

    const handleKeyDown = (event) => {
      if (event.key === "Escape" && !busy) onClose();
    };

    document.addEventListener("keydown", handleKeyDown);
    cancelButtonRef.current?.focus();
    return () => document.removeEventListener("keydown", handleKeyDown);
  }, [busy, onClose, open]);

  if (!open) return null;

  const handleBackdropClick = (event) => {
    if (event.target === event.currentTarget && !busy) onClose();
  };

  return (
    <div
      className="fixed inset-0 z-50 flex items-end justify-center bg-slate-950/60 p-4 sm:items-center"
      onMouseDown={handleBackdropClick}
      role="presentation"
    >
      <div
        role="dialog"
        aria-modal="true"
        aria-labelledby="confirmation-modal-title"
        aria-describedby="confirmation-modal-message"
        className="w-full max-w-md rounded-3xl border border-slate-200 bg-white p-6 shadow-2xl sm:p-7"
      >
        <h2 id="confirmation-modal-title" className="text-xl font-black text-slate-900 sm:text-2xl">
          {title}
        </h2>
        <p id="confirmation-modal-message" className="mt-3 text-sm font-medium leading-6 text-slate-600">
          {message}
        </p>
        <div className="mt-7 flex flex-col-reverse gap-3 sm:flex-row sm:justify-end">
          <button
            ref={cancelButtonRef}
            type="button"
            onClick={onClose}
            disabled={busy}
            className="rounded-xl border border-slate-300 bg-white px-5 py-3 text-sm font-black text-slate-700 transition hover:bg-slate-50 focus:outline-none focus:ring-2 focus:ring-slate-400 focus:ring-offset-2 disabled:cursor-not-allowed disabled:opacity-50"
          >
            {cancelLabel}
          </button>
          <button
            type="button"
            onClick={onConfirm}
            disabled={busy}
            className={`rounded-xl px-5 py-3 text-sm font-black text-white transition focus:outline-none focus:ring-2 focus:ring-offset-2 disabled:cursor-not-allowed disabled:opacity-50 ${
              destructive
                ? "bg-red-600 hover:bg-red-700 focus:ring-red-500"
                : "bg-[#08264a] hover:bg-[#0b3565] focus:ring-[#08264a]"
            }`}
          >
            {busy ? "Removing..." : confirmLabel}
          </button>
        </div>
      </div>
    </div>
  );
}
