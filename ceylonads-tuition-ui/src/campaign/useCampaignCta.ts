import { useNavigate } from "react-router-dom";
import { useAuth } from "../auth/AuthContext";

// Campaign CTA behavior is product-defined, not database-driven (the campaign only supplies
// ctaLabel - see tuition CLAUDE.md "Promotions" / "CTA Behavior"): authenticated tutors go to My
// Classes to pick which class to promote; guests go to Login first, reusing ProtectedRoute's own
// `state.from.pathname` redirect-after-login convention so they land back on My Classes once
// logged in.
const PROMOTION_INTENT_PATH = "/my-ads";

export function useCampaignCta(): () => void {
  const { isAuthenticated } = useAuth();
  const navigate = useNavigate();

  return () => {
    if (isAuthenticated) {
      navigate(PROMOTION_INTENT_PATH);
    } else {
      navigate("/login", { state: { from: { pathname: PROMOTION_INTENT_PATH } } });
    }
  };
}
