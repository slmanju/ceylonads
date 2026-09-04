import { useState, type ChangeEvent, type FormEvent } from "react";
import { Link, useNavigate } from "react-router-dom";
import { useAuth } from "../auth/AuthContext";
import { Seo } from "../components/Seo/Seo";
import { getApiErrorMessage } from "../utils/apiError";
import "./AuthPages.css";

interface FormState {
  username: string;
  password: string;
  email: string;
  displayName: string;
  phone: string;
}

const INITIAL_STATE: FormState = { username: "", password: "", email: "", displayName: "", phone: "" };

function validate(form: FormState): string | null {
  if (form.username.trim().length < 3) return "Username must be at least 3 characters.";
  if (form.password.length < 8) return "Password must be at least 8 characters.";
  if (!/^\S+@\S+\.\S+$/.test(form.email)) return "Please enter a valid email address.";
  if (form.displayName.trim().length === 0) return "Please enter your display name.";
  return null;
}

export function RegisterPage() {
  const { register } = useAuth();
  const navigate = useNavigate();

  const [form, setForm] = useState<FormState>(INITIAL_STATE);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const updateField = (field: keyof FormState) => (event: ChangeEvent<HTMLInputElement>) => {
    setForm((prev) => ({ ...prev, [field]: event.target.value }));
  };

  const handleSubmit = async (event: FormEvent) => {
    event.preventDefault();
    setError(null);

    const validationError = validate(form);
    if (validationError) {
      setError(validationError);
      return;
    }

    setSubmitting(true);
    try {
      await register({
        username: form.username.trim(),
        password: form.password,
        email: form.email.trim(),
        displayName: form.displayName.trim(),
        ...(form.phone.trim() ? { phone: form.phone.trim() } : {}),
      });
      navigate("/", { replace: true });
    } catch (err) {
      setError(getApiErrorMessage(err, "Registration failed. Please check your details and try again."));
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="auth-page container">
      <Seo title="Register" noindex />
      <div className="auth-card">
        <h1 className="auth-card__title">Create your ezClass account</h1>
        <p className="auth-card__subtitle">Create an account to post and manage your classes.</p>

        {error && (
          <p className="auth-card__error" role="alert">
            {error}
          </p>
        )}

        <form onSubmit={handleSubmit} className="auth-form" noValidate>
          <div className="auth-form__field">
            <label htmlFor="register-displayName">Display name</label>
            <input
              id="register-displayName"
              type="text"
              autoComplete="name"
              value={form.displayName}
              onChange={updateField("displayName")}
              maxLength={120}
              required
            />
          </div>

          <div className="auth-form__field">
            <label htmlFor="register-username">Username</label>
            <input
              id="register-username"
              type="text"
              autoComplete="username"
              value={form.username}
              onChange={updateField("username")}
              minLength={3}
              maxLength={80}
              required
            />
          </div>

          <div className="auth-form__field">
            <label htmlFor="register-email">Email</label>
            <input
              id="register-email"
              type="email"
              autoComplete="email"
              value={form.email}
              onChange={updateField("email")}
              maxLength={160}
              required
            />
          </div>

          <div className="auth-form__field">
            <label htmlFor="register-phone">Phone (optional)</label>
            <input
              id="register-phone"
              type="tel"
              autoComplete="tel"
              value={form.phone}
              onChange={updateField("phone")}
              maxLength={30}
            />
          </div>

          <div className="auth-form__field">
            <label htmlFor="register-password">Password</label>
            <input
              id="register-password"
              type="password"
              autoComplete="new-password"
              value={form.password}
              onChange={updateField("password")}
              minLength={8}
              maxLength={100}
              required
            />
          </div>

          <button type="submit" className="btn btn-accent btn-block" disabled={submitting}>
            {submitting ? "Creating account…" : "Create account"}
          </button>
        </form>

        <p className="auth-card__footer">
          Already have an account? <Link to="/login">Login</Link>
        </p>
      </div>
    </div>
  );
}
