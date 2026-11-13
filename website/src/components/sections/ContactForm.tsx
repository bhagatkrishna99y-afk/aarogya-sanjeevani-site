import { useState, type FormEvent } from "react";
import { Alert } from "@/components/ui/Alert";
import { Button } from "@/components/ui/Button";
import { FormField } from "@/components/ui/FormField";
import { Input } from "@/components/ui/Input";
import { Textarea } from "@/components/ui/Textarea";
import { contactFormContent as content } from "@/content/contact";
import { submitContactMessage } from "@/lib/contactForm";

type FieldErrors = Partial<Record<"name" | "email" | "message", string>>;
type Status = "idle" | "submitting" | "success" | "error";

const EMAIL_PATTERN = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

function validate(data: { name: string; email: string; message: string }): FieldErrors {
  const errors: FieldErrors = {};
  if (!data.name.trim()) errors.name = content.errors.nameRequired;

  if (!data.email.trim()) errors.email = content.errors.emailRequired;
  else if (!EMAIL_PATTERN.test(data.email.trim())) errors.email = content.errors.emailInvalid;

  if (!data.message.trim()) errors.message = content.errors.messageRequired;
  else if (data.message.trim().length < 10) errors.message = content.errors.messageTooShort;

  return errors;
}

/**
 * A plain, uncontrolled-by-any-backend contact form -- see
 * `src/lib/contactForm.ts` for where the actual submission goes.
 * Validates client-side only (name/email/message all required, email
 * must look like an email, message needs at least 10 characters) --
 * there's no server here to validate again, so this is the only check
 * that ever runs. `FieldError` (rendered via `FormField`) already uses
 * `role="alert"` for each per-field error, and the post-submit
 * success/error `Alert` announces the same way -- neither needs an
 * extra `aria-live` region of its own.
 */
export function ContactForm() {
  const [name, setName] = useState("");
  const [email, setEmail] = useState("");
  const [message, setMessage] = useState("");
  const [errors, setErrors] = useState<FieldErrors>({});
  const [status, setStatus] = useState<Status>("idle");

  async function handleSubmit(event: FormEvent) {
    event.preventDefault();

    const fieldErrors = validate({ name, email, message });
    setErrors(fieldErrors);
    if (Object.keys(fieldErrors).length > 0) return;

    setStatus("submitting");
    try {
      await submitContactMessage({
        name: name.trim(),
        email: email.trim(),
        message: message.trim(),
      });
      setStatus("success");
    } catch {
      setStatus("error");
    }
  }

  if (status === "success") {
    return (
      <Alert variant="success" title={content.successTitle}>
        {content.successBody}
      </Alert>
    );
  }

  return (
    <form onSubmit={handleSubmit} noValidate className="space-y-6">
      <h2 className="text-xl font-semibold text-slate-900">{content.heading}</h2>

      {status === "error" && (
        <Alert variant="error" title={content.errorTitle}>
          {content.errorBody}
        </Alert>
      )}

      <FormField label={content.fields.name.label} required error={errors.name}>
        <Input
          name="name"
          autoComplete="name"
          placeholder={content.fields.name.placeholder}
          value={name}
          onChange={(e) => setName(e.target.value)}
        />
      </FormField>

      <FormField label={content.fields.email.label} required error={errors.email}>
        <Input
          type="email"
          name="email"
          autoComplete="email"
          placeholder={content.fields.email.placeholder}
          value={email}
          onChange={(e) => setEmail(e.target.value)}
        />
      </FormField>

      <FormField label={content.fields.message.label} required error={errors.message}>
        <Textarea
          name="message"
          placeholder={content.fields.message.placeholder}
          value={message}
          onChange={(e) => setMessage(e.target.value)}
        />
      </FormField>

      <Button type="submit" loading={status === "submitting"}>
        {status === "submitting" ? content.submittingLabel : content.submitLabel}
      </Button>
    </form>
  );
}
