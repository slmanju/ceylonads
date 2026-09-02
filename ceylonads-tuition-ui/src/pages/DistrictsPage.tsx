import { Link } from "react-router-dom";
import { FaMapMarkerAlt } from "react-icons/fa";
import { Seo } from "../components/Seo/Seo";
import { LoadingState } from "../components/LoadingState/LoadingState";
import { ErrorState } from "../components/ErrorState/ErrorState";
import { useDistricts } from "../hooks/useDistricts";
import "./DistrictsPage.css";

export function DistrictsPage() {
  const { districts, loading, error } = useDistricts();

  return (
    <div className="districts-page container">
      <Seo
        title="Browse Tuition Classes by District"
        description="Find tuition classes and tutors in every district of Sri Lanka."
      />
      <h1 className="districts-page__title">Browse by District</h1>
      <p className="districts-page__subtitle">Find classes and tutors near you, anywhere in Sri Lanka.</p>

      {loading && <LoadingState label="Loading districts…" />}
      {error && <ErrorState message={error} />}
      {!loading && !error && (
        <div className="districts-page__grid">
          {districts.map((district) => (
            <Link key={district.id} to={`/classes?location=${district.slug}`} className="districts-page__card">
              <FaMapMarkerAlt aria-hidden="true" />
              {district.name}
            </Link>
          ))}
        </div>
      )}
    </div>
  );
}
