import { useEffect, useRef, useState } from "react";
import type { TouchEvent } from "react";
import { FaChevronLeft, FaChevronRight, FaExpandAlt, FaImage } from "react-icons/fa";
import type { MediaResponse } from "../../types/api";
import { resolveMediaUrl } from "../../api/apiClient";
import { ImageLightbox } from "../ImageLightbox/ImageLightbox";
import "./ImageGallery.css";

interface ImageGalleryProps {
  media: MediaResponse[];
  title: string;
  // "portrait" is for categories whose promotional artwork is taller than it is wide (e.g.
  // Tuition posters) - it changes the main/thumbnail aspect ratio only, never the gallery
  // behavior, so every other category keeps today's landscape treatment untouched.
  variant?: "landscape" | "portrait";
}

const SWIPE_THRESHOLD = 40;

export function ImageGallery({ media, title, variant = "landscape" }: ImageGalleryProps) {
  const galleryClassName = `image-gallery ${variant === "portrait" ? "image-gallery--portrait" : ""}`.trim();
  const [activeIndex, setActiveIndex] = useState(0);
  const [lightboxOpen, setLightboxOpen] = useState(false);
  const thumbRefs = useRef<Array<HTMLButtonElement | null>>([]);
  const touchStartX = useRef<number | null>(null);

  const hasMultiple = media.length > 1;

  useEffect(() => {
    thumbRefs.current[activeIndex]?.scrollIntoView({ block: "nearest", inline: "nearest", behavior: "smooth" });
  }, [activeIndex]);

  if (media.length === 0) {
    return (
      <div className={galleryClassName}>
        <div className="image-gallery__main image-gallery__main--fallback">
          <FaImage aria-hidden="true" />
        </div>
      </div>
    );
  }

  const goToPrev = () => {
    if (!hasMultiple) return;
    setActiveIndex((i) => (i - 1 + media.length) % media.length);
  };

  const goToNext = () => {
    if (!hasMultiple) return;
    setActiveIndex((i) => (i + 1) % media.length);
  };

  const handleTouchStart = (e: TouchEvent<HTMLDivElement>) => {
    touchStartX.current = e.touches[0].clientX;
  };

  const handleTouchEnd = (e: TouchEvent<HTMLDivElement>) => {
    if (touchStartX.current === null) return;
    const delta = e.changedTouches[0].clientX - touchStartX.current;
    touchStartX.current = null;
    if (Math.abs(delta) < SWIPE_THRESHOLD) return;
    if (delta > 0) goToPrev();
    else goToNext();
  };

  const active = media[activeIndex] ?? media[0];

  return (
    <div className={galleryClassName}>
      <div className="image-gallery__main" onTouchStart={handleTouchStart} onTouchEnd={handleTouchEnd}>
        <button
          type="button"
          className="image-gallery__viewer"
          onClick={() => setLightboxOpen(true)}
          aria-label="View larger image"
        >
          <img src={resolveMediaUrl(active.url)} alt={title} />
          <span className="image-gallery__expand" aria-hidden="true">
            <FaExpandAlt />
          </span>
        </button>

        {hasMultiple && (
          <>
            <button
              type="button"
              className="image-gallery__nav image-gallery__nav--prev"
              onClick={goToPrev}
              aria-label="Previous image"
            >
              <FaChevronLeft aria-hidden="true" />
            </button>
            <button
              type="button"
              className="image-gallery__nav image-gallery__nav--next"
              onClick={goToNext}
              aria-label="Next image"
            >
              <FaChevronRight aria-hidden="true" />
            </button>
          </>
        )}
      </div>

      {hasMultiple && (
        <div className="image-gallery__thumbs" role="tablist" aria-label="Ad photos">
          {media.map((item, index) => (
            <button
              key={item.id}
              ref={(el) => {
                thumbRefs.current[index] = el;
              }}
              type="button"
              role="tab"
              aria-selected={index === activeIndex}
              className={`image-gallery__thumb ${index === activeIndex ? "image-gallery__thumb--active" : ""}`}
              onClick={() => setActiveIndex(index)}
            >
              <img src={resolveMediaUrl(item.url)} alt="" />
            </button>
          ))}
        </div>
      )}

      {lightboxOpen && (
        <ImageLightbox
          media={media}
          index={activeIndex}
          title={title}
          onIndexChange={setActiveIndex}
          onClose={() => setLightboxOpen(false)}
        />
      )}
    </div>
  );
}
