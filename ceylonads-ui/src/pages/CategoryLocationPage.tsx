import { Navigate, useParams, useSearchParams } from "react-router-dom";

// Legacy category+location SEO landing URL (e.g. /vehicles/colombo) kept working as a redirect
// onto the unified search page - see CategoryPage.tsx for the equivalent single-category redirect.
export function CategoryLocationPage() {
  const { categorySlug = "", locationSlug = "" } = useParams<{ categorySlug: string; locationSlug: string }>();
  const [searchParams] = useSearchParams();
  const page = searchParams.get("page");

  const query = new URLSearchParams({ category: categorySlug, location: locationSlug });
  if (page) query.set("page", page);

  return <Navigate to={`/ads?${query.toString()}`} replace />;
}
