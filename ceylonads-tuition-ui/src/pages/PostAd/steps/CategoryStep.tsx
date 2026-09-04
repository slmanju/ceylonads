import { FaBook, FaChalkboardTeacher, FaCheckCircle, FaGraduationCap, FaLaptopCode, FaUniversity } from "react-icons/fa";
import type { CategoryResponse } from "../../../types/api";
import { LoadingState } from "../../../components/LoadingState/LoadingState";
import { ErrorState } from "../../../components/ErrorState/ErrorState";
import "./CategoryStep.css";

const ICONS: Record<string, typeof FaBook> = {
  "school-tuition": FaBook,
  "higher-education": FaUniversity,
  "language-classes": FaChalkboardTeacher,
  "professional-courses": FaGraduationCap,
  "online-courses": FaLaptopCode,
};

interface CategoryStepProps {
  subcategories: CategoryResponse[];
  loading: boolean;
  error: string | null;
  categorySlug: string;
  onSelect: (slug: string, name: string) => void;
}

export function CategoryStep({ subcategories, loading, error, categorySlug, onSelect }: CategoryStepProps) {
  if (loading) return <LoadingState label="Loading class types…" />;
  if (error) return <ErrorState message={error} />;

  return (
    <div className="post-ad-step">
      <h2 className="post-ad-step__title">What kind of class are you advertising?</h2>
      <p className="post-ad-step__subtitle">Choose the class type that best fits what you're offering.</p>

      <div className="category-step__grid">
        {subcategories.map((category) => {
          const Icon = ICONS[category.slug] ?? FaBook;
          const isSelected = categorySlug === category.slug;
          return (
            <button
              key={category.id}
              type="button"
              className={`category-step__card ${isSelected ? "category-step__card--selected" : ""}`}
              onClick={() => onSelect(category.slug, category.name)}
            >
              <Icon aria-hidden="true" className="category-step__card-icon" />
              <span>{category.name}</span>
              {isSelected && <FaCheckCircle aria-hidden="true" className="category-step__check" />}
            </button>
          );
        })}
      </div>
    </div>
  );
}
