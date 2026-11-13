/**
 * All copy for the Services page -- see src/pages/public/ServicesPage.tsx
 * for how each section renders this. The services listed here are
 * EXAMPLES for orientation only; the live, bookable catalog is managed
 * inside the Patient app and can change independently of this page.
 */
import {
  Stethoscope,
  HeartPulse,
  Activity,
  Home as HomeIcon,
  Users,
  type LucideIcon,
} from "lucide-react";
import { siteContent } from "./site";

export type ServiceMode = "in-person" | "home-visit" | "online";

export const SERVICE_MODE_LABEL: Record<ServiceMode, string> = {
  "in-person": "In-person",
  "home-visit": "Home visit",
  online: "Online",
};

export interface ExampleService {
  name: string;
  description: string;
  modes: ServiceMode[];
}

export interface ServiceCategoryDetail {
  icon: LucideIcon;
  name: string;
  description: string;
  examples: ExampleService[];
}

export interface RequestStep {
  title: string;
  description: string;
}

export const servicesContent = {
  intro: {
    heading: "Services",
    paragraph:
      "Services on Aarogya Sanjeevani are discovered here and requested through the Patient app -- this page is for getting an idea of what's available before you download it. The categories and examples below are illustrative; the live, bookable catalog is managed in the app and can change over time.",
  },

  categories: [
    {
      icon: Stethoscope,
      name: "General Healthcare",
      description: "Everyday consultations for common health concerns.",
      examples: [
        {
          name: "General consultation",
          description: "A conversation with a doctor about a non-urgent health concern.",
          modes: ["in-person", "online"],
        },
        {
          name: "Follow-up visit",
          description: "A check-in on an ongoing concern or earlier consultation.",
          modes: ["in-person", "online"],
        },
      ],
    },
    {
      icon: HeartPulse,
      name: "Specialist Consultation",
      description: "Connect with doctors focused on a particular area of care.",
      examples: [
        {
          name: "Specialist consultation",
          description: "A consultation with a doctor focused on a specific area of care.",
          modes: ["in-person", "online"],
        },
        {
          name: "Second opinion",
          description: "A specialist's review of an existing diagnosis or recommendation.",
          modes: ["online"],
        },
      ],
    },
    {
      icon: Activity,
      name: "Diagnostic Services",
      description: "Tests and screenings, including options at home.",
      examples: [
        {
          name: "Lab test",
          description: "A standard diagnostic test, such as a blood or urine panel.",
          modes: ["in-person", "home-visit"],
        },
        {
          name: "Health screening",
          description: "A routine check to get a general picture of your health.",
          modes: ["in-person"],
        },
      ],
    },
    {
      icon: HomeIcon,
      name: "Home Healthcare",
      description: "Care delivered at your doorstep when visiting a clinic isn't easy.",
      examples: [
        {
          name: "Home doctor visit",
          description: "A doctor's consultation at your home.",
          modes: ["home-visit"],
        },
        {
          name: "Home sample collection",
          description: "A diagnostic sample collected at your home for lab testing.",
          modes: ["home-visit"],
        },
      ],
    },
    {
      icon: Users,
      name: "Nursing Services",
      description: "Qualified nursing support for ongoing care needs.",
      examples: [
        {
          name: "Post-care nursing",
          description: "Nursing support after a procedure or hospital stay.",
          modes: ["home-visit"],
        },
        {
          name: "Routine nursing care",
          description: "Ongoing nursing support for a continuing care need.",
          modes: ["home-visit"],
        },
      ],
    },
  ] satisfies ServiceCategoryDetail[],

  howRequesting: {
    heading: "How requesting a service works",
    description: "From browsing to a completed visit, in four steps.",
    steps: [
      {
        title: "Browse services",
        description: "Explore categories and example services in the Patient app.",
      },
      {
        title: "Choose a doctor and slot",
        description: "Pick an approved doctor and a time that works for you.",
      },
      {
        title: "Submit your request",
        description: "Add any notes the doctor should know, then send your request.",
      },
      {
        title: "Track and get notified",
        description: "Follow your request as it's accepted, confirmed and completed.",
      },
    ] satisfies RequestStep[],
  },

  emergencyNotice: {
    title: "If this is a medical emergency",
    body: `Aarogya Sanjeevani does not provide medical advice or emergency response, and requests submitted here are not monitored for urgent situations. If you have a medical emergency, call your local emergency number (${siteContent.emergencyPhoneIndia} in India) or go to the nearest hospital directly.`,
  },

  appCta: {
    heading: "Ready to request a service?",
    description:
      "Patients request services and doctors manage their availability, both from the app.",
    patient: { label: "Patient app", href: siteContent.playStore.patient },
    doctor: { label: "Doctor app", href: siteContent.playStore.doctor },
  },
} as const;

export type ServicesContent = typeof servicesContent;
