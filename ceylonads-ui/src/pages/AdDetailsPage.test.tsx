import { describe, expect, it, vi, beforeEach } from "vitest";
import { render, screen } from "@testing-library/react";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import { AxiosError } from "axios";
import { AdDetailsPage } from "./AdDetailsPage";

vi.mock("../api/adsApi", () => ({ getAd: vi.fn() }));
vi.mock("../api/categoryApi", () => ({ listCategories: vi.fn() }));

import { getAd } from "../api/adsApi";
import { listCategories } from "../api/categoryApi";

function renderAtSlug(slug: string) {
  return render(
    <MemoryRouter initialEntries={[`/ads/${slug}`]}>
      <Routes>
        <Route path="/ads/:slug" element={<AdDetailsPage />} />
      </Routes>
    </MemoryRouter>,
  );
}

function notFoundError(): AxiosError {
  return new AxiosError("Not Found", "404", undefined, undefined, {
    status: 404,
    data: { status: 404, error: "Not Found", message: "Ad not found" },
    // Fields below aren't read by the component; only response.status is checked.
    statusText: "Not Found",
    headers: {},
    config: {} as never,
  });
}

function serverError(): AxiosError {
  return new AxiosError("Internal Server Error", "500", undefined, undefined, {
    status: 500,
    data: { status: 500, error: "Internal Server Error", message: "Something broke" },
    statusText: "Internal Server Error",
    headers: {},
    config: {} as never,
  });
}

beforeEach(() => {
  vi.mocked(listCategories).mockResolvedValue([]);
});

describe("AdDetailsPage", () => {
  it("shows a dedicated Ad Not Found state with a Browse Ads link for a 404 from the public ad API", async () => {
    vi.mocked(getAd).mockRejectedValue(notFoundError());

    renderAtSlug("something-that-does-not-exist-999999");

    expect(await screen.findByText("Ad not found")).toBeInTheDocument();
    expect(
      screen.getByText("This ad may have been removed, expired, or the link may be incorrect."),
    ).toBeInTheDocument();
    const browseLink = screen.getByRole("link", { name: "Browse Ads" });
    expect(browseLink).toHaveAttribute("href", "/ads");
  });

  it("shows the general error state (not Ad Not Found) for an unexpected failure like a 500", async () => {
    vi.mocked(getAd).mockRejectedValue(serverError());

    renderAtSlug("some-real-ad-1");

    expect(await screen.findByText("Something went wrong")).toBeInTheDocument();
    expect(screen.queryByText("Ad not found")).not.toBeInTheDocument();
    expect(screen.queryByRole("link", { name: "Browse Ads" })).not.toBeInTheDocument();
  });

  it("shows the general error state for a network failure with no response", async () => {
    vi.mocked(getAd).mockRejectedValue(new AxiosError("Network Error"));

    renderAtSlug("some-real-ad-1");

    expect(await screen.findByText("Something went wrong")).toBeInTheDocument();
    expect(screen.queryByText("Ad not found")).not.toBeInTheDocument();
  });
});
