import { ClassSearchResults } from "../features/ClassSearch/ClassSearchResults";
import { Seo } from "../components/Seo/Seo";
import { useTuitionCategories } from "../hooks/useTuitionCategories";
import "./ClassesPage.css";

// "Tutors" has no dedicated backend endpoint (no tutor/seller directory API) - it's implemented
// as a meaningful, real filter over the shared ad search: individual, one-to-one tuition (the
// classType=INDIVIDUAL attribute) as opposed to institute/group classes.
export function TutorsPage() {
  const { root } = useTuitionCategories();

  return (
    <div className="classes-page container">
      <Seo
        title="Private Tutors in Sri Lanka"
        description="Find individual, one-to-one tutors for school, language and professional subjects across Sri Lanka."
      />
      {root && (
        <ClassSearchResults
          heading="Tutors"
          intro={
            <>
              Individual tutors offering one-to-one classes. Looking for group classes instead? Browse{" "}
              <a href="/classes">all classes</a>.
            </>
          }
          fixedCategorySlug={root.slug}
          fixedAttributeFilters={{ classType: "INDIVIDUAL" }}
          emptyTitle="No individual tutors match your search"
          emptyMessage="Try a different subject or district."
        />
      )}
    </div>
  );
}
