import { useEffect, useState } from "react";
import { Link, useParams } from "react-router-dom";
import { getMyAds } from "../api/adsApi";
import { PostAdWizard } from "./PostAd/PostAdWizard";
import { LoadingState } from "../components/LoadingState/LoadingState";
import { ErrorState } from "../components/ErrorState/ErrorState";
import { Seo } from "../components/Seo/Seo";
import { useTuitionCategories } from "../hooks/useTuitionCategories";
import type { AdResponse } from "../types/api";
import type { AdFormValues } from "./PostAd/types";
import { getApiErrorMessage } from "../utils/apiError";

export function EditAdPage() {
  const { id } = useParams<{ id: string }>();
  const { bySlug: tuitionCategorySlugs, loading: categoriesLoading } = useTuitionCategories();
  const [ad, setAd] = useState<AdResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (categoriesLoading) return;
    let cancelled = false;
    setLoading(true);
    setError(null);

    getMyAds()
      .then((ads) => {
        if (cancelled) return;
        const match = ads.find((a) => String(a.id) === id);
        if (!match) {
          setError("This ad could not be found, or it doesn't belong to your account.");
        } else if (!tuitionCategorySlugs.has(match.categorySlug)) {
          // This is the tuition site's edit form (tuition category picker, tuition-only
          // attributes) - an ad from elsewhere in the CeylonAds marketplace can't be edited here.
          setError("This ad isn't a tuition ad. Edit it from the main CeylonAds site instead.");
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
  }, [id, categoriesLoading, tuitionCategorySlugs]);

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
            Back to My Classes
          </Link>
        </p>
      </div>
    );
  }

  const phoneNumber = ad.contactOverride?.phoneNumber ?? "";
  const whatsappNumber = ad.contactOverride?.whatsappNumber ?? "";
  const whatsappSameAsPhone = whatsappNumber === "" || whatsappNumber === phoneNumber;

  const initialValues: AdFormValues = {
    categorySlug: ad.categorySlug,
    categoryPath: ad.category,
    title: ad.title,
    description: ad.description,
    price: ad.price === 0 ? "" : String(ad.price),
    locationSlugs: ad.locations.map((l) => l.slug),
    attributes: Object.fromEntries(ad.attributes.map((a) => [a.key, a.value])),
    contactName: ad.contactOverride?.contactName ?? "",
    phoneNumber,
    whatsappNumber,
    whatsappSameAsPhone,
  };

  return (
    <>
      <Seo title="Edit Your Ad" noindex />
      <PostAdWizard mode="edit" adId={ad.id} initialValues={initialValues} initialMedia={ad.media} />
    </>
  );
}
