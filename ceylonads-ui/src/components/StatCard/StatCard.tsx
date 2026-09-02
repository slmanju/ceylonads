import type { ComponentType } from "react";
import { Link } from "react-router-dom";
import "./StatCard.css";

interface StatCardProps {
  label: string;
  value: number | null;
  loading?: boolean;
  error?: string | null;
  icon: ComponentType<{ "aria-hidden"?: boolean | "true" | "false" }>;
  to?: string;
}

export function StatCard({ label, value, loading, error, icon: Icon, to }: StatCardProps) {
  const content = (
    <>
      <div className="stat-card__icon">
        <Icon aria-hidden="true" />
      </div>
      <div>
        <p className="stat-card__value">{loading ? "…" : error ? "—" : value}</p>
        <p className="stat-card__label">{label}</p>
      </div>
    </>
  );

  if (to) {
    return (
      <Link to={to} className="stat-card stat-card--link">
        {content}
      </Link>
    );
  }

  return <div className="stat-card">{content}</div>;
}
