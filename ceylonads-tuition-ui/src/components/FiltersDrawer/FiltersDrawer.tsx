import type { ReactNode } from "react";
import { FaTimes } from "react-icons/fa";
import "./FiltersDrawer.css";

interface FiltersDrawerProps {
  open: boolean;
  onClose: () => void;
  children: ReactNode;
  footer?: ReactNode;
  title?: string;
}

export function FiltersDrawer({ open, onClose, children, footer, title = "Filters" }: FiltersDrawerProps) {
  if (!open) return null;

  return (
    <div className="filters-drawer" role="dialog" aria-modal="true" aria-label={title}>
      <button type="button" className="filters-drawer__backdrop" aria-label={`Close ${title.toLowerCase()}`} onClick={onClose} />
      <div className="filters-drawer__panel">
        <div className="filters-drawer__header">
          <h2>{title}</h2>
          <button type="button" className="filters-drawer__close" onClick={onClose} aria-label={`Close ${title.toLowerCase()}`}>
            <FaTimes aria-hidden="true" />
          </button>
        </div>
        <div className="filters-drawer__body">{children}</div>
        {footer && <div className="filters-drawer__footer">{footer}</div>}
      </div>
    </div>
  );
}
