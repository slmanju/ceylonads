import { Link } from "react-router-dom";
import { FaUserCircle } from "react-icons/fa";
import { useAuth } from "../auth/AuthContext";
import { Seo } from "../components/Seo/Seo";
import "./AccountPage.css";

export function AccountPage() {
  const { username, role, logout } = useAuth();

  return (
    <div className="account-page container">
      <Seo title="My Account" noindex />
      <div className="account-page__card">
        <FaUserCircle aria-hidden="true" className="account-page__icon" />
        <h1 className="account-page__name">{username}</h1>
        <p className="account-page__role">{role}</p>

        <div className="account-page__links">
          <Link to="/my-ads" className="btn btn-secondary btn-block">
            My Classes
          </Link>
          <Link to="/post-ad" className="btn btn-primary btn-block">
            Post a Class
          </Link>
          <button type="button" className="btn btn-outline btn-block" onClick={logout}>
            Logout
          </button>
        </div>
      </div>
    </div>
  );
}
