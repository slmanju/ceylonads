import { useEffect, useRef, useState } from "react";
import { NavLink, useNavigate } from "react-router-dom";
import { FaChevronDown, FaUserCircle } from "react-icons/fa";
import { useAuth } from "../../auth/AuthContext";
import "./AccountMenu.css";

export function AccountMenu() {
  const { username, role, logout } = useAuth();
  const [open, setOpen] = useState(false);
  const containerRef = useRef<HTMLDivElement>(null);
  const navigate = useNavigate();

  useEffect(() => {
    if (!open) return;

    const handlePointerDown = (event: MouseEvent) => {
      if (containerRef.current && !containerRef.current.contains(event.target as Node)) {
        setOpen(false);
      }
    };
    const handleKeyDown = (event: KeyboardEvent) => {
      if (event.key === "Escape") setOpen(false);
    };

    document.addEventListener("mousedown", handlePointerDown);
    document.addEventListener("keydown", handleKeyDown);
    return () => {
      document.removeEventListener("mousedown", handlePointerDown);
      document.removeEventListener("keydown", handleKeyDown);
    };
  }, [open]);

  const closeMenu = () => setOpen(false);

  const handleLogout = () => {
    closeMenu();
    logout();
    navigate("/");
  };

  return (
    <div className="account-menu" ref={containerRef}>
      <button
        type="button"
        className="account-menu__trigger"
        onClick={() => setOpen((prev) => !prev)}
        aria-haspopup="true"
        aria-expanded={open}
      >
        <FaUserCircle aria-hidden="true" />
        <span>Account</span>
        <FaChevronDown aria-hidden="true" className="account-menu__chevron" />
      </button>

      {open && (
        <div className="account-menu__dropdown" role="menu">
          <div className="account-menu__identity">{username}</div>
          <div className="account-menu__divider" role="separator" />

          {role === "CUSTOMER" && (
            <>
              <NavLink to="/my-ads" className="account-menu__item" role="menuitem" onClick={closeMenu}>
                My Ads
              </NavLink>
              <NavLink to="/my-promotions" className="account-menu__item" role="menuitem" onClick={closeMenu}>
                My Promotions
              </NavLink>
              <NavLink to="/my-payments" className="account-menu__item" role="menuitem" onClick={closeMenu}>
                My Payments
              </NavLink>
            </>
          )}

          {role === "MODERATOR" && (
            <>
              <NavLink to="/my-ads" className="account-menu__item" role="menuitem" onClick={closeMenu}>
                My Ads
              </NavLink>
              <NavLink to="/moderation" className="account-menu__item" role="menuitem" onClick={closeMenu}>
                Moderation
              </NavLink>
            </>
          )}

          <NavLink to="/account/change-password" className="account-menu__item" role="menuitem" onClick={closeMenu}>
            Change Password
          </NavLink>
          <div className="account-menu__divider" role="separator" />

          <button
            type="button"
            className="account-menu__item account-menu__item--logout"
            role="menuitem"
            onClick={handleLogout}
          >
            Logout
          </button>
        </div>
      )}
    </div>
  );
}
