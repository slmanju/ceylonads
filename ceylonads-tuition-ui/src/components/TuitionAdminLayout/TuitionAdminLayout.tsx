import { useState } from "react";
import { Link, NavLink, Outlet, useNavigate } from "react-router-dom";
import {
  FaBars,
  FaTimes,
  FaTachometerAlt,
  FaClipboardList,
  FaCommentDots,
  FaSignOutAlt,
  FaStar,
  FaTags,
  FaBullhorn,
} from "react-icons/fa";
import { useAuth } from "../../auth/AuthContext";
import "./TuitionAdminLayout.css";

// Modeled on ceylonads-ui's AdminLayout (sidebar + topbar shell), but this is a wholly separate
// component living in ceylonads-tuition-ui - it must never render or link to MAIN_SITE admin
// pages, and is labeled "ezClass Admin" rather than "CeylonAds Admin".
const NAV_ITEMS = [
  { to: "/admin/tuition", label: "Dashboard", icon: FaTachometerAlt, end: true },
  { to: "/admin/tuition/pending", label: "Classes", icon: FaClipboardList, end: false },
  { to: "/admin/tuition/promotions", label: "Promotions", icon: FaStar, end: false },
  { to: "/admin/tuition/promotion-plans", label: "Promotion Plans", icon: FaTags, end: false },
  { to: "/admin/tuition/campaigns", label: "Promotion Campaigns", icon: FaBullhorn, end: false },
  { to: "/admin/tuition/suggestions", label: "Suggestions", icon: FaCommentDots, end: false },
];

export function TuitionAdminLayout() {
  const { username, logout } = useAuth();
  const navigate = useNavigate();
  const [drawerOpen, setDrawerOpen] = useState(false);

  const closeDrawer = () => setDrawerOpen(false);

  const handleLogout = () => {
    closeDrawer();
    logout();
    navigate("/");
  };

  return (
    <div className="tuition-admin-shell">
      <header className="tuition-admin-topbar">
        <button
          type="button"
          className="tuition-admin-topbar__menu-toggle"
          aria-label={drawerOpen ? "Close menu" : "Open menu"}
          aria-expanded={drawerOpen}
          onClick={() => setDrawerOpen((open) => !open)}
        >
          {drawerOpen ? <FaTimes /> : <FaBars />}
        </button>
        <Link to="/admin/tuition" className="tuition-admin-topbar__title">
          ez<span>Class</span> Admin
        </Link>
        <span className="tuition-admin-topbar__user">{username}</span>
      </header>

      <div className="tuition-admin-body">
        {drawerOpen && (
          <button
            type="button"
            className="tuition-admin-drawer-backdrop"
            aria-label="Close menu"
            onClick={closeDrawer}
          />
        )}

        <aside className={`tuition-admin-sidebar ${drawerOpen ? "tuition-admin-sidebar--open" : ""}`}>
          <nav className="tuition-admin-sidebar__nav">
            {NAV_ITEMS.map(({ to, label, icon: Icon, end }) => (
              <NavLink
                key={to}
                to={to}
                end={end}
                className={({ isActive }) =>
                  `tuition-admin-sidebar__link ${isActive ? "tuition-admin-sidebar__link--active" : ""}`
                }
                onClick={closeDrawer}
              >
                <Icon aria-hidden="true" />
                {label}
              </NavLink>
            ))}
            <button type="button" className="tuition-admin-sidebar__link tuition-admin-sidebar__logout" onClick={handleLogout}>
              <FaSignOutAlt aria-hidden="true" />
              Logout
            </button>
          </nav>
        </aside>

        <main className="tuition-admin-content">
          <Outlet />
        </main>
      </div>
    </div>
  );
}
