import { useState, type FormEvent } from "react";
import { useNavigate } from "react-router-dom";
import { FaSearch } from "react-icons/fa";
import type { LocationResponse } from "../../types/api";
import { LocationField } from "../LocationField/LocationField";
import { SubjectCombobox } from "../SubjectCombobox/SubjectCombobox";
import { useTuitionFilters } from "../../hooks/useTuitionFilters";
import { useMediaQuery } from "../../hooks/useMediaQuery";
import "./SearchBar.css";

interface SearchBarProps {
  locations: LocationResponse[];
}

export function SearchBar({ locations }: SearchBarProps) {
  const [subject, setSubject] = useState("");
  const [location, setLocation] = useState("");
  const navigate = useNavigate();
  const { data: filters, loading: filtersLoading } = useTuitionFilters();
  const isNarrow = useMediaQuery("(max-width: 380px)");

  const handleSubmit = (event: FormEvent) => {
    event.preventDefault();
    const params = new URLSearchParams();
    if (subject) params.set("subject", subject);
    if (location) params.set("location", location);
    navigate(`/classes?${params.toString()}`);
  };

  return (
    <form className="tuition-search-bar" onSubmit={handleSubmit} role="search">
      <div className="tuition-search-bar__field tuition-search-bar__field--query">
        <FaSearch className="tuition-search-bar__icon" aria-hidden="true" />
        <SubjectCombobox
          id="tuition-search-subject"
          label="Search by subject, e.g. A/L Physics"
          options={filters?.subjects ?? []}
          value={subject}
          onChange={setSubject}
          placeholder={isNarrow ? "Search by subject" : 'Search by subject, e.g. "A/L Physics"'}
          loading={filtersLoading}
        />
      </div>

      <div className="tuition-search-bar__field">
        <LocationField locations={locations} value={location} onChange={setLocation} />
      </div>

      <button type="submit" className="btn btn-accent tuition-search-bar__submit">
        Search Classes
      </button>
    </form>
  );
}
