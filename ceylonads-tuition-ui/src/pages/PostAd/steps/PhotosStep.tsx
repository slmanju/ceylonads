import { useRef, useState, type DragEvent } from "react";
import { FaCloudUploadAlt, FaTimes, FaExclamationCircle, FaRedo } from "react-icons/fa";
import type { MediaResponse } from "../../../types/api";
import { resolveMediaUrl } from "../../../api/apiClient";
import { MAX_PHOTOS, type PendingPhoto } from "../types";
import "./PhotosStep.css";

interface PhotosStepProps {
  existingMedia: MediaResponse[];
  pendingPhotos: PendingPhoto[];
  onAddFiles: (files: File[]) => void;
  onRemovePhoto: (localId: string) => void;
  onRetryPhoto?: (localId: string) => void;
}

export function PhotosStep({ existingMedia, pendingPhotos, onAddFiles, onRemovePhoto, onRetryPhoto }: PhotosStepProps) {
  const inputRef = useRef<HTMLInputElement>(null);
  const [dragActive, setDragActive] = useState(false);
  const [validationError, setValidationError] = useState<string | null>(null);

  const usedSlots = existingMedia.length + pendingPhotos.length;
  const remainingSlots = MAX_PHOTOS - usedSlots;

  const handleFiles = (fileList: FileList | File[]) => {
    const files = Array.from(fileList);
    const imagesOnly = files.filter((f) => f.type.startsWith("image/"));

    if (imagesOnly.length < files.length) {
      setValidationError("Only image files can be uploaded.");
    } else {
      setValidationError(null);
    }

    const accepted = imagesOnly.slice(0, Math.max(0, remainingSlots));
    if (accepted.length < imagesOnly.length) {
      setValidationError(`You can add up to ${MAX_PHOTOS} photos per ad.`);
    }

    if (accepted.length > 0) onAddFiles(accepted);
  };

  const handleDrop = (e: DragEvent<HTMLDivElement>) => {
    e.preventDefault();
    setDragActive(false);
    if (e.dataTransfer.files?.length) handleFiles(e.dataTransfer.files);
  };

  return (
    <div className="post-ad-step">
      <h2 className="post-ad-step__title">Add photos</h2>
      <p className="post-ad-step__subtitle">Classes with photos of your classroom or materials get more responses.</p>
      <p className="post-ad-form__hint">
        Recommended poster ratio: 4:5 (e.g. 1080 × 1350). Portrait posters work best for tuition classes.
      </p>

      {remainingSlots > 0 && (
        <div
          className={`photos-step__dropzone ${dragActive ? "photos-step__dropzone--active" : ""}`}
          onDragOver={(e) => {
            e.preventDefault();
            setDragActive(true);
          }}
          onDragLeave={() => setDragActive(false)}
          onDrop={handleDrop}
        >
          <FaCloudUploadAlt aria-hidden="true" className="photos-step__dropzone-icon" />
          <p className="photos-step__dropzone-title">Add Photos</p>
          <p className="photos-step__dropzone-hint">Drag images here, or</p>
          <button type="button" className="btn btn-primary" onClick={() => inputRef.current?.click()}>
            Choose Photos
          </button>
          <input
            ref={inputRef}
            type="file"
            accept="image/*"
            multiple
            className="visually-hidden"
            onChange={(e) => {
              if (e.target.files?.length) handleFiles(e.target.files);
              e.target.value = "";
            }}
          />
          <p className="photos-step__dropzone-limit">
            {remainingSlots} of {MAX_PHOTOS} photos remaining
          </p>
        </div>
      )}

      {validationError && (
        <p className="post-ad-form__error photos-step__validation-error">
          <FaExclamationCircle aria-hidden="true" /> {validationError}
        </p>
      )}

      {existingMedia.length > 0 && (
        <div className="photos-step__section">
          <p className="photos-step__section-title">Current photos</p>
          <div className="photos-step__grid">
            {existingMedia.map((media, index) => (
              <div key={media.id} className="photos-step__thumb">
                <img src={resolveMediaUrl(media.url)} alt="" />
                {index === 0 && <span className="photos-step__cover-badge">Cover</span>}
              </div>
            ))}
          </div>
          <p className="post-ad-form__hint">Existing photos can't be removed yet — add new ones below.</p>
        </div>
      )}

      {pendingPhotos.length > 0 && (
        <div className="photos-step__section">
          {existingMedia.length > 0 && <p className="photos-step__section-title">New photos</p>}
          <div className="photos-step__grid">
            {pendingPhotos.map((photo, index) => (
              <div key={photo.localId} className="photos-step__thumb">
                <img src={photo.previewUrl} alt="" />
                {existingMedia.length === 0 && index === 0 && <span className="photos-step__cover-badge">Cover</span>}

                {photo.status === "uploading" && (
                  <div className="photos-step__progress">
                    <div className="photos-step__progress-bar" style={{ width: `${photo.progress}%` }} />
                  </div>
                )}

                {photo.status === "error" && (
                  <div className="photos-step__thumb-error" title={photo.error}>
                    <FaExclamationCircle aria-hidden="true" />
                    {onRetryPhoto && (
                      <button type="button" onClick={() => onRetryPhoto(photo.localId)} aria-label="Retry upload">
                        <FaRedo aria-hidden="true" />
                      </button>
                    )}
                  </div>
                )}

                {photo.status !== "uploading" && (
                  <button type="button" className="photos-step__remove" onClick={() => onRemovePhoto(photo.localId)} aria-label="Remove photo">
                    <FaTimes aria-hidden="true" />
                  </button>
                )}
              </div>
            ))}
          </div>
        </div>
      )}

      {existingMedia.length === 0 && pendingPhotos.length === 0 && (
        <p className="post-ad-form__hint">No photos added yet — you can still post without one, but ads with photos perform better.</p>
      )}
    </div>
  );
}
