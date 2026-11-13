import { Seo } from "@/components/seo/Seo";
import { AppDownloadSection } from "@/components/sections/AppDownloadSection";
import { BenefitsSection } from "@/components/sections/BenefitsSection";
import { FaqSection } from "@/components/sections/FaqSection";
import { FinalCtaSection } from "@/components/sections/FinalCtaSection";
import { HeroSection } from "@/components/sections/HeroSection";
import { HowItWorksSection } from "@/components/sections/HowItWorksSection";
import { ServicesSection } from "@/components/sections/ServicesSection";
import { TrustSafetySection } from "@/components/sections/TrustSafetySection";
import { homeContent } from "@/content/home";

export function HomePage() {
  return (
    <div>
      <Seo title="Home" description={homeContent.hero.subheading} />
      <HeroSection />
      <BenefitsSection />
      <HowItWorksSection />
      <ServicesSection />
      <TrustSafetySection />
      <AppDownloadSection />
      <FaqSection />
      <FinalCtaSection />
    </div>
  );
}
