import { ClassSearchResults } from "../features/ClassSearch/ClassSearchResults";
import { Seo } from "../components/Seo/Seo";
import { useTuitionCategories } from "../hooks/useTuitionCategories";
import "./ClassesPage.css";

export function OnlineClassesPage() {
  const { root } = useTuitionCategories();

  return (
    <div className="classes-page container">
      <Seo
        title="Online Tuition Classes in Sri Lanka"
        description="Browse online tuition classes and courses taught remotely across Sri Lanka."
      />
      {root && (
        <ClassSearchResults
          heading="Online Classes"
          fixedCategorySlug={root.slug}
          fixedAttributeFilters={{ classMode: "ONLINE" }}
          emptyTitle="No online classes match your search"
          emptyMessage="Try a different subject or clear some filters."
        />
      )}
    </div>
  );
}
