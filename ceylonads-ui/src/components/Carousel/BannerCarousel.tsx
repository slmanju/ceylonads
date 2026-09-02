import { useEffect, useState } from "react";
import { resolveMediaUrl } from "../../api/apiClient";
import { usePrefersReducedMotion } from "../../hooks/usePrefersReducedMotion";
import { useSwipe } from "../../hooks/useSwipe";
import type { PromotionBannerResponse } from "../../types/api";
import { CarouselArrows } from "./CarouselArrows";
import { CarouselDots } from "./CarouselDots";
import "./BannerCarousel.css";

const AUTOPLAY_INTERVAL_MS = 6000;

interface BannerCarouselProps {
  banners: PromotionBannerResponse[];
}

export function BannerCarousel({ banners }: BannerCarouselProps) {
  const [index, setIndex] = useState(0);
  const [paused, setPaused] = useState(false);
  const reducedMotion = usePrefersReducedMotion();
  const count = banners.length;

  // Guards against the index going stale if the banner list shrinks (e.g. a re-fetch).
  useEffect(() => {
    if (index >= count && count > 0) {
      setIndex(0);
    }
  }, [count, index]);

  useEffect(() => {
    if (count <= 1 || paused || reducedMotion) return;
    const timer = setInterval(() => {
      setIndex((current) => (current + 1) % count);
    }, AUTOPLAY_INTERVAL_MS);
    return () => clearInterval(timer);
  }, [count, paused, reducedMotion]);

  const goTo = (next: number) => setIndex(((next % count) + count) % count);
  const swipe = useSwipe(
    () => goTo(index + 1),
    () => goTo(index - 1),
  );

  if (count === 0) return null;

  return (
    <div
      className="banner-carousel"
      onMouseEnter={() => setPaused(true)}
      onMouseLeave={() => setPaused(false)}
      onFocus={() => setPaused(true)}
      onBlur={() => setPaused(false)}
    >
      <div className="banner-carousel__viewport" onTouchStart={swipe.onTouchStart} onTouchEnd={swipe.onTouchEnd}>
        <div
          className="banner-carousel__track"
          style={{
            transform: `translateX(-${index * 100}%)`,
            transition: reducedMotion ? "none" : "transform 0.5s ease",
          }}
        >
          {banners.map((banner, slideIndex) => (
            <div className="banner-carousel__slide" key={banner.promotionId}>
              {banner.targetUrl ? (
                <a href={banner.targetUrl} target="_blank" rel="noopener noreferrer" tabIndex={index === slideIndex ? 0 : -1}>
                  <img
                    src={resolveMediaUrl(banner.bannerMediaUrl)}
                    alt="Promoted"
                    className="banner-carousel__image"
                    loading={slideIndex === 0 ? "eager" : "lazy"}
                  />
                </a>
              ) : (
                <img
                  src={resolveMediaUrl(banner.bannerMediaUrl)}
                  alt="Promoted"
                  className="banner-carousel__image"
                  loading={slideIndex === 0 ? "eager" : "lazy"}
                />
              )}
            </div>
          ))}
        </div>

        {count > 1 && <CarouselArrows onPrev={() => goTo(index - 1)} onNext={() => goTo(index + 1)} labelPrefix="Banner" />}
      </div>

      <CarouselDots count={count} activeIndex={index} onSelect={goTo} labelPrefix="Banner" />
    </div>
  );
}
