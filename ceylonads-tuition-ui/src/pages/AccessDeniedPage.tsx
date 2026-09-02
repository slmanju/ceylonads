import { Link } from "react-router-dom";
import "./StatusPages.css";

export function AccessDeniedPage() {
  return (
    <div className="container status-page">
      <h1>Access Denied</h1>
      <p>Your account doesn't have access to this page.</p>
      <Link to="/" className="btn btn-primary">
        Back to homepage
      </Link>
    </div>
  );
}
