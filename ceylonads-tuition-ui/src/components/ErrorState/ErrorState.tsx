import "./ErrorState.css";

interface ErrorStateProps {
  title?: string;
  message?: string;
  onRetry?: () => void;
}

export function ErrorState({
  title = "Something went wrong",
  message = "Please try again in a moment.",
  onRetry,
}: ErrorStateProps) {
  return (
    <div className="error-state" role="alert">
      <p className="error-state__title">{title}</p>
      <p className="error-state__message">{message}</p>
      {onRetry && (
        <button type="button" className="btn btn-secondary" onClick={onRetry}>
          Try again
        </button>
      )}
    </div>
  );
}
