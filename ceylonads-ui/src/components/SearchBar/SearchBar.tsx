import { useState, type FormEvent } from "react";
import { useNavigate } from "react-router-dom";
import { FaSearch } from "react-icons/fa";
import type { CategoryResponse, LocationResponse } from "../../types/api";
import { CategoryField } from "./CategoryField";
import { LocationField } from "./LocationField";
import "./SearchBar.css";

interface SearchBarProps {
  categories: CategoryResponse[];
  locations: LocationResponse[];
  initialQuery?: string;
  initialCategory?: string;
  initialLocation?: string;
}

export function SearchBar({
  categories,
  locations,
  initialQuery = "",
  initialCategory = "",
  initialLocation = "",
}: SearchBarProps) {
  const [q, setQ] = useState(initialQuery);
  const [category, setCategory] = useState(initialCategory);
  const [location, setLocation] = useState(initialLocation);
  const navigate = useNavigate();

  const handleSubmit = (event: FormEvent) => {
    event.preventDefault();
    const params = new URLSearchParams();
    if (q.trim()) params.set("q", q.trim());
    if (category) params.set("category", category);
    if (location) params.set("location", location);
    navigate(`/ads?${params.toString()}`);
  };

  return (
    <form className="search-bar" onSubmit={handleSubmit} role="search">
      <div className="search-bar__field search-bar__field--query">
        <FaSearch className="search-bar__icon" aria-hidden="true" />
        <label htmlFor="search-q" className="visually-hidden">
          What are you looking for?
        </label>
        <input
          id="search-q"
          type="text"
          placeholder="What are you looking for?"
          value={q}
          onChange={(e) => setQ(e.target.value)}
        />
      </div>

      <div className="search-bar__field">
        <CategoryField categories={categories} value={category} onChange={setCategory} />
      </div>

      <div className="search-bar__field">
        <LocationField locations={locations} value={location} onChange={setLocation} />
      </div>

      <button type="submit" className="btn btn-primary search-bar__submit">
        Search
      </button>
    </form>
  );
}
