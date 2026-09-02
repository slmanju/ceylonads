import { Link } from "react-router-dom";
import { getCategoryIcon } from "../../utils/categoryIcons";
import type { CategoryResponse } from "../../types/api";
import "./CategoryCard.css";

interface CategoryCardProps {
  category: CategoryResponse;
  adCount?: number;
}

export function CategoryCard({ category, adCount }: CategoryCardProps) {
  const Icon = getCategoryIcon(category.slug);

  return (
    <Link to={`/ads?category=${category.slug}`} className="category-card">
      <span className="category-card__icon">
        <Icon aria-hidden="true" />
      </span>
      <span className="category-card__name">{category.name}</span>
      {typeof adCount === "number" && <span className="category-card__count">{adCount} ads</span>}
    </Link>
  );
}
