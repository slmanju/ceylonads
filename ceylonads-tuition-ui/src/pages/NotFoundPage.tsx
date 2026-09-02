import { Link } from "react-router-dom";
import { Seo } from "../components/Seo/Seo";
import "./StatusPages.css";

export function NotFoundPage() {
  return (
    <div className="container status-page">
      <Seo title="Page Not Found" noindex />
      <h1>404</h1>
      <p>The page you're looking for doesn't exist.</p>
      <Link to="/" className="btn btn-primary">
        Back to homepage
      </Link>
    </div>
  );
}
