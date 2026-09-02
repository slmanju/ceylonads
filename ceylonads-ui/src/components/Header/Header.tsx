import { useState } from "react";
import { Link, NavLink, useNavigate } from "react-router-dom";
import { FaBars, FaTimes, FaPlus } from "react-icons/fa";
import { useAuth } from "../../auth/AuthContext";
import { useMediaQuery } from "../../hooks/useMediaQuery";
import { AccountMenu } from "./AccountMenu";
import "./Header.css";

const MOBILE_QUERY = "(max-width: 860px)";

export function Header() {
  const { isAuthenticated, role, username, logout } = useAuth();
  const [menuOpen, setMenuOpen] = useState(false);
  const isMobile = useMediaQuery(MOBILE_QUERY);
  const navigate = useNavigate();

  const closeMenu = () => setMenuOpen(false);

  const handlePostAd = () => {
    closeMenu();
    navigate(isAuthenticated ? "/post-ad" : "/login");
  };

  const handleLogout = () => {
    closeMenu();
    logout();
    navigate("/");
  };

  return (
    <header className="header">
      <div className="container header__bar">
        <Link to="/" className="header__logo" onClick={closeMenu}>
          Ceylon<span>Ads</span>
        </Link>

        <nav className={`header__nav ${menuOpen ? "header__nav--open" : ""}`}>
          <NavLink to="/ads" className="header__link" onClick={closeMenu}>
            Browse
          </NavLink>

          {isAuthenticated && role === "CUSTOMER" && (
            <>
              <NavLink to="/my-ads" className="header__link" onClick={closeMenu}>
                My Ads
              </NavLink>
              <NavLink to="/my-promotions" className="header__link" onClick={closeMenu}>
                My Promotions
              </NavLink>
            </>
          )}

          {isAuthenticated && role === "MODERATOR" && (
            <>
              <NavLink to="/my-ads" className="header__link" onClick={closeMenu}>
                My Ads
              </NavLink>
              <NavLink to="/moderation" className="header__link" onClick={closeMenu}>
                Moderation
              </NavLink>
            </>
          )}

          {isAuthenticated && role === "ADMIN" && (
            <NavLink to="/admin" className="header__link" onClick={closeMenu}>
              Admin
            </NavLink>
          )}

          <div className="header__nav-actions">
            {!isAuthenticated && (
              <>
                <Link to="/login" className="header__link" onClick={closeMenu}>
                  Login
                </Link>
                <Link to="/register" className="header__link" onClick={closeMenu}>
                  Register
                </Link>
              </>
            )}

            {isAuthenticated && !isMobile && <AccountMenu />}

            {isAuthenticated && isMobile && (
              <div className="header__account-mobile">
                {role === "CUSTOMER" && (
                  <NavLink to="/my-payments" className="header__link" onClick={closeMenu}>
                    My Payments
                  </NavLink>
                )}
                <NavLink to="/account/change-password" className="header__link" onClick={closeMenu}>
                  Change Password
                </NavLink>
                <span className="header__username">{username}</span>
                <button type="button" className="header__link header__logout" onClick={handleLogout}>
                  Logout
                </button>
              </div>
            )}

            {role !== "ADMIN" && (
              <button type="button" className="btn btn-primary header__post-btn" onClick={handlePostAd}>
                <FaPlus aria-hidden="true" />
                Post Free Ad
              </button>
            )}
          </div>
        </nav>

        <button
          type="button"
          className="header__menu-toggle"
          aria-label={menuOpen ? "Close menu" : "Open menu"}
          aria-expanded={menuOpen}
          onClick={() => setMenuOpen((open) => !open)}
        >
          {menuOpen ? <FaTimes /> : <FaBars />}
        </button>
      </div>
    </header>
  );
}
