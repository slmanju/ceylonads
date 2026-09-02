import type { ReactNode } from "react";
import { Link } from "react-router-dom";
import type { PromotionTarget } from "../../tuition/promotion/model/promotion";
import { resolvePromotionTargetHref } from "../../tuition/promotion/api/resolveTarget";

interface PromotionLinkProps {
  target: PromotionTarget;
  className?: string;
  children: ReactNode;
}

export function PromotionLink({ target, className, children }: PromotionLinkProps) {
  const href = resolvePromotionTargetHref(target);

  if (href.startsWith("/")) {
    return (
      <Link to={href} className={className}>
        {children}
      </Link>
    );
  }

  return (
    <a href={href} className={className} target="_blank" rel="noopener noreferrer">
      {children}
    </a>
  );
}
