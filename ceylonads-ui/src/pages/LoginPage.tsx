import { useState, type FormEvent } from "react";
import { Link, useLocation, useNavigate } from "react-router-dom";
import { useAuth } from "../auth/AuthContext";
import { Seo } from "../components/Seo/Seo";
import { getApiErrorMessage } from "../utils/apiError";
import "./AuthPages.css";

const DEMO_ACCOUNTS = [
  { role: "ADMIN", username: "admin", password: "admin123" },
  { role: "MODERATOR", username: "moderator1", password: "moderator123" },
  { role: "CUSTOMER", username: "kamal", password: "customer123" },
  { role: "CUSTOMER", username: "nimal", password: "customer123" },
];

export function LoginPage() {
  const { login } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();

  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [demoOpen, setDemoOpen] = useState(false);

  const state = location.state as { from?: { pathname?: string } } | null;
  const redirectTo = state?.from?.pathname ?? "/";

  const handleSubmit = async (event: FormEvent) => {
    event.preventDefault();
    setError(null);

    if (!username.trim() || !password) {
      setError("Please enter your username and password.");
      return;
    }

    setSubmitting(true);
    try {
      await login({ username: username.trim(), password });
      navigate(redirectTo, { replace: true });
    } catch (err) {
      setError(getApiErrorMessage(err, "Login failed. Please check your credentials."));
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="auth-page container">
      <Seo title="Login" noindex />
      <div className="auth-card">
        <h1 className="auth-card__title">Login to CeylonAds</h1>
        <p className="auth-card__subtitle">Access your account to manage your ads.</p>

        {error && (
          <p className="auth-card__error" role="alert">
            {error}
          </p>
        )}

        <form onSubmit={handleSubmit} className="auth-form" noValidate>
          <div className="auth-form__field">
            <label htmlFor="login-username">Username</label>
            <input
              id="login-username"
              type="text"
              autoComplete="username"
              value={username}
              onChange={(e) => setUsername(e.target.value)}
              required
            />
          </div>

          <div className="auth-form__field">
            <label htmlFor="login-password">Password</label>
            <input
              id="login-password"
              type="password"
              autoComplete="current-password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              required
            />
          </div>

          <button type="submit" className="btn btn-primary btn-block" disabled={submitting}>
            {submitting ? "Logging in…" : "Login"}
          </button>
        </form>

        <p className="auth-card__footer">
          Don't have an account? <Link to="/register">Create an account</Link>
        </p>

        <div className="auth-card__demo">
          <button type="button" className="auth-card__demo-toggle" onClick={() => setDemoOpen((v) => !v)}>
            {demoOpen ? "Hide" : "Show"} demo accounts
          </button>
          {demoOpen && (
            <ul className="auth-card__demo-list">
              {DEMO_ACCOUNTS.map((account) => (
                <li key={account.username}>
                  <strong>{account.role}</strong> — {account.username} / {account.password}
                </li>
              ))}
            </ul>
          )}
        </div>
      </div>
    </div>
  );
}
