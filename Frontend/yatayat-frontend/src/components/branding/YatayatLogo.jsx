const SIZE_STYLES = {
  sm: { icon: "h-8 w-8", wordmark: "text-lg", subtitle: "text-[10px]" },
  md: { icon: "h-10 w-10", wordmark: "text-xl sm:text-2xl", subtitle: "text-[11px]" },
  lg: { icon: "h-12 w-12", wordmark: "text-2xl sm:text-3xl", subtitle: "text-xs" },
};

export default function YatayatLogo({
  variant = "compact",
  size = "md",
  light = false,
  subtitle = "Smart Public Transportation Management System",
  className = "",
}) {
  const styles = SIZE_STYLES[size] || SIZE_STYLES.md;
  const iconOnly = variant === "icon";
  const showSubtitle = variant === "full" && subtitle;
  const containerClass = ["inline-flex min-w-0 items-center gap-2.5", className].filter(Boolean).join(" ");
  const iconClass = [styles.icon, "shrink-0 object-contain"].join(" ");
  const wordmarkClass = ["block truncate font-black tracking-tight", styles.wordmark, light ? "text-white" : "text-[#08264a]"].join(" ");
  const subtitleClass = ["mt-1 block max-w-52 leading-tight", styles.subtitle, light ? "text-slate-300" : "text-slate-500"].join(" ");

  return (
    <span className={containerClass} aria-label="Yatayat">
      <img src="/favicon.svg" alt="" aria-hidden="true" className={iconClass} />
      {!iconOnly && (
        <span className="min-w-0 leading-none">
          <span className={wordmarkClass}>Yatayat</span>
          {showSubtitle && <span className={subtitleClass}>{subtitle}</span>}
        </span>
      )}
    </span>
  );
}
