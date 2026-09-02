import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { FaBullhorn } from "react-icons/fa";
import { AdCard } from "../AdCard/AdCard";
import { usePrefersReducedMotion } from "../../hooks/usePrefersReducedMotion";
import { useSwipe } from "../../hooks/useSwipe";
import type { AdResponse } from "../../types/api";
import { CarouselArrows } from "./CarouselArrows";
import { CarouselDots } from "./CarouselDots";
import "./AdCarousel.css";

export interface AdCarouselPlaceholder {
  title: string;
  subtitle: string;
  priceLabel: string;
}

interface AdCarouselProps {
  ads: AdResponse[];
  visibleCount: number;
  labelPrefix: string;
  placeholder?: AdCarouselPlaceholder;
}

function chunk(ads: AdResponse[], size: number): AdResponse[][] {
  if (ads.length === 0) return [[]];
  const pages: AdResponse[][] = [];
  for (let i = 0; i < ads.length; i += size) {
    pages.push(ads.slice(i, i + size));
  }
  return pages;
}

export function AdCarousel({ ads, visibleCount, labelPrefix, placeholder }: AdCarouselProps) {
  const pages = chunk(ads, visibleCount);
  const [page, setPage] = useState(0);
  const reducedMotion = usePrefersReducedMotion();

  useEffect(() => {
    if (page >= pages.length) {
      setPage(0);
    }
  }, [page, pages.length]);

  const goTo = (next: number) => setPage(((next % pages.length) + pages.length) % pages.length);
  const swipe = useSwipe(
    () => goTo(page + 1),
    () => goTo(page - 1),
  );

  if (ads.length === 0 && !placeholder) return null;

  return (
    <div className="listing-carousel">
      <div className="listing-carousel__viewport" onTouchStart={swipe.onTouchStart} onTouchEnd={swipe.onTouchEnd}>
        <div
          className="listing-carousel__track"
          style={{
            transform: `translateX(-${page * 100}%)`,
            transition: reducedMotion ? "none" : "transform 0.4s ease",
          }}
        >
          {pages.map((pageAds, pageIndex) => {
            // Without placeholders (e.g. category-featured), a sparse page shouldn't reserve
            // empty grid columns for cards that don't exist.
            const columns = placeholder ? visibleCount : Math.max(1, pageAds.length);
            return (
            <div
              className={`listing-carousel__page listing-carousel__page--cols-${columns}`}
              key={pageIndex}
              aria-hidden={pageIndex !== page}
            >
              {pageAds.map((ad) => (
                <AdCard key={ad.id} ad={ad} />
              ))}
              {placeholder &&
                pageIndex === pages.length - 1 &&
                Array.from({ length: Math.max(0, visibleCount - pageAds.length) }).map((_, i) => (
                  <Link to="/my-ads" className="listing-carousel__placeholder" key={`placeholder-${i}`}>
                    <FaBullhorn className="listing-carousel__placeholder-icon" aria-hidden="true" />
                    <span className="listing-carousel__placeholder-title">{placeholder.title}</span>
                    <span className="listing-carousel__placeholder-subtitle">{placeholder.subtitle}</span>
                    <span className="listing-carousel__placeholder-price">{placeholder.priceLabel}</span>
                  </Link>
                ))}
            </div>
            );
          })}
        </div>

        {pages.length > 1 && (
          <CarouselArrows onPrev={() => goTo(page - 1)} onNext={() => goTo(page + 1)} labelPrefix={labelPrefix} />
        )}
      </div>

      <CarouselDots count={pages.length} activeIndex={page} onSelect={goTo} labelPrefix={labelPrefix} />
    </div>
  );
}
