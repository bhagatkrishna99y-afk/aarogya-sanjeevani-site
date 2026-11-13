/**
 * All copy for the About page -- see src/pages/public/AboutPage.tsx for how
 * each section renders this. Same bar as home.ts: no medical claims, no
 * promised outcomes, and nothing that implies a certification or
 * compliance badge we don't actually hold.
 */
import {
  Shield,
  Eye,
  Lock,
  Sparkles,
  HandHeart,
  Accessibility,
  Search,
  ClipboardList,
  UserCog,
  ShieldCheck,
  type LucideIcon,
} from "lucide-react";
import { siteContent } from "./site";

export interface ValueItem {
  icon: LucideIcon;
  title: string;
  description: string;
}

export interface ApproachItem {
  icon: LucideIcon;
  title: string;
  description: string;
}

export interface SafetyPoint {
  title: string;
  description: string;
}

export const aboutContent = {
  intro: {
    heading: "About Aarogya Sanjeevani",
    paragraphs: [
      "Aarogya Sanjeevani exists to make finding healthcare simpler -- a place where patients can discover approved doctors and services, and doctors can offer their care, without the usual back-and-forth.",
      "We built it because looking for the right doctor or service often means scattered phone numbers, uncertain availability and no easy way to track what happens next. Aarogya Sanjeevani brings discovery, requests and updates into one place.",
    ],
  },

  missionVision: {
    mission: {
      heading: "Our mission",
      body: "To make discovering and requesting healthcare services straightforward for patients, and to give doctors a simple, dependable way to offer their care.",
    },
    vision: {
      heading: "Our vision",
      body: "A place where anyone can find an approved doctor or service with confidence, and every request is clear, trackable and respectful of their privacy.",
    },
  },

  approach: {
    heading: "Our healthcare approach",
    description: "A few principles guide how Aarogya Sanjeevani is built.",
    items: [
      {
        icon: Search,
        title: "Simple discovery",
        description:
          "Services and doctors are organized clearly, so finding the right one doesn't take guesswork.",
      },
      {
        icon: ClipboardList,
        title: "Transparent requests",
        description:
          "Every request shows its real status -- submitted, accepted, confirmed or completed -- with no hidden steps.",
      },
      {
        icon: UserCog,
        title: "Doctors control their schedule",
        description:
          "Each doctor sets their own availability and decides which requests to accept.",
      },
      {
        icon: Lock,
        title: "Patient privacy",
        description:
          "We only collect what's needed to connect a patient with the right doctor and handle their request.",
      },
    ] satisfies ApproachItem[],
  },

  values: {
    heading: "What we stand for",
    description: "The values behind how Aarogya Sanjeevani is designed and run.",
    items: [
      {
        icon: Shield,
        title: "Trust",
        description: "Every doctor on the platform is reviewed before they can accept a request.",
      },
      {
        icon: Eye,
        title: "Transparency",
        description: "Patients and doctors can always see where a request stands.",
      },
      {
        icon: ShieldCheck,
        title: "Safety",
        description:
          "Accounts and access are protected, and approval checks happen before anyone goes live.",
      },
      {
        icon: Sparkles,
        title: "Simplicity",
        description: "We'd rather do fewer things clearly than overwhelm you with options.",
      },
      {
        icon: HandHeart,
        title: "Respect",
        description: "For patients' time and privacy, and for doctors' schedules and judgment.",
      },
      {
        icon: Accessibility,
        title: "Accessibility",
        description:
          "Built to work for as many people as possible, on ordinary devices and connections.",
      },
    ] satisfies ValueItem[],
  },

  safety: {
    heading: "How we keep the platform safe",
    description: "A few plain-language notes on what happens behind the scenes.",
    points: [
      {
        title: "Doctor approval",
        description:
          "Every doctor submits their qualifications and license details, and our team reviews them before that doctor can accept any requests.",
      },
      {
        title: "Access controls",
        description:
          "Access to patient and request information is limited to what's needed to support that request -- not open by default.",
      },
      {
        title: "Data minimization",
        description:
          "We only ask for the information needed to connect a patient with the right doctor and handle their request, and nothing more.",
      },
    ] satisfies SafetyPoint[],
  },

  closingCta: {
    heading: "See it for yourself",
    description:
      "Download the Patient app to discover services and doctors, or get in touch if you have questions.",
    cta: { label: "Get the Patient App", href: siteContent.playStore.patient },
    secondaryCta: { label: "Contact us", to: "/contact" },
  },
} as const;

export type AboutContent = typeof aboutContent;
