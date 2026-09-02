import type { ReactNode } from "react";
import { Navigate, useLocation } from "react-router-dom";
import { useAuth } from "./AuthContext";
import { AccessDeniedPage } from "../pages/AccessDeniedPage";

interface ProtectedRouteProps {
  children: ReactNode;
}

export function ProtectedRoute({ children }: ProtectedRouteProps) {
  const { isAuthenticated, role } = useAuth();
  const location = useLocation();

  if (!isAuthenticated) {
    return <Navigate to="/login" state={{ from: location }} replace />;
  }

  // The tuition UI only supports the CUSTOMER-facing flows (posting/managing ads); ADMIN and
  // MODERATOR accounts can still log in (shared account system) but have nothing to do here.
  if (role !== "CUSTOMER") {
    return <AccessDeniedPage />;
  }

  return <>{children}</>;
}
