import { describe, expect, it } from "vitest";
import { canContinueFromStep, normalizePrice, validateDetails } from "./PostAdWizard";
import { EMPTY_FORM_VALUES, STEPS, type AdFormValues } from "./types";

function values(overrides: Partial<AdFormValues>): AdFormValues {
  return { ...EMPTY_FORM_VALUES, title: "A valid title", ...overrides };
}

describe("validateDetails - price", () => {
  it("blank price does not block continue", () => {
    expect(validateDetails(values({ price: "" }))).not.toHaveProperty("price");
  });

  it("price 0 does not block continue", () => {
    expect(validateDetails(values({ price: "0" }))).not.toHaveProperty("price");
  });

  it("a positive price does not block continue", () => {
    expect(validateDetails(values({ price: "1500" }))).not.toHaveProperty("price");
  });

  it("negative price is invalid", () => {
    expect(validateDetails(values({ price: "-5" }))).toHaveProperty("price");
  });

  it("non-numeric price is invalid", () => {
    expect(validateDetails(values({ price: "abc" }))).toHaveProperty("price");
  });
});

describe("validateDetails - description", () => {
  it("blank description does not block continue", () => {
    expect(validateDetails(values({ description: "" }))).not.toHaveProperty("description");
  });

  it("a 5000-character description is valid", () => {
    expect(validateDetails(values({ description: "a".repeat(5000) }))).not.toHaveProperty("description");
  });

  it("a 5001-character description is invalid", () => {
    expect(validateDetails(values({ description: "a".repeat(5001) }))).toHaveProperty("description");
  });
});

describe("validateDetails - title", () => {
  it("still requires a title", () => {
    expect(validateDetails(values({ title: "" }))).toHaveProperty("title");
  });
});

describe("canContinueFromStep - location", () => {
  const locationStep = STEPS.find((s) => s.key === "location")!;

  it("zero locations does not block continue for an online-tuition-style category", () => {
    expect(canContinueFromStep(locationStep, values({ locationSlugs: [] }), [])).toBe(true);
  });

  it("zero locations does not block continue for a category where location is optional", () => {
    expect(canContinueFromStep(locationStep, values({ categorySlug: "cars", locationSlugs: [] }), [])).toBe(true);
  });
});

describe("normalizePrice", () => {
  it("normalizes a blank price to 0", () => {
    expect(normalizePrice("")).toBe(0);
    expect(normalizePrice("   ")).toBe(0);
  });

  it("normalizes 0 to 0", () => {
    expect(normalizePrice("0")).toBe(0);
  });

  it("passes through a positive price", () => {
    expect(normalizePrice("1500")).toBe(1500);
  });
});
