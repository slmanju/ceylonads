import { useState, type FormEvent } from "react";
import { Seo } from "../components/Seo/Seo";
import { createSuggestion } from "../api/suggestionApi";
import { getApiErrorMessage } from "../utils/apiError";
import "./SuggestPage.css";

const MESSAGE_MAX_LENGTH = 2000;
const NAME_MAX_LENGTH = 120;
const EMAIL_MAX_LENGTH = 180;
// Mirrors the backend's Phones.SRI_LANKAN_PHONE_PATTERN (local "0XXXXXXXXX" or "+94XXXXXXXXX").
const PHONE_PATTERN = /^(0[1-9][0-9]{8}|\+94[1-9][0-9]{8})$/;
const EMAIL_PATTERN = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

export function SuggestPage() {
  const [name, setName] = useState("");
  const [email, setEmail] = useState("");
  const [phone, setPhone] = useState("");
  const [message, setMessage] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [submitted, setSubmitted] = useState(false);

  const validate = (): string | null => {
    if (!message.trim()) return "Please enter your suggestion or feedback.";
    if (message.trim().length > MESSAGE_MAX_LENGTH) return `Suggestion must be ${MESSAGE_MAX_LENGTH} characters or fewer.`;
    if (email.trim() && !EMAIL_PATTERN.test(email.trim())) return "Please enter a valid email address.";
    if (phone.trim() && !PHONE_PATTERN.test(phone.trim())) {
      return "Please enter a valid Sri Lankan phone number, e.g. 0712345678 or +94712345678.";
    }
    return null;
  };

  const handleSubmit = async (event: FormEvent) => {
    event.preventDefault();
    const validationError = validate();
    if (validationError) {
      setError(validationError);
      return;
    }

    setError(null);
    setSubmitting(true);
    try {
      await createSuggestion({
        name: name.trim() || undefined,
        email: email.trim() || undefined,
        phone: phone.trim() || undefined,
        message: message.trim(),
      });
      setSubmitted(true);
    } catch (err) {
      setError(getApiErrorMessage(err, "Could not submit your suggestion. Please try again."));
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="suggest-page container">
      <Seo title="Suggest a Class or Feature" noindex />
      <div className="suggest-card">
        {submitted ? (
          <div className="suggest-card__success" role="status">
            <h1>Thank you for your suggestion.</h1>
            <p>We read every message and use it to improve ezClass.</p>
          </div>
        ) : (
          <>
            <h1 className="suggest-card__title">Suggest a Class or Feature</h1>
            <p className="suggest-card__subtitle">
              Tell us what's missing, what's broken, or what you'd like to see on ezClass.
            </p>

            {error && (
              <p className="suggest-card__error" role="alert">
                {error}
              </p>
            )}

            <form onSubmit={handleSubmit} className="suggest-form" noValidate>
              <div className="suggest-form__field">
                <label htmlFor="suggest-name">Name (optional)</label>
                <input
                  id="suggest-name"
                  type="text"
                  maxLength={NAME_MAX_LENGTH}
                  value={name}
                  onChange={(e) => setName(e.target.value)}
                />
              </div>

              <div className="suggest-form__field">
                <label htmlFor="suggest-email">Email (optional)</label>
                <input
                  id="suggest-email"
                  type="email"
                  maxLength={EMAIL_MAX_LENGTH}
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                />
              </div>

              <div className="suggest-form__field">
                <label htmlFor="suggest-phone">Phone (optional)</label>
                <input
                  id="suggest-phone"
                  type="tel"
                  placeholder="0712345678"
                  value={phone}
                  onChange={(e) => setPhone(e.target.value)}
                />
              </div>

              <div className="suggest-form__field">
                <label htmlFor="suggest-message">Your suggestion or feedback</label>
                <textarea
                  id="suggest-message"
                  rows={6}
                  maxLength={MESSAGE_MAX_LENGTH}
                  value={message}
                  onChange={(e) => setMessage(e.target.value)}
                  required
                />
              </div>

              <button type="submit" className="btn btn-accent btn-block" disabled={submitting}>
                {submitting ? "Submitting…" : "Submit Suggestion"}
              </button>
            </form>
          </>
        )}
      </div>
    </div>
  );
}
