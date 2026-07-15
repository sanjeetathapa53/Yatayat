export default function Button({
    children,
    variant = "primary",
    className = "",
  }) {
    const styles = {
      primary:
        "bg-emerald-600 hover:bg-emerald-700 text-white",
      secondary:
        "bg-white border border-slate-300 text-slate-800 hover:bg-slate-50",
    };
  
    return (
      <button
        className={`
          px-6 py-3 rounded-xl
          font-semibold
          transition-all duration-300
          ${styles[variant]}
          ${className}
        `}
      >
        {children}
      </button>
    );
  }