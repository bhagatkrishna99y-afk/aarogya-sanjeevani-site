import { Accordion, AccordionItem } from "@/components/ui/Accordion";
import { Section } from "@/components/ui/Section";
import { homeContent } from "@/content/home";

export function FaqSection() {
  return (
    <Section className="bg-white" eyebrow="FAQ" heading="Frequently asked questions">
      <Accordion aria-label="Frequently asked questions" className="max-w-3xl">
        {homeContent.faq.map((item, index) => (
          <AccordionItem key={item.question} id={`faq-${index}`} question={item.question}>
            <p>{item.answer}</p>
          </AccordionItem>
        ))}
      </Accordion>
    </Section>
  );
}
