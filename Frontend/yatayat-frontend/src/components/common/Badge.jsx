export default function Badge({ children }) {
    return (
      <span className="inline-flex items-center rounded-full bg-emerald-100 px-4 py-2 text-sm font-semibold text-emerald-700">
        {children}
      </span>
    );
  }