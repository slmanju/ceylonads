import { useState } from "react";
import { Link, NavLink, useNavigate } from "react-router-dom";
import { FaBars, FaGraduationCap, FaPlus, FaTimes, FaUserCircle } from "react-icons/fa";
import { useAuth } from "../../auth/AuthContext";
import "./Header.css";

export function Header() {
  const { isAuthenticated, username, logout } = useAuth();
  const [menuOpen, setMenuOpen] = useState(false);
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
    <header className="tuition-header">
      <div className="container tuition-header__bar">
        <Link to="/" className="tuition-header__logo" onClick={closeMenu}>
          <FaGraduationCap aria-hidden="true" />
          <span className="tuition-header__logo-text">
            ez<span className="tuition-header__logo-accent">Class</span>
          </span>
        </Link>

        <nav className={`tuition-header__nav ${menuOpen ? "tuition-header__nav--open" : ""}`}>
          <NavLink to="/classes" className="tuition-header__link" onClick={closeMenu}>
            Search
          </NavLink>
          <NavLink to="/pricing" className="tuition-header__link" onClick={closeMenu}>
            Promote
          </NavLink>
          <div className="tuition-header__actions">
            {isAuthenticated ? (
              <>
                <NavLink to="/my-ads" className="tuition-header__link" onClick={closeMenu}>
                  My Classes
                </NavLink>
                <NavLink to="/account" className="tuition-header__link tuition-header__account" onClick={closeMenu}>
                  <FaUserCircle aria-hidden="true" />
                  {username}
                </NavLink>
                <button type="button" className="tuition-header__link tuition-header__logout" onClick={handleLogout}>
                  Logout
                </button>
              </>
            ) : (
              <>
                <Link to="/login" className="tuition-header__link" onClick={closeMenu}>
                  Login
                </Link>
                <Link to="/register" className="tuition-header__link" onClick={closeMenu}>
                  Register
                </Link>
              </>
            )}

            <button type="button" className="btn btn-accent tuition-header__post-btn" onClick={handlePostAd}>
              <FaPlus aria-hidden="true" />
              Post Free Class
            </button>
          </div>
        </nav>

        <button
          type="button"
          className="tuition-header__menu-toggle"
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
