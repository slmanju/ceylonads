import { Link } from "react-router-dom";
import type { BreadcrumbItem } from "../../utils/seo";
import "./Breadcrumbs.css";

interface BreadcrumbsProps {
  items: BreadcrumbItem[];
}

export function Breadcrumbs({ items }: BreadcrumbsProps) {
  if (items.length === 0) return null;

  return (
    <nav className="breadcrumbs" aria-label="Breadcrumb">
      <ol className="breadcrumbs__list">
        {items.map((item, index) => {
          const isLast = index === items.length - 1;
          return (
            <li key={`${item.name}-${index}`} className="breadcrumbs__item">
              {item.path && !isLast ? (
                <Link to={item.path}>{item.name}</Link>
              ) : (
                <span aria-current={isLast ? "page" : undefined}>{item.name}</span>
              )}
              {!isLast && (
                <span className="breadcrumbs__separator" aria-hidden="true">
                  /
                </span>
              )}
            </li>
          );
        })}
      </ol>
    </nav>
  );
}
