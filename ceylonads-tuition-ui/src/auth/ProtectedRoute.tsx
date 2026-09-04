import type { ReactNode } from "react";
import { Navigate, useLocation } from "react-router-dom";
import { useAuth } from "./AuthContext";

interface ProtectedRouteProps {
  children: ReactNode;
}

export function ProtectedRoute({ children }: ProtectedRouteProps) {
  const { isAuthenticated } = useAuth();
  const location = useLocation();

  if (!isAuthenticated) {
    return <Navigate to="/login" state={{ from: location }} replace />;
  }

  // Self-service pages (account, my classes, post/edit/promote a class) are available to any
  // authenticated account, not just CUSTOMER - an ADMIN (or MODERATOR) is still a normal ezClass
  // user first and must keep those capabilities on top of their admin-only access (see
  // ProtectedAdminRoute for the actual admin-only gate). There is no "customer-only business
  // operation" behind this guard that would justify narrowing it further.
  return <>{children}</>;
}
