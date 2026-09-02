import { describe, expect, it, vi, beforeEach } from "vitest";
import { render, screen, waitFor, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { MemoryRouter } from "react-router-dom";
import { PostAdWizard } from "./PostAdWizard";
import type { AdFormValues } from "./types";
import type { AdResponse, MediaResponse } from "../../types/api";

vi.mock("../../api/adsApi", () => ({ createAd: vi.fn(), updateAd: vi.fn() }));
vi.mock("../../api/mediaApi", () => ({ uploadAdMedia: vi.fn() }));
vi.mock("../../api/categoryApi", () => ({ listCategories: vi.fn(), getCategoryAttributes: vi.fn() }));
vi.mock("../../api/locationApi", () => ({ listLocations: vi.fn() }));
vi.mock("../../api/customerApi", () => ({ getMyProfile: vi.fn() }));

import { createAd, updateAd } from "../../api/adsApi";
import { uploadAdMedia } from "../../api/mediaApi";
import { listCategories, getCategoryAttributes } from "../../api/categoryApi";
import { listLocations } from "../../api/locationApi";
import { getMyProfile } from "../../api/customerApi";

const mockCreateAd = vi.mocked(createAd);
const mockUpdateAd = vi.mocked(updateAd);
const mockUploadAdMedia = vi.mocked(uploadAdMedia);

beforeEach(() => {
  vi.clearAllMocks();
  vi.mocked(listCategories).mockResolvedValue([]);
  vi.mocked(getCategoryAttributes).mockResolvedValue([]);
  vi.mocked(listLocations).mockResolvedValue([]);
  vi.mocked(getMyProfile).mockRejectedValue(new Error("not mocked"));
  URL.createObjectURL = vi.fn(() => "blob:mock-preview");
  URL.revokeObjectURL = vi.fn();
});

const editValues: AdFormValues = {
  categorySlug: "cars",
  categoryPath: "Vehicles > Cars",
  title: "1998 Toyota Corolla",
  description: "Well maintained.",
  price: "1500000",
  locationSlugs: [],
  attributes: {},
  contactName: "",
  phoneNumber: "",
  whatsappNumber: "",
  whatsappSameAsPhone: true,
};

const existingPhoto: MediaResponse = { id: 1, url: "/media/existing.jpg", contentType: "image/jpeg", displayOrder: 0 };

function renderEdit(initialMedia: MediaResponse[] = [existingPhoto]) {
  render(
    <MemoryRouter>
      <PostAdWizard mode="edit" adId={42} initialValues={editValues} initialMedia={initialMedia} />
    </MemoryRouter>,
  );
}

function pngFile(name = "photo.png") {
  return new File(["fake-bytes"], name, { type: "image/png" });
}

async function addPhotoViaEditButton() {
  // Review's Photos row has an "Edit" button that jumps to the Photos step.
  const photoRow = screen.getByText(/^Photos \(/).closest(".review-step__row") as HTMLElement;
  await userEvent.click(within(photoRow).getByRole("button", { name: /edit/i }));
  const input = document.querySelector('input[type="file"]') as HTMLInputElement;
  await userEvent.upload(input, pngFile());
  // Back to Review via the stepper (all steps are reachable in edit mode).
  await userEvent.click(screen.getByRole("button", { name: /review/i }));
}

describe("PostAdWizard - Edit Ad photo upload", () => {
  it("never treats existing persisted media as a pending/new photo", () => {
    renderEdit();
    // Only the persisted photo is shown; no upload attempted without any interaction.
    expect(mockUploadAdMedia).not.toHaveBeenCalled();
  });

  it("saves text-only changes without triggering any media upload", async () => {
    mockUpdateAd.mockResolvedValue({ id: 42 } as AdResponse);
    renderEdit();

    await userEvent.click(screen.getByRole("button", { name: /save changes/i }));

    await waitFor(() => expect(mockUpdateAd).toHaveBeenCalledWith(42, expect.any(Object)));
    expect(mockUploadAdMedia).not.toHaveBeenCalled();
  });

  it("uploads exactly one file when a photo is added, using the persisted ad id", async () => {
    mockUpdateAd.mockResolvedValue({ id: 42 } as AdResponse);
    mockUploadAdMedia.mockResolvedValue({ id: 2, url: "/media/new.jpg", contentType: "image/png", displayOrder: 1 });
    renderEdit();

    await addPhotoViaEditButton();
    await userEvent.click(screen.getByRole("button", { name: /save changes/i }));

    await waitFor(() => expect(mockUploadAdMedia).toHaveBeenCalledTimes(1));
    expect(mockUploadAdMedia).toHaveBeenCalledWith(42, expect.any(File), expect.any(Function));
    expect(mockUploadAdMedia.mock.calls[0][1].name).toBe("photo.png");
  });

  it("does not re-upload a photo that already succeeded", async () => {
    mockUpdateAd.mockResolvedValue({ id: 42 } as AdResponse);
    mockUploadAdMedia.mockResolvedValue({ id: 2, url: "/media/new.jpg", contentType: "image/png", displayOrder: 1 });
    renderEdit();

    await addPhotoViaEditButton();
    await userEvent.click(screen.getByRole("button", { name: /save changes/i }));
    await waitFor(() => expect(mockUploadAdMedia).toHaveBeenCalledTimes(1));

    // Saving again (e.g. a second click/effect) must not resend the already-uploaded file.
    await userEvent.click(screen.getByRole("button", { name: /save changes/i }));
    await waitFor(() => expect(screen.queryByRole("button", { name: /submitting/i })).not.toBeInTheDocument());
    expect(mockUploadAdMedia).toHaveBeenCalledTimes(1);
  });

  it("shows no failure banner after a successful upload", async () => {
    mockUpdateAd.mockResolvedValue({ id: 42 } as AdResponse);
    mockUploadAdMedia.mockResolvedValue({ id: 2, url: "/media/new.jpg", contentType: "image/png", displayOrder: 1 });
    renderEdit();

    await addPhotoViaEditButton();
    await userEvent.click(screen.getByRole("button", { name: /save changes/i }));

    await waitFor(() => expect(mockUploadAdMedia).toHaveBeenCalledTimes(1));
    expect(screen.queryByText(/failed to upload/i)).not.toBeInTheDocument();
  });

  it("marks a photo failed only when the upload request actually rejects, and retry resends only that file", async () => {
    mockUpdateAd.mockResolvedValue({ id: 42 } as AdResponse);
    mockUploadAdMedia.mockRejectedValueOnce(new Error("network error"));
    renderEdit();

    await addPhotoViaEditButton();
    await userEvent.click(screen.getByRole("button", { name: /save changes/i }));

    await waitFor(() => expect(screen.getByText(/1 photo\(s\) failed to upload/i)).toBeInTheDocument());
    expect(mockUploadAdMedia).toHaveBeenCalledTimes(1);

    // Retry from the Photos step should resend only the failed file.
    mockUploadAdMedia.mockResolvedValueOnce({ id: 3, url: "/media/retry.jpg", contentType: "image/png", displayOrder: 1 });
    const photoRowAgain = screen.getByText(/^Photos \(/).closest(".review-step__row") as HTMLElement;
    await userEvent.click(within(photoRowAgain).getByRole("button", { name: /edit/i }));
    await userEvent.click(screen.getByRole("button", { name: /retry upload/i }));

    await waitFor(() => expect(mockUploadAdMedia).toHaveBeenCalledTimes(2));
  });
});

describe("PostAdWizard - Post Ad (create) photo upload", () => {
  it("creates the ad, then uploads the selected photo against the newly persisted ad id, with no failure message", async () => {
    mockCreateAd.mockResolvedValue({ id: 99 } as AdResponse);
    mockUploadAdMedia.mockResolvedValue({ id: 5, url: "/media/created.jpg", contentType: "image/png", displayOrder: 0 });

    render(
      <MemoryRouter>
        <PostAdWizard mode="create" initialValues={editValues} />
      </MemoryRouter>,
    );

    // Pre-filled valid values let Continue proceed through Category -> Details -> Location -> Photos.
    for (let i = 0; i < 3; i++) {
      await userEvent.click(screen.getByRole("button", { name: "Continue" }));
    }

    const input = document.querySelector('input[type="file"]') as HTMLInputElement;
    await userEvent.upload(input, pngFile("cover.png"));
    await userEvent.click(screen.getByRole("button", { name: "Continue" }));

    await userEvent.click(screen.getByRole("button", { name: /submit ad/i }));

    await waitFor(() => expect(mockCreateAd).toHaveBeenCalledTimes(1));
    await waitFor(() => expect(mockUploadAdMedia).toHaveBeenCalledWith(99, expect.any(File), expect.any(Function)));
    expect(screen.queryByText(/failed to upload/i)).not.toBeInTheDocument();
  });
});
