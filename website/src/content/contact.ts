/**
 * Copy for the contact form on /contact -- see
 * src/components/sections/ContactForm.tsx. Field labels, validation
 * messages, and the post-submit states.
 */
export const contactFormContent = {
  heading: "Send us a message",
  fields: {
    name: { label: "Your name", placeholder: "Jane Doe" },
    email: { label: "Email address", placeholder: "you@example.com" },
    message: { label: "Message", placeholder: "How can we help?" },
  },
  errors: {
    nameRequired: "Please enter your name.",
    emailRequired: "Please enter your email address.",
    emailInvalid: "Please enter a valid email address.",
    messageRequired: "Please enter a message.",
    messageTooShort: "Please enter at least 10 characters.",
  },
  submitLabel: "Send message",
  submittingLabel: "Sending…",
  successTitle: "Message sent",
  successBody: "Thanks for reaching out -- we'll get back to you soon.",
  errorTitle: "Something went wrong",
  errorBody: "We couldn't send your message. Please try again in a moment.",
} as const;
