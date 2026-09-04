import type { ReactNode } from "react";
import { Navigate, useLocation } from "react-router-dom";
import { useAuth } from "./AuthContext";
import { AccessDeniedPage } from "../pages/AccessDeniedPage";

interface ProtectedAdminRouteProps {
  children: ReactNode;
}

// Gates /admin/tuition/** specifically - ordinary self-service pages (account, my classes, post a
// class) use the isAuthenticated-only ProtectedRoute instead, since ADMIN keeps normal user
// capability on top of admin access. Only ADMIN gets Tuition admin access for now (see CLAUDE.md's
// "Roles and authentication") - CUSTOMER/MODERATOR are denied here even though they pass
// ProtectedRoute everywhere else.
export function ProtectedAdminRoute({ children }: ProtectedAdminRouteProps) {
  const { isAuthenticated, role } = useAuth();
  const location = useLocation();

  if (!isAuthenticated) {
    return <Navigate to="/login" state={{ from: location }} replace />;
  }

  if (role !== "ADMIN") {
    return <AccessDeniedPage />;
  }

  return <>{children}</>;
}
