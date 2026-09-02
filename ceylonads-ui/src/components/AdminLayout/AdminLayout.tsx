import { useState } from "react";
import { Link, NavLink, Outlet, useNavigate } from "react-router-dom";
import {
  FaBars,
  FaTimes,
  FaTachometerAlt,
  FaBullhorn,
  FaUsers,
  FaSitemap,
  FaMapMarkedAlt,
  FaSignOutAlt,
  FaTags,
  FaStar,
  FaMoneyCheckAlt,
  FaLayerGroup,
  FaKey,
} from "react-icons/fa";
import { useAuth } from "../../auth/AuthContext";
import "./AdminLayout.css";

const NAV_ITEMS = [
  { to: "/admin", label: "Dashboard", icon: FaTachometerAlt, end: true },
  { to: "/admin/ads", label: "Ads", icon: FaBullhorn, end: false },
  { to: "/admin/customers", label: "Customers", icon: FaUsers, end: false },
  { to: "/admin/categories", label: "Categories", icon: FaSitemap, end: false },
  { to: "/admin/locations", label: "Locations", icon: FaMapMarkedAlt, end: false },
  { to: "/admin/promotion-slots", label: "Promotion Slots", icon: FaLayerGroup, end: false },
  { to: "/admin/promotion-plans", label: "Promotion Plans", icon: FaTags, end: false },
  { to: "/admin/promotions", label: "Promotions", icon: FaStar, end: false },
  { to: "/admin/payments", label: "Payments", icon: FaMoneyCheckAlt, end: false },
];

export function AdminLayout() {
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
    <div className="admin-shell">
      <header className="admin-topbar">
        <button
          type="button"
          className="admin-topbar__menu-toggle"
          aria-label={drawerOpen ? "Close menu" : "Open menu"}
          aria-expanded={drawerOpen}
          onClick={() => setDrawerOpen((open) => !open)}
        >
          {drawerOpen ? <FaTimes /> : <FaBars />}
        </button>
        <Link to="/admin" className="admin-topbar__title">
          CeylonAds <span>Admin</span>
        </Link>
        <span className="admin-topbar__user">{username}</span>
      </header>

      <div className="admin-body">
        {drawerOpen && <button type="button" className="admin-drawer-backdrop" aria-label="Close menu" onClick={closeDrawer} />}

        <aside className={`admin-sidebar ${drawerOpen ? "admin-sidebar--open" : ""}`}>
          <nav className="admin-sidebar__nav">
            {NAV_ITEMS.map(({ to, label, icon: Icon, end }) => (
              <NavLink
                key={to}
                to={to}
                end={end}
                className={({ isActive }) => `admin-sidebar__link ${isActive ? "admin-sidebar__link--active" : ""}`}
                onClick={closeDrawer}
              >
                <Icon aria-hidden="true" />
                {label}
              </NavLink>
            ))}
          </nav>
          <NavLink
            to="/account/change-password"
            className={({ isActive }) => `admin-sidebar__link ${isActive ? "admin-sidebar__link--active" : ""}`}
            onClick={closeDrawer}
          >
            <FaKey aria-hidden="true" />
            Change Password
          </NavLink>
          <button type="button" className="admin-sidebar__logout" onClick={handleLogout}>
            <FaSignOutAlt aria-hidden="true" />
            Logout
          </button>
        </aside>

        <main className="admin-content">
          <Outlet />
        </main>
      </div>
    </div>
  );
}
