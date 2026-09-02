import { Link } from "react-router-dom";
import "./NotFoundPage.css";

export function AccessDeniedPage() {
  return (
    <div className="container not-found-page">
      <h1>403</h1>
      <p>You don't have permission to view this page.</p>
      <Link to="/" className="btn btn-primary">
        Back to homepage
      </Link>
    </div>
  );
}
