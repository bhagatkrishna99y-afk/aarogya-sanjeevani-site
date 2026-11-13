import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { checkA11y } from "@/test/axe";
import { ContactForm } from "./ContactForm";

describe("ContactForm", () => {
  beforeEach(() => {
    vi.stubGlobal("fetch", vi.fn());
  });

  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it("shows a validation error per field when submitted empty, and never calls fetch", async () => {
    const user = userEvent.setup();
    render(<ContactForm />);

    await user.click(screen.getByRole("button", { name: "Send message" }));

    expect(await screen.findByText("Please enter your name.")).toBeInTheDocument();
    expect(screen.getByText("Please enter your email address.")).toBeInTheDocument();
    expect(screen.getByText("Please enter a message.")).toBeInTheDocument();
    expect(fetch).not.toHaveBeenCalled();
  });

  it("flags an invalid email and a too-short message without touching valid fields", async () => {
    const user = userEvent.setup();
    render(<ContactForm />);

    await user.type(screen.getByLabelText(/^Your name/), "Jane Doe");
    await user.type(screen.getByLabelText(/^Email address/), "not-an-email");
    await user.type(screen.getByLabelText(/^Message/), "too short");
    await user.click(screen.getByRole("button", { name: "Send message" }));

    expect(await screen.findByText("Please enter a valid email address.")).toBeInTheDocument();
    expect(screen.getByText("Please enter at least 10 characters.")).toBeInTheDocument();
    expect(screen.queryByText("Please enter your name.")).not.toBeInTheDocument();
  });

  it("submits the trimmed values and shows a success state when the request succeeds", async () => {
    vi.mocked(fetch).mockResolvedValueOnce(new Response(null, { status: 200 }));
    const user = userEvent.setup();
    render(<ContactForm />);

    await user.type(screen.getByLabelText(/^Your name/), "  Jane Doe  ");
    await user.type(screen.getByLabelText(/^Email address/), "jane@example.com");
    await user.type(screen.getByLabelText(/^Message/), "I have a question about services.");
    await user.click(screen.getByRole("button", { name: "Send message" }));

    expect(await screen.findByText("Message sent")).toBeInTheDocument();
    expect(fetch).toHaveBeenCalledTimes(1);
    const [, init] = vi.mocked(fetch).mock.calls[0];
    expect(JSON.parse(init!.body as string)).toEqual({
      name: "Jane Doe",
      email: "jane@example.com",
      message: "I have a question about services.",
    });
  });

  it("shows an error state and keeps the form's values when the request fails", async () => {
    vi.mocked(fetch).mockResolvedValueOnce(new Response(null, { status: 500 }));
    const user = userEvent.setup();
    render(<ContactForm />);

    await user.type(screen.getByLabelText(/^Your name/), "Jane Doe");
    await user.type(screen.getByLabelText(/^Email address/), "jane@example.com");
    await user.type(screen.getByLabelText(/^Message/), "I have a question about services.");
    await user.click(screen.getByRole("button", { name: "Send message" }));

    await waitFor(() => expect(screen.getByText("Something went wrong")).toBeInTheDocument());
    expect(screen.getByLabelText(/^Your name/)).toHaveValue("Jane Doe");
  });

  it("has no detectable accessibility violations", async () => {
    const { container } = render(<ContactForm />);
    const violations = await checkA11y(container);
    expect(violations).toHaveLength(0);
  });
});
