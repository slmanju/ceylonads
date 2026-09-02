import { useEffect, useMemo, useRef, useState, type KeyboardEvent } from "react";
import { FaChevronDown, FaTimes } from "react-icons/fa";
import { useClickOutside } from "../../hooks/useClickOutside";
import "./SubjectCombobox.css";

export interface ComboboxOption {
  value: string;
  label: string;
}

interface SubjectComboboxProps {
  id: string;
  label: string;
  options: ComboboxOption[];
  value: string;
  onChange: (value: string) => void;
  placeholder?: string;
  loading?: boolean;
}

// Searchable select over the tuition subject master data (GET /api/tuition/filters ->
// filters.subjects). Only a value already present in `options` can be committed - typing filters
// the list but never submits arbitrary free text, per the tuition search spec.
export function SubjectCombobox({
  id,
  label,
  options,
  value,
  onChange,
  placeholder = "Search subject...",
  loading = false,
}: SubjectComboboxProps) {
  const [open, setOpen] = useState(false);
  const [query, setQuery] = useState("");
  const [highlighted, setHighlighted] = useState(0);
  const rootRef = useRef<HTMLDivElement>(null);

  const selected = options.find((o) => o.value === value) ?? null;

  useClickOutside(rootRef, () => setOpen(false), open);

  useEffect(() => {
    if (!open) setQuery("");
  }, [open]);

  const filtered = useMemo(() => {
    const q = query.trim().toLowerCase();
    if (!q) return options;
    return options.filter((o) => o.label.toLowerCase().includes(q));
  }, [options, query]);

  useEffect(() => {
    setHighlighted(0);
  }, [query, open]);

  const commit = (option: ComboboxOption | null) => {
    onChange(option ? option.value : "");
    setOpen(false);
    setQuery("");
  };

  const handleKeyDown = (e: KeyboardEvent<HTMLInputElement>) => {
    if (!open) {
      if (e.key === "ArrowDown" || e.key === "Enter") {
        e.preventDefault();
        setOpen(true);
      }
      return;
    }

    if (e.key === "ArrowDown") {
      e.preventDefault();
      setHighlighted((h) => Math.min(h + 1, filtered.length - 1));
    } else if (e.key === "ArrowUp") {
      e.preventDefault();
      setHighlighted((h) => Math.max(h - 1, 0));
    } else if (e.key === "Enter") {
      e.preventDefault();
      const option = filtered[highlighted];
      if (option) commit(option);
    } else if (e.key === "Escape") {
      setOpen(false);
      setQuery("");
    }
  };

  const displayValue = open ? query : (selected?.label ?? "");

  return (
    <div className="subject-combobox" ref={rootRef}>
      <label htmlFor={id} className="visually-hidden">
        {label}
      </label>
      <div className={`subject-combobox__control${open ? " subject-combobox__control--open" : ""}`}>
        <input
          id={id}
          type="text"
          role="combobox"
          aria-expanded={open}
          aria-autocomplete="list"
          aria-controls={`${id}-listbox`}
          autoComplete="off"
          placeholder={loading ? "Loading subjects…" : placeholder}
          value={displayValue}
          disabled={loading}
          onFocus={() => setOpen(true)}
          onChange={(e) => {
            setQuery(e.target.value);
            setOpen(true);
          }}
          onKeyDown={handleKeyDown}
        />
        {selected && !open ? (
          <button
            type="button"
            className="subject-combobox__clear"
            aria-label={`Clear ${label.toLowerCase()}`}
            onMouseDown={(e) => e.preventDefault()}
            onClick={() => commit(null)}
          >
            <FaTimes aria-hidden="true" />
          </button>
        ) : (
          <FaChevronDown aria-hidden="true" className="subject-combobox__chevron" />
        )}
      </div>

      {open && (
        <ul id={`${id}-listbox`} role="listbox" className="subject-combobox__listbox">
          <li role="option" aria-selected={value === ""}>
            <button
              type="button"
              className="subject-combobox__option subject-combobox__option--any"
              onMouseDown={(e) => e.preventDefault()}
              onClick={() => commit(null)}
            >
              Any subject
            </button>
          </li>
          {filtered.length === 0 && <li className="subject-combobox__empty">No subjects match</li>}
          {filtered.map((option, index) => (
            <li key={option.value} role="option" aria-selected={option.value === value}>
              <button
                type="button"
                className={`subject-combobox__option${index === highlighted ? " subject-combobox__option--highlighted" : ""}${
                  option.value === value ? " subject-combobox__option--selected" : ""
                }`}
                onMouseDown={(e) => e.preventDefault()}
                onClick={() => commit(option)}
              >
                {option.label}
              </button>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}
