export default function SectionTitle({
    title,
    subtitle,
  }) {
    return (
      <div className="text-center max-w-3xl mx-auto">
        <h2 className="text-3xl font-semibold text-slate-900 sm:text-4xl">
          {title}
        </h2>
  
        <p className="mt-5 text-slate-600 text-lg">
          {subtitle}
        </p>
      </div>
    );
  }