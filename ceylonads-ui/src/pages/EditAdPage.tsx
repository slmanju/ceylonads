import { useEffect, useState } from "react";
import { Link, useParams } from "react-router-dom";
import { getMyAds } from "../api/adsApi";
import { PostAdWizard } from "./PostAd/PostAdWizard";
import { LoadingState } from "../components/LoadingState/LoadingState";
import { ErrorState } from "../components/ErrorState/ErrorState";
import type { AdResponse } from "../types/api";
import type { AdFormValues } from "./PostAd/types";
import { getApiErrorMessage } from "../utils/apiError";

export function EditAdPage() {
  const { id } = useParams<{ id: string }>();
  const [ad, setAd] = useState<AdResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;
    setLoading(true);
    setError(null);

    getMyAds()
      .then((ads) => {
        if (cancelled) return;
        const match = ads.find((a) => String(a.id) === id);
        if (!match) {
          setError("This ad could not be found, or it doesn't belong to your account.");
        } else {
          setAd(match);
        }
      })
      .catch((err) => {
        if (!cancelled) setError(getApiErrorMessage(err, "Could not load this ad."));
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });

    return () => {
      cancelled = true;
    };
  }, [id]);

  if (loading) {
    return (
      <div className="container">
        <LoadingState label="Loading your ad…" />
      </div>
    );
  }

  if (error || !ad) {
    return (
      <div className="container">
        <ErrorState title="Ad not found" message={error ?? "This ad is unavailable."} />
        <p style={{ textAlign: "center", marginTop: 16 }}>
          <Link to="/my-ads" className="btn btn-primary">
            Back to My Ads
          </Link>
        </p>
      </div>
    );
  }

  // Load the raw ad-specific override (contactOverride), never the resolved fallback (contact) -
  // otherwise an ad with no override would silently turn the account's phone into a persisted
  // override the moment the seller saves the form again.
  const phoneNumber = ad.contactOverride?.phoneNumber ?? "";
  const whatsappNumber = ad.contactOverride?.whatsappNumber ?? "";
  // Same-number heuristic: no separate "linked" flag is persisted (see backend), so infer the
  // checkbox state from whether the two override values match (or WhatsApp has no override yet).
  const whatsappSameAsPhone = whatsappNumber === "" || whatsappNumber === phoneNumber;

  const initialValues: AdFormValues = {
    categorySlug: ad.categorySlug,
    categoryPath: ad.category,
    title: ad.title,
    description: ad.description,
    // 0 means "contact for price" (see formatAdPrice) - show it as blank so the field reads as
    // unset rather than a literal zero price.
    price: ad.price === 0 ? "" : String(ad.price),
    locationSlugs: ad.locations.map((l) => l.slug),
    attributes: Object.fromEntries(ad.attributes.map((a) => [a.key, a.value])),
    contactName: ad.contactOverride?.contactName ?? "",
    phoneNumber,
    whatsappNumber,
    whatsappSameAsPhone,
  };

  return <PostAdWizard mode="edit" adId={ad.id} initialValues={initialValues} initialMedia={ad.media} />;
}
