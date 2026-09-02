import { describe, expect, it, vi } from "vitest";
import { render, screen } from "@testing-library/react";
import { ReviewStep } from "./ReviewStep";
import { EMPTY_FORM_VALUES, type AdFormValues } from "../types";
import type { AttributeDefinitionResponse, LocationResponse } from "../../../types/api";

const locations: LocationResponse[] = [
  { id: 1, name: "Kandy", slug: "kandy", type: "CITY", parentId: null },
  { id: 2, name: "Peradeniya", slug: "peradeniya", type: "CITY", parentId: null },
];

const multiSelectAttribute: AttributeDefinitionResponse = {
  id: 1,
  categoryId: 1,
  key: "features",
  name: "Features",
  dataType: "MULTI_SELECT",
  required: false,
  filterable: true,
  unit: null,
  displayOrder: 0,
  active: true,
  options: [
    { id: 1, value: "ac", label: "Air Conditioning", displayOrder: 0, active: true },
    { id: 2, value: "sunroof", label: "Sunroof", displayOrder: 1, active: true },
    { id: 3, value: "abs", label: "ABS", displayOrder: 2, active: true },
  ],
};

function values(overrides: Partial<AdFormValues>): AdFormValues {
  return { ...EMPTY_FORM_VALUES, categoryPath: "Cars", title: "My Ad", ...overrides };
}

function renderReview(overrides: Partial<AdFormValues>, attributeDefinitions: AttributeDefinitionResponse[] = []) {
  render(
    <ReviewStep
      values={values(overrides)}
      attributeDefinitions={attributeDefinitions}
      locations={locations}
      existingMedia={[]}
      pendingPhotos={[]}
      mode="create"
      submitting={false}
      submitError={null}
      onEditStep={vi.fn()}
      onSubmit={vi.fn()}
    />,
  );
}

describe("ReviewStep - optional values", () => {
  it("shows Contact for price for a blank price", () => {
    renderReview({ price: "" });
    expect(screen.getByText("Contact for price")).toBeInTheDocument();
    expect(screen.queryByText(/Rs\.\s*0/)).not.toBeInTheDocument();
  });

  it("shows Contact for price for a price of 0", () => {
    renderReview({ price: "0" });
    expect(screen.getByText("Contact for price")).toBeInTheDocument();
  });

  it("shows a formatted price for a positive value", () => {
    renderReview({ price: "1500" });
    expect(screen.getByText("Rs. 1,500")).toBeInTheDocument();
  });

  it("omits the Description row when description is blank", () => {
    renderReview({ description: "" });
    expect(screen.queryByText("Description")).not.toBeInTheDocument();
  });

  it("shows the Description row when description is filled", () => {
    renderReview({ description: "A great item." });
    expect(screen.getByText("Description")).toBeInTheDocument();
    expect(screen.getByText("A great item.")).toBeInTheDocument();
  });

  it("omits the Location row when no locations are selected", () => {
    renderReview({ locationSlugs: [] });
    expect(screen.queryByText("Location")).not.toBeInTheDocument();
  });

  it("shows selected locations", () => {
    renderReview({ locationSlugs: ["kandy"] });
    expect(screen.getByText("Location")).toBeInTheDocument();
    expect(screen.getByText("Kandy")).toBeInTheDocument();
  });

  it("shows all selected locations when multiple are chosen", () => {
    renderReview({ locationSlugs: ["kandy", "peradeniya"] });
    expect(screen.getByText("Kandy, Peradeniya")).toBeInTheDocument();
  });

  it("shows every selected value for a multi-select attribute", () => {
    renderReview({ attributes: { features: "ac,sunroof,abs" } }, [multiSelectAttribute]);
    expect(screen.getByText("Features: Air Conditioning, Sunroof, ABS")).toBeInTheDocument();
  });
});
