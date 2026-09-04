import { useEffect, useState } from "react";
import { listSuggestions, updateSuggestionStatus } from "../../api/adminTuitionApi";
import { LoadingState } from "../../components/LoadingState/LoadingState";
import { ErrorState } from "../../components/ErrorState/ErrorState";
import { EmptyState } from "../../components/EmptyState/EmptyState";
import { formatFullDate } from "../../utils/formatDate";
import { getApiErrorMessage } from "../../utils/apiError";
import type { TuitionSuggestionAdmin } from "../../types/api";
import "./AdminSuggestionsPage.css";

export function AdminSuggestionsPage() {
  const [suggestions, setSuggestions] = useState<TuitionSuggestionAdmin[] | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);
  const [actionLoadingId, setActionLoadingId] = useState<number | null>(null);
  const [expandedId, setExpandedId] = useState<number | null>(null);

  const load = () => {
    setLoading(true);
    setError(null);
    listSuggestions()
      .then(setSuggestions)
      .catch((err) => setError(getApiErrorMessage(err, "Could not load suggestions.")))
      .finally(() => setLoading(false));
  };

  useEffect(load, []);

  const handleStatusChange = async (id: number, status: "REVIEWED" | "CLOSED") => {
    setActionLoadingId(id);
    try {
      const updated = await updateSuggestionStatus(id, status);
      setSuggestions((current) => current?.map((s) => (s.id === id ? updated : s)) ?? null);
    } catch (err) {
      setError(getApiErrorMessage(err, "Could not update this suggestion."));
    } finally {
      setActionLoadingId(null);
    }
  };

  if (loading) return <LoadingState label="Loading suggestions…" />;
  if (error && !suggestions) return <ErrorState message={error} onRetry={load} />;

  return (
    <div className="tuition-admin-suggestions">
      <h1>Suggestions</h1>
      {error && (
        <p className="tuition-admin-suggestions__error" role="alert">
          {error}
        </p>
      )}

      {!suggestions || suggestions.length === 0 ? (
        <EmptyState title="No suggestions yet" message="Feedback submitted on the public Suggest page will show up here." />
      ) : (
        <ul className="tuition-admin-suggestions__list">
          {suggestions.map((s) => {
            const expanded = expandedId === s.id;
            const contact = [s.name, s.email, s.phone].filter(Boolean).join(" · ");
            return (
              <li key={s.id} className="tuition-admin-suggestion">
                <button
                  type="button"
                  className="tuition-admin-suggestion__row"
                  onClick={() => setExpandedId(expanded ? null : s.id)}
                >
                  <span className={`tuition-admin-suggestion__status tuition-admin-suggestion__status--${s.status.toLowerCase()}`}>
                    {s.status}
                  </span>
                  <span className="tuition-admin-suggestion__preview">
                    {contact && <strong>{contact} — </strong>}
                    {s.message}
                  </span>
                  <span className="tuition-admin-suggestion__date">{formatFullDate(s.createdAt)}</span>
                </button>

                {expanded && (
                  <div className="tuition-admin-suggestion__detail">
                    <p className="tuition-admin-suggestion__message">{s.message}</p>
                    <div className="tuition-admin-suggestion__actions">
                      {s.status !== "REVIEWED" && (
                        <button
                          type="button"
                          className="btn btn-outline"
                          disabled={actionLoadingId === s.id}
                          onClick={() => handleStatusChange(s.id, "REVIEWED")}
                        >
                          Mark Reviewed
                        </button>
                      )}
                      {s.status !== "CLOSED" && (
                        <button
                          type="button"
                          className="btn btn-outline"
                          disabled={actionLoadingId === s.id}
                          onClick={() => handleStatusChange(s.id, "CLOSED")}
                        >
                          Close
                        </button>
                      )}
                    </div>
                  </div>
                )}
              </li>
            );
          })}
        </ul>
      )}
    </div>
  );
}
