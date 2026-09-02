import { Link } from "react-router-dom";
import { Seo } from "../components/Seo/Seo";
import "./NotFoundPage.css";

export function NotFoundPage() {
  return (
    <div className="container not-found-page">
      <Seo title="Page Not Found" noindex />
      <h1>404</h1>
      <p>The page you're looking for doesn't exist.</p>
      <Link to="/" className="btn btn-primary">
        Back to homepage
      </Link>
    </div>
  );
}
