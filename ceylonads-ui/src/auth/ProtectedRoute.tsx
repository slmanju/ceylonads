import type { ReactNode } from "react";
import { Navigate, useLocation } from "react-router-dom";
import { useAuth } from "./AuthContext";
import { AccessDeniedPage } from "../pages/AccessDeniedPage";
import { Seo } from "../components/Seo/Seo";
import type { Role } from "../types/api";

interface ProtectedRouteProps {
  children: ReactNode;
  requireRole?: Role | Role[];
}

export function ProtectedRoute({ children, requireRole }: ProtectedRouteProps) {
  const { isAuthenticated, role } = useAuth();
  const location = useLocation();

  if (!isAuthenticated) {
    return <Navigate to="/login" state={{ from: location }} replace />;
  }

  const allowedRoles = requireRole === undefined ? undefined : Array.isArray(requireRole) ? requireRole : [requireRole];

  if (allowedRoles && (!role || !allowedRoles.includes(role))) {
    return (
      <>
        <Seo title="Access Denied" noindex />
        <AccessDeniedPage />
      </>
    );
  }

  // Every screen behind a protected route is account-specific or admin-only, so it's kept out
  // of search indexes centrally here rather than one Seo tag per page.
  return (
    <>
      <Seo title="Account" noindex />
      {children}
    </>
  );
}
