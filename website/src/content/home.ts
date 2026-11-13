/**
 * All copy for the Home page, in one place -- see src/pages/public/HomePage.tsx
 * for how each section renders this. Every sentence here was written to
 * make no medical claim and promise no outcome (never "get better",
 * "best doctors", "guaranteed", a specific compliance/certification
 * name we don't actually hold, etc.) -- if you edit this file, keep that
 * bar.
 */
import {
  Search,
  UserCheck,
  CalendarCheck,
  ClipboardCheck,
  Bell,
  ShieldCheck,
  Stethoscope,
  HeartPulse,
  Activity,
  Home as HomeIcon,
  Users,
  type LucideIcon,
} from "lucide-react";
import { siteContent } from "./site";

export interface BenefitItem {
  icon: LucideIcon;
  title: string;
  description: string;
}

export interface StepItem {
  title: string;
  description: string;
}

export interface ServiceCategoryItem {
  icon: LucideIcon;
  name: string;
  description: string;
}

export interface FaqItem {
  question: string;
  answer: string;
}

export const homeContent = {
  hero: {
    headline: "Find trusted healthcare, without the guesswork.",
    subheading:
      "Aarogya Sanjeevani helps you discover approved doctors and services near you, and keep track of every request from one place.",
    primaryCta: { label: "Get the Patient App", href: siteContent.playStore.patient },
    secondaryCta: { label: "For Doctors", href: siteContent.playStore.doctor },
    trustLine: "Approved doctors. Clear requests. Secure accounts.",
  },

  benefits: [
    {
      icon: Search,
      title: "Discover services",
      description:
        "Browse consultations, home visits, diagnostics and more, organized by category.",
    },
    {
      icon: UserCheck,
      title: "Find approved doctors",
      description:
        "Every doctor listed has been reviewed by our team before they can accept requests.",
    },
    {
      icon: CalendarCheck,
      title: "See real availability",
      description: "Pick a time that's actually open, based on the doctor's own schedule.",
    },
    {
      icon: ClipboardCheck,
      title: "Request in a few taps",
      description: "Tell the doctor what you need and send your request right from the app.",
    },
    {
      icon: Bell,
      title: "Track every update",
      description: "Get notified the moment your request is accepted, confirmed or completed.",
    },
    {
      icon: ShieldCheck,
      title: "Secure by design",
      description: "Your account and information are protected at every step.",
    },
  ] satisfies BenefitItem[],

  howItWorks: {
    heading: "How it works",
    description: "From browsing to a completed visit, in five simple steps.",
    patientSteps: [
      {
        title: "Create your account",
        description: "Sign up in the Patient app in a couple of minutes.",
      },
      {
        title: "Browse services",
        description: "Explore the kinds of care available, from consultations to home visits.",
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
    ] satisfies StepItem[],
    doctorStrip: {
      heading: "Are you a doctor?",
      description: "Offer your services through the Doctor app, in four steps.",
      steps: [
        { title: "Join", description: "Register through the Doctor app." },
        { title: "Get approved", description: "Our team reviews your profile before you go live." },
        {
          title: "Set availability",
          description: "Choose the days and times you're open for requests.",
        },
        {
          title: "Manage requests",
          description: "Accept, confirm and complete requests as you go.",
        },
      ] satisfies StepItem[],
    },
  },

  services: {
    heading: "Main services",
    description: "A look at what's available -- browse the full catalog for specifics near you.",
    categories: [
      {
        icon: Stethoscope,
        name: "General Healthcare",
        description: "Everyday consultations for common health concerns.",
      },
      {
        icon: HeartPulse,
        name: "Specialist Consultation",
        description: "Connect with doctors focused on a particular area of care.",
      },
      {
        icon: Activity,
        name: "Diagnostic Services",
        description: "Tests and screenings, including options at home.",
      },
      {
        icon: HomeIcon,
        name: "Home Healthcare",
        description: "Care delivered at your doorstep when visiting a clinic isn't easy.",
      },
      {
        icon: Users,
        name: "Nursing Services",
        description: "Qualified nursing support for ongoing care needs.",
      },
    ] satisfies ServiceCategoryItem[],
    cta: { label: "Browse all services", to: "/services" },
  },

  trust: {
    heading: "Trust and safety",
    description: "A few plain-language notes on how we handle approval, privacy and security.",
    points: [
      {
        title: "Doctors are reviewed before they're listed",
        description:
          "Every doctor submits their qualifications and license details, and our team checks them before that doctor can accept any requests. Only approved doctors appear to patients.",
      },
      {
        title: "Your data is handled privacy-first",
        description:
          "We only ask for what's needed to connect you with the right doctor and handle your request. We don't sell your information or share it with advertisers.",
      },
      {
        title: "Accounts are kept secure",
        description:
          "Signing in is protected, and access to your information is limited to what's needed to support your requests.",
      },
    ],
  },

  app: {
    heading: "Get the app",
    description: "Booking and managing requests happens in our mobile apps.",
    patient: { label: "Patient app", href: siteContent.playStore.patient },
    doctor: { label: "Doctor app", href: siteContent.playStore.doctor },
  },

  faq: [
    {
      question: "What is Aarogya Sanjeevani?",
      answer:
        "A place to discover approved doctors and healthcare services, and keep track of your requests from start to finish -- an easier way to find the right care and stay updated, without the back-and-forth.",
    },
    {
      question: "Is it free?",
      answer:
        "Yes -- browsing, creating an account, and submitting a request are all free. This version of Aarogya Sanjeevani doesn't process payments, so any arrangement for a specific consultation or service is between you and the doctor or provider directly.",
    },
    {
      question: "Do you handle medical emergencies?",
      answer: `No. Aarogya Sanjeevani is not an emergency service. If you have a medical emergency, call your local emergency number (${siteContent.emergencyPhoneIndia} in India) or go to the nearest hospital directly -- please don't wait on a request here.`,
    },
    {
      question: "How are doctors verified?",
      answer:
        "Every doctor creates a profile with their qualifications and license details, and our team reviews it before they're approved. If their details change later, that review happens again.",
    },
    {
      question: "How is my data protected?",
      answer:
        "Your account is protected by secure sign-in, and access to your information is limited to what's needed to handle your requests. We don't sell or share your data with advertisers.",
    },
    {
      question: "How do I cancel a request?",
      answer:
        "Open the request in the Patient app and choose to cancel it, for as long as it hasn't already been completed. The doctor is notified automatically.",
    },
    {
      question: "How do doctors join?",
      answer:
        "Doctors register through the Doctor app and submit their profile for review. Once our team approves it, the doctor can set their availability and start receiving requests.",
    },
  ] satisfies FaqItem[],

  finalCta: {
    heading: "Ready to get started?",
    description: "Download the Patient app to discover services and doctors near you.",
    cta: { label: "Get the Patient App", href: siteContent.playStore.patient },
  },
} as const;

export type HomeContent = typeof homeContent;
