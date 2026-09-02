import { Navigate, useParams, useSearchParams } from "react-router-dom";

// Legacy category-browsing URL kept working as a redirect onto the unified search page, so old
// links/bookmarks/indexed pages still resolve - category selection now lives entirely in
// /ads?category=... search state rather than a separate browsing route.
export function CategoryPage() {
  const { slug = "" } = useParams<{ slug: string }>();
  const [searchParams] = useSearchParams();
  const page = searchParams.get("page");

  const query = new URLSearchParams({ category: slug });
  if (page) query.set("page", page);

  return <Navigate to={`/ads?${query.toString()}`} replace />;
}
