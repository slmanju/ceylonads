import { describe, expect, it, vi, beforeEach } from "vitest";
import { render, screen, waitFor, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { MemoryRouter, useSearchParams } from "react-router-dom";
import { AdSearchResults } from "./AdSearchResults";
import type { AdResponse, AdSearchParams, CategoryResponse, LocationResponse, PageResponse } from "../../types/api";

vi.mock("../../api/adsApi", () => ({ searchAds: vi.fn(), searchTuitionClasses: vi.fn() }));
vi.mock("../../api/categoryApi", () => ({ listCategories: vi.fn(), getCategoryFilters: vi.fn() }));
vi.mock("../../api/locationApi", () => ({ listLocations: vi.fn() }));

import { searchAds, searchTuitionClasses } from "../../api/adsApi";
import { listCategories, getCategoryFilters } from "../../api/categoryApi";
import { listLocations } from "../../api/locationApi";

const categories: CategoryResponse[] = [
  { id: 1, name: "Vehicles", slug: "vehicles", parentId: null, displayOrder: 0, active: true },
  { id: 2, name: "Cars", slug: "cars", parentId: 1, displayOrder: 0, active: true },
  { id: 3, name: "Education & Tuition", slug: "education-tuition", parentId: null, displayOrder: 0, active: true },
  { id: 4, name: "School Tuition", slug: "school-tuition", parentId: 3, displayOrder: 0, active: true },
];

const locations: LocationResponse[] = [{ id: 1, name: "Colombo", slug: "colombo", type: "CITY", parentId: null }];

function ad(overrides: Partial<AdResponse>): AdResponse {
  return {
    id: 0,
    slug: "ad",
    title: "Ad",
    description: "",
    price: 1000,
    category: "Cars",
    categorySlug: "cars",
    locations: [{ id: 1, name: "Colombo", slug: "colombo", type: "CITY", parentId: null }],
    seller: { id: 1, displayName: "Seller", phone: null },
    status: "ACTIVE",
    createdAt: new Date().toISOString(),
    publishedAt: new Date().toISOString(),
    reviewedAt: null,
    media: [],
    promoted: false,
    attributes: [],
    contact: null,
    contactOverride: null,
    ...overrides,
  };
}

const allAds: AdResponse[] = [
  ad({ id: 1, slug: "toyota-aqua", title: "Toyota Aqua", categorySlug: "cars", price: 3_500_000 }),
  ad({ id: 2, slug: "honda-fit", title: "Honda Fit", categorySlug: "cars", price: 2_800_000 }),
  ad({ id: 3, slug: "yamaha-bike", title: "Yamaha Bike", categorySlug: "vehicles", price: 500_000 }),
];

function matches(item: AdResponse, params: AdSearchParams): boolean {
  if (params.q && !item.title.toLowerCase().includes(params.q.toLowerCase())) return false;
  if (params.category && item.categorySlug !== params.category) return false;
  if (params.location && !item.locations.some((l) => l.slug === params.location)) return false;
  if (params.minPrice !== undefined && item.price < params.minPrice) return false;
  if (params.maxPrice !== undefined && item.price > params.maxPrice) return false;
  return true;
}

function page<T>(matched: T[], size: number): PageResponse<T> {
  return { content: matched.slice(0, size), page: 0, size, totalElements: matched.length, totalPages: 1, first: true, last: true };
}

function CurrentSearch() {
  const [params] = useSearchParams();
  return <div data-testid="current-search">{params.toString()}</div>;
}

function renderPage(initialPath = "/ads") {
  return render(
    <MemoryRouter initialEntries={[initialPath]}>
      <AdSearchResults />
      <CurrentSearch />
    </MemoryRouter>,
  );
}

beforeEach(() => {
  vi.mocked(listCategories).mockResolvedValue(categories);
  vi.mocked(listLocations).mockResolvedValue(locations);
  vi.mocked(getCategoryFilters).mockImplementation(async (slug: string) => ({
    category: categories.find((c) => c.slug === slug)!,
    filters: [],
  }));
  vi.mocked(searchAds).mockReset();
  vi.mocked(searchAds).mockImplementation(async (params: AdSearchParams = {}) => {
    const matched = allAds.filter((item) => matches(item, params));
    return page(matched, params.size ?? 20);
  });
  vi.mocked(searchTuitionClasses).mockReset();
});

describe("AdSearchResults", () => {
  it("searches nationwide by default without sending a location param", async () => {
    renderPage("/ads");

    await screen.findByText("Toyota Aqua");
    expect(screen.getByText("Honda Fit")).toBeInTheDocument();
    expect(screen.getByText("Yamaha Bike")).toBeInTheDocument();

    const lastCall = vi.mocked(searchAds).mock.calls.at(-1)![0];
    expect(lastCall?.location).toBeUndefined();
  });

  it("shows All Sri Lanka as the default location in the filter panel", async () => {
    renderPage("/ads");
    await screen.findByText("Toyota Aqua");
    expect(screen.getAllByText("All Sri Lanka").length).toBeGreaterThan(0);
  });

  it("does not apply draft filter edits to results until confirmed", async () => {
    const user = userEvent.setup();
    renderPage("/ads");
    await screen.findByText("Toyota Aqua");

    const sidebar = document.querySelector(".ad-search-results__sidebar") as HTMLElement;
    const minPriceInput = sidebar.querySelector("#filter-min-price") as HTMLInputElement;
    await user.type(minPriceInput, "3000000");

    // Draft edit alone must not remove Honda Fit from the still-applied result list.
    expect(screen.getByText("Honda Fit")).toBeInTheDocument();

    await user.click(within(sidebar).getByRole("button", { name: "Search" }));

    await waitFor(() => expect(screen.queryByText("Honda Fit")).not.toBeInTheDocument());
    expect(screen.getByText("Toyota Aqua")).toBeInTheDocument();
  });

  it("Reset all clears the draft back to defaults without touching applied results", async () => {
    const user = userEvent.setup();
    renderPage("/ads?minPrice=3000000");
    await screen.findByText("Toyota Aqua");
    expect(screen.queryByText("Honda Fit")).not.toBeInTheDocument();

    const sidebar = document.querySelector(".ad-search-results__sidebar") as HTMLElement;
    await user.click(within(sidebar).getByText("Reset all"));
    await user.click(within(sidebar).getByRole("button", { name: "Search" }));

    await waitFor(() => expect(screen.getByText("Honda Fit")).toBeInTheDocument());
    expect(screen.getByText("Yamaha Bike")).toBeInTheDocument();
  });

  it("initializes the search field from the URL, and applying an edit updates the URL and results", async () => {
    const user = userEvent.setup();
    renderPage("/ads?q=toyota");
    await screen.findByText("Toyota Aqua");
    expect(screen.getByTestId("current-search").textContent).toBe("q=toyota");

    const sidebar = document.querySelector(".ad-search-results__sidebar") as HTMLElement;
    const searchInput = sidebar.querySelector("#filter-search-q") as HTMLInputElement;
    expect(searchInput.value).toBe("toyota");

    await user.clear(searchInput);
    await user.type(searchInput, "honda");
    await user.click(within(sidebar).getByRole("button", { name: "Search" }));

    await waitFor(() => expect(screen.getByTestId("current-search").textContent).toBe("q=honda"));
    await waitFor(() => expect(screen.getByText("Honda Fit")).toBeInTheDocument());
    expect(screen.queryByText("Toyota Aqua")).not.toBeInTheDocument();
  });

  it("pressing Enter in the search field applies the current search state", async () => {
    const user = userEvent.setup();
    renderPage("/ads");
    await screen.findByText("Toyota Aqua");

    const sidebar = document.querySelector(".ad-search-results__sidebar") as HTMLElement;
    const searchInput = sidebar.querySelector("#filter-search-q") as HTMLInputElement;
    await user.type(searchInput, "honda{Enter}");

    await waitFor(() => expect(screen.getByTestId("current-search").textContent).toBe("q=honda"));
    await waitFor(() => expect(screen.getByText("Honda Fit")).toBeInTheDocument());
    expect(screen.queryByText("Toyota Aqua")).not.toBeInTheDocument();
  });

  it("reopening the filter drawer restores the applied state, discarding abandoned edits", async () => {
    const user = userEvent.setup();
    renderPage("/ads");
    await screen.findByText("Toyota Aqua");

    await user.click(screen.getByRole("button", { name: /^Filters$/ }));
    const drawer = document.querySelector(".filters-drawer__panel") as HTMLElement;
    const minPriceInput = within(drawer).getByPlaceholderText("Min") as HTMLInputElement;
    await user.type(minPriceInput, "999999999");
    expect(minPriceInput.value).toBe("999999999");

    await user.click(within(drawer).getByRole("button", { name: "Close filters" }));
    expect(document.querySelector(".filters-drawer__panel")).not.toBeInTheDocument();

    await user.click(screen.getByRole("button", { name: /^Filters$/ }));
    const reopenedDrawer = document.querySelector(".filters-drawer__panel") as HTMLElement;
    const reopenedMinPrice = within(reopenedDrawer).getByPlaceholderText("Min") as HTMLInputElement;
    expect(reopenedMinPrice.value).toBe("");
  });

  describe("Tuition search results", () => {
    const tuitionAds: AdResponse[] = Array.from({ length: 9 }, (_, i) =>
      ad({ id: 100 + i, slug: `tuition-class-${i}`, title: `Tuition Class ${i}`, categorySlug: "school-tuition" }),
    );

    it("uses the isolated Tuition endpoint, a 9-item page size, and the 3-column grid for a Tuition category", async () => {
      vi.mocked(searchTuitionClasses).mockResolvedValue({
        content: tuitionAds,
        page: 0,
        size: 9,
        totalElements: 11,
        totalPages: 2,
        first: true,
        last: false,
      });

      renderPage("/ads?category=school-tuition");

      await screen.findByText("Tuition Class 0");
      expect(screen.getByText("Tuition Class 8")).toBeInTheDocument();

      expect(searchAds).not.toHaveBeenCalled();
      const lastCall = vi.mocked(searchTuitionClasses).mock.calls.at(-1)![0];
      expect(lastCall?.size).toBe(9);
      expect(lastCall?.category).toBe("school-tuition");

      expect(document.querySelector(".listing-grid--tuition")).toBeInTheDocument();
    });

    it("does not use the Tuition endpoint for a non-Tuition category", async () => {
      renderPage("/ads");

      await screen.findByText("Toyota Aqua");
      expect(searchTuitionClasses).not.toHaveBeenCalled();
      expect(document.querySelector(".listing-grid--tuition")).not.toBeInTheDocument();
    });
  });
});
