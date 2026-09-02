import { describe, expect, it, vi } from "vitest";
import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { LocationStep } from "./LocationStep";
import type { CategoryResponse, LocationResponse } from "../../../types/api";

function location(overrides: Partial<LocationResponse>): LocationResponse {
  return { id: 0, name: "", slug: "", type: "PROVINCE", parentId: null, ...overrides };
}

const locations: LocationResponse[] = [
  location({ id: 1, name: "Central", slug: "central", type: "PROVINCE", parentId: null }),
  location({ id: 2, name: "Kandy District", slug: "kandy-district", type: "DISTRICT", parentId: 1 }),
  location({ id: 3, name: "Kandy", slug: "kandy", type: "CITY", parentId: 2 }),
  location({ id: 4, name: "Katugastota", slug: "katugastota", type: "CITY", parentId: 2 }),
  location({ id: 5, name: "Peradeniya", slug: "peradeniya", type: "CITY", parentId: 2 }),
];

const categories: CategoryResponse[] = [
  { id: 1, name: "Education & Tuition", slug: "education-tuition", parentId: null, displayOrder: 0, active: true },
  { id: 2, name: "School Tuition", slug: "school-tuition", parentId: 1, displayOrder: 0, active: true },
];

function renderStep(props: Partial<React.ComponentProps<typeof LocationStep>> = {}) {
  const onChange = vi.fn();
  render(
    <LocationStep
      locations={locations}
      loading={false}
      error={null}
      locationSlugs={[]}
      onChange={onChange}
      categories={categories}
      categorySlug="school-tuition"
      attributeValues={{}}
      {...props}
    />,
  );
  return { onChange };
}

describe("LocationStep", () => {
  it("does not render the location hierarchy by default", () => {
    renderStep();
    expect(screen.queryByText("Kandy District")).not.toBeInTheDocument();
    expect(screen.queryByText("Katugastota")).not.toBeInTheDocument();
    expect(screen.getByText("Browse locations")).toBeInTheDocument();
  });

  it("expands the hierarchy only after Browse locations is opened", async () => {
    renderStep();
    await userEvent.click(screen.getByText("Browse locations"));
    expect(screen.getByText("Central")).toBeInTheDocument();
  });

  it("search finds and selects a location", async () => {
    const { onChange } = renderStep();
    await userEvent.type(screen.getByPlaceholderText("Search city or area…"), "Kandy");

    expect(screen.getByText("Kandy")).toBeInTheDocument();
    await userEvent.click(screen.getByText("Kandy"));

    expect(onChange).toHaveBeenCalledWith(["kandy"]);
  });

  it("supports selecting multiple locations without resetting earlier ones", async () => {
    const { onChange } = renderStep({ locationSlugs: ["kandy"] });
    await userEvent.type(screen.getByPlaceholderText("Search city or area…"), "Peradeniya");
    await userEvent.click(screen.getByText("Peradeniya"));

    expect(onChange).toHaveBeenCalledWith(["kandy", "peradeniya"]);
  });

  it("shows selected locations as removable chips", () => {
    renderStep({ locationSlugs: ["kandy", "peradeniya"] });
    expect(screen.getByText("Kandy")).toBeInTheDocument();
    expect(screen.getByText("Peradeniya")).toBeInTheDocument();
  });

  it("removes a selected location when its chip is clicked", async () => {
    const { onChange } = renderStep({ locationSlugs: ["kandy", "peradeniya"] });
    await userEvent.click(screen.getByText("Kandy"));

    expect(onChange).toHaveBeenCalledWith(["peradeniya"]);
  });

  it("does not offer an already-selected location again in search results", async () => {
    renderStep({ locationSlugs: ["kandy"] });
    await userEvent.type(screen.getByPlaceholderText("Search city or area…"), "Kandy");

    // Only the removable chip should show "Kandy"; it must not also appear as a search result.
    expect(screen.getAllByText("Kandy")).toHaveLength(1);
  });

  it("adding a location through Browse adds to the existing selection", async () => {
    const { onChange } = renderStep({ locationSlugs: ["peradeniya"] });
    await userEvent.click(screen.getByText("Browse locations"));
    await userEvent.click(screen.getByText("Central"));
    await userEvent.click(screen.getByText("Kandy District"));
    await userEvent.click(screen.getByText("Katugastota"));

    expect(onChange).toHaveBeenCalledWith(["peradeniya", "katugastota"]);
  });
});
