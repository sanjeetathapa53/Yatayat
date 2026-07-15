import Navbar from "../components/layout/Navbar";
import Hero from "../components/landing/Hero";
import Connectivity from "../components/landing/Connectivity";
import Features from "../components/landing/Features";
import Testimonials from "../components/landing/Testimonials";
import Footer from "../components/layout/Footer";

export default function LandingPage() {
  return (
    <>
      <Navbar />
      <Hero />
      <Connectivity />
      <Features />
      <Testimonials />
      <Footer />
    </>
  );
}