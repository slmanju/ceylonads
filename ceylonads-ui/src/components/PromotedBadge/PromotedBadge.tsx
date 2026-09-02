import { FaStar } from "react-icons/fa";
import "./PromotedBadge.css";

export function PromotedBadge() {
  return (
    <span className="promoted-badge">
      <FaStar aria-hidden="true" />
      Featured
    </span>
  );
}
