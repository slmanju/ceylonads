import { useState, type FormEvent } from "react";
import { Seo } from "../components/Seo/Seo";
import { getApiErrorMessage } from "../utils/apiError";
import * as accountApi from "../api/accountApi";
import "./AuthPages.css";

interface FormState {
  currentPassword: string;
  newPassword: string;
  confirmPassword: string;
}

const INITIAL_STATE: FormState = { currentPassword: "", newPassword: "", confirmPassword: "" };

function validate(form: FormState): string | null {
  if (!form.currentPassword) return "Please enter your current password.";
  if (form.newPassword.length < 8) return "New password must be at least 8 characters.";
  if (form.newPassword !== form.confirmPassword) return "New password and confirmation do not match.";
  if (form.newPassword === form.currentPassword) return "New password must be different from your current password.";
  return null;
}

export function ChangePasswordPage() {
  const [form, setForm] = useState<FormState>(INITIAL_STATE);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState(false);

  const updateField = (field: keyof FormState) => (event: React.ChangeEvent<HTMLInputElement>) => {
    setForm((prev) => ({ ...prev, [field]: event.target.value }));
    setSuccess(false);
  };

  const handleSubmit = async (event: FormEvent) => {
    event.preventDefault();
    setError(null);
    setSuccess(false);

    const validationError = validate(form);
    if (validationError) {
      setError(validationError);
      return;
    }

    setSubmitting(true);
    try {
      await accountApi.changePassword(form);
      setSuccess(true);
      setForm(INITIAL_STATE);
    } catch (err) {
      setError(getApiErrorMessage(err, "Could not change your password. Please try again."));
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="auth-page container">
      <Seo title="Change Password" noindex />
      <div className="auth-card">
        <h1 className="auth-card__title">Change Password</h1>
        <p className="auth-card__subtitle">Update the password for your CeylonAds account.</p>

        {error && (
          <p className="auth-card__error" role="alert">
            {error}
          </p>
        )}

        {success && (
          <p className="auth-card__success" role="status">
            Password changed successfully.
          </p>
        )}

        <form onSubmit={handleSubmit} className="auth-form" noValidate>
          <div className="auth-form__field">
            <label htmlFor="change-password-current">Current password</label>
            <input
              id="change-password-current"
              type="password"
              autoComplete="current-password"
              value={form.currentPassword}
              onChange={updateField("currentPassword")}
              required
            />
          </div>

          <div className="auth-form__field">
            <label htmlFor="change-password-new">New password</label>
            <input
              id="change-password-new"
              type="password"
              autoComplete="new-password"
              value={form.newPassword}
              onChange={updateField("newPassword")}
              minLength={8}
              maxLength={100}
              required
            />
          </div>

          <div className="auth-form__field">
            <label htmlFor="change-password-confirm">Confirm new password</label>
            <input
              id="change-password-confirm"
              type="password"
              autoComplete="new-password"
              value={form.confirmPassword}
              onChange={updateField("confirmPassword")}
              minLength={8}
              maxLength={100}
              required
            />
          </div>

          <button type="submit" className="btn btn-primary btn-block" disabled={submitting}>
            {submitting ? "Changing password…" : "Change password"}
          </button>
        </form>
      </div>
    </div>
  );
}
