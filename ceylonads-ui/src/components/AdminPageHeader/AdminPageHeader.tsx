import type { ReactNode } from "react";
import "./AdminPageHeader.css";

interface AdminPageHeaderProps {
  title: string;
  subtitle?: string;
  action?: ReactNode;
}

export function AdminPageHeader({ title, subtitle, action }: AdminPageHeaderProps) {
  return (
    <div className="admin-page-header">
      <div>
        <h1 className="admin-page-header__title">{title}</h1>
        {subtitle && <p className="admin-page-header__subtitle">{subtitle}</p>}
      </div>
      {action && <div className="admin-page-header__action">{action}</div>}
    </div>
  );
}
