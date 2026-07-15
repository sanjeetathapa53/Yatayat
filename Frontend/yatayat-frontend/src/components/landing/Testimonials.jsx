import { useLanguage } from "../../context/LanguageContext";

export default function Testimonials() {
  const { t } = useLanguage();

  const testimonials = [
    {
      name: languageName("Bikash Shrestha", "बिकाश श्रेष्ठ", t),
      role: languageName("Daily Commuter", "दैनिक यात्रु", t),
      text: languageName(
        "Yatayat has completely changed how I plan my mornings.",
        "यातायातले मेरो बिहानको यात्रा योजना गर्ने तरिका नै परिवर्तन गरिदिएको छ।",
        t
      ),
    },
    {
      name: languageName("Anjali Rana", "अञ्जली राणा", t),
      role: languageName("University Student", "विश्वविद्यालय विद्यार्थी", t),
      text: languageName(
        "Digital ticketing is a lifesaver. It feels clean and professional.",
        "डिजिटल टिकटिङ धेरै उपयोगी छ। यो सफा र व्यावसायिक महसुस हुन्छ।",
        t
      ),
    },
    {
      name: languageName("Rajesh Hamal", "राजेश हमाल", t),
      role: languageName("Transit Advocate", "यातायात समर्थक", t),
      text: languageName(
        "This kind of system can make public transport more reliable.",
        "यस्तो प्रणालीले सार्वजनिक यातायातलाई अझ भरपर्दो बनाउन सक्छ।",
        t
      ),
    },
  ];

  return (
    <section className="bg-white px-6 py-20">
      <h2 className="text-center text-4xl font-extrabold text-[#08264a]">
        {t.testimonialsTitle}
      </h2>

      <div className="mx-auto mt-14 grid max-w-7xl gap-8 md:grid-cols-3">
        {testimonials.map((item) => (
          <div key={item.name} className="rounded-3xl border border-slate-200 p-8 shadow-sm">
            <div className="flex items-center gap-4">
              <div className="flex h-12 w-12 items-center justify-center rounded-full bg-slate-200 font-bold text-[#08264a]">
                {item.name.charAt(0)}
              </div>

              <div>
                <h4 className="font-bold text-[#08264a]">{item.name}</h4>
                <p className="text-xs text-slate-500">{item.role}</p>
              </div>
            </div>

            <p className="mt-6 italic leading-7 text-slate-600">
              “{item.text}”
            </p>
          </div>
        ))}
      </div>
    </section>
  );
}

function languageName(en, np, t) {
  return t.navRoutes === "Routes" ? en : np;
}