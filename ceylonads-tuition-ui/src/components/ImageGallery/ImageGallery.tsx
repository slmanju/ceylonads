import { useState, type KeyboardEvent } from "react";
import { FaBook, FaChevronLeft, FaChevronRight } from "react-icons/fa";
import type { MediaResponse } from "../../types/api";
import { resolveMediaUrl } from "../../api/apiClient";
import "./ImageGallery.css";

interface ImageGalleryProps {
  media: MediaResponse[];
  title: string;
}

// Only the first MAX_VISIBLE_THUMBS - 1 thumbnails ever render individually; once more images
// exist, the final slot becomes a "+N" tile (see section 8 of the redesign brief) rather than
// letting the thumbnail row grow wide enough to unbalance the gallery shell.
const MAX_VISIBLE_THUMBS = 5;

export function ImageGallery({ media, title }: ImageGalleryProps) {
  const [activeIndex, setActiveIndex] = useState(0);

  if (media.length === 0) {
    return (
      <div className="image-gallery">
        <div className="image-gallery__stage">
          <div className="image-gallery__main image-gallery__main--fallback">
            <FaBook aria-hidden="true" />
          </div>
        </div>
      </div>
    );
  }

  const total = media.length;
  const active = media[activeIndex] ?? media[0];
  const hasMultiple = total > 1;
  const overflowStartIndex = MAX_VISIBLE_THUMBS - 1;
  const showOverflowTile = total > MAX_VISIBLE_THUMBS;
  const visibleThumbs = showOverflowTile ? media.slice(0, overflowStartIndex) : media;

  const goPrev = () => setActiveIndex((i) => (i - 1 + total) % total);
  const goNext = () => setActiveIndex((i) => (i + 1) % total);

  // Arrow keys move the gallery while focus is on the stage, matching the "keyboard accessible"
  // requirement without hijacking arrow keys anywhere else on the page.
  const handleStageKeyDown = (e: KeyboardEvent) => {
    if (!hasMultiple) return;
    if (e.key === "ArrowLeft") {
      e.preventDefault();
      goPrev();
    } else if (e.key === "ArrowRight") {
      e.preventDefault();
      goNext();
    }
  };

  return (
    <div className="image-gallery">
      <div
        className="image-gallery__stage"
        tabIndex={hasMultiple ? 0 : -1}
        role="group"
        aria-label={`${title} photos`}
        aria-roledescription="carousel"
        onKeyDown={handleStageKeyDown}
      >
        <div className="image-gallery__frame">
          {hasMultiple && (
            <button
              type="button"
              className="image-gallery__arrow image-gallery__arrow--prev"
              onClick={goPrev}
              aria-label="Previous image"
            >
              <FaChevronLeft aria-hidden="true" />
            </button>
          )}

          <div className="image-gallery__main">
            <img src={resolveMediaUrl(active.url)} alt={title} />
          </div>

          {hasMultiple && (
            <button
              type="button"
              className="image-gallery__arrow image-gallery__arrow--next"
              onClick={goNext}
              aria-label="Next image"
            >
              <FaChevronRight aria-hidden="true" />
            </button>
          )}
        </div>
      </div>

      {hasMultiple && (
        <>
          <div className="image-gallery__thumbs" role="tablist" aria-label="Class photos">
            {visibleThumbs.map((item, index) => (
              <button
                key={item.id}
                type="button"
                role="tab"
                aria-selected={index === activeIndex}
                className={`image-gallery__thumb ${index === activeIndex ? "image-gallery__thumb--active" : ""}`}
                onClick={() => setActiveIndex(index)}
              >
                <img src={resolveMediaUrl(item.url)} alt="" />
              </button>
            ))}

            {showOverflowTile && (
              <button
                type="button"
                role="tab"
                aria-selected={activeIndex >= overflowStartIndex}
                aria-label={`Show ${total - overflowStartIndex} more photos`}
                className={`image-gallery__thumb image-gallery__thumb--overflow ${
                  activeIndex >= overflowStartIndex ? "image-gallery__thumb--active" : ""
                }`}
                onClick={() => setActiveIndex(overflowStartIndex)}
              >
                <img src={resolveMediaUrl(media[overflowStartIndex].url)} alt="" />
                <span className="image-gallery__thumb-overflow-count">+{total - overflowStartIndex}</span>
              </button>
            )}
          </div>

          <p className="image-gallery__counter">
            {activeIndex + 1} / {total}
          </p>
        </>
      )}
    </div>
  );
}
