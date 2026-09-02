import { describe, expect, it, vi, beforeEach } from "vitest";
import { render, screen, waitFor, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { MemoryRouter } from "react-router-dom";
import { AdminPromotionsPage } from "./AdminPromotionsPage";
import { ToastProvider } from "../../components/Toast/ToastProvider";
import type { PaymentResponse, PaymentSummaryResponse, PromotionResponse } from "../../types/api";

vi.mock("../../api/adminPromotionApi", () => ({
  listPromotions: vi.fn(),
  cancelPromotionAsAdmin: vi.fn(),
  approvePromotion: vi.fn(),
  listPromotionSlots: vi.fn(),
  listAllPromotionPlans: vi.fn(),
  uploadBannerMedia: vi.fn(),
  createPromotion: vi.fn(),
}));
vi.mock("../../api/adminPaymentApi", () => ({
  listPayments: vi.fn(),
  getPayment: vi.fn(),
  approvePayment: vi.fn(),
  rejectPayment: vi.fn(),
}));
vi.mock("../../api/adminApi", () => ({
  listCustomers: vi.fn(),
  listCustomerActiveAds: vi.fn(),
}));

import * as adminPromotionApi from "../../api/adminPromotionApi";
import * as adminPaymentApi from "../../api/adminPaymentApi";

function basePromotion(overrides: Partial<PromotionResponse> = {}): PromotionResponse {
  return {
    id: 1,
    kind: "AD_PROMOTION",
    adId: 10,
    adTitle: "Toyota Prius 2015",
    customerId: 100,
    customerDisplayName: "Nimal Perera",
    promotionPlanId: 5,
    promotionPlanCode: "FEATURED_7D",
    promotionPlanName: "Featured 7 Days",
    slotId: 2,
    slotCode: "FEATURED",
    placementType: "CATEGORY_FEATURED",
    bannerMediaUrl: null,
    targetUrl: null,
    price: 1500,
    durationDays: 7,
    paymentRequired: true,
    paymentWaived: false,
    status: "PENDING_PAYMENT",
    createdAt: "2026-08-20T10:00:00Z",
    startsAt: null,
    endsAt: null,
    ...overrides,
  } as PromotionResponse;
}

function basePaymentSummary(overrides: Partial<PaymentSummaryResponse> = {}): PaymentSummaryResponse {
  return {
    id: 50,
    paymentReference: "PAY-0050",
    promotionId: 1,
    adId: 10,
    adTitle: "Toyota Prius 2015",
    promotionPlanName: "Featured 7 Days",
    customerId: 100,
    customerDisplayName: "Nimal Perera",
    amount: 1500,
    paymentMethod: "BANK_TRANSFER",
    status: "PENDING",
    bankReference: null,
    submittedAt: null,
    createdAt: "2026-08-20T10:00:00Z",
    ...overrides,
  };
}

function fullPayment(overrides: Partial<PaymentResponse> = {}): PaymentResponse {
  return {
    id: 50,
    paymentReference: "PAY-0050",
    promotionId: 1,
    adId: 10,
    adTitle: "Toyota Prius 2015",
    promotionPlanName: "Featured 7 Days",
    placementType: "CATEGORY_FEATURED",
    customerId: 100,
    customerDisplayName: "Nimal Perera",
    customerPhone: null,
    customerEmail: "nimal@example.com",
    amount: 1500,
    paymentMethod: "BANK_TRANSFER",
    status: "PENDING",
    bankReference: null,
    receiptUrl: null,
    customerNote: null,
    adminNote: null,
    submittedAt: null,
    reviewedAt: null,
    createdAt: "2026-08-20T10:00:00Z",
    updatedAt: "2026-08-20T10:00:00Z",
    ...overrides,
  };
}

function renderPage() {
  return render(
    <MemoryRouter>
      <ToastProvider>
        <AdminPromotionsPage />
      </ToastProvider>
    </MemoryRouter>,
  );
}

beforeEach(() => {
  vi.clearAllMocks();
});

describe("AdminPromotionsPage — Verify Payment action", () => {
  it("shows Verify Payment (and Cancel) for a PENDING_PAYMENT promotion with a PENDING payment", async () => {
    vi.mocked(adminPromotionApi.listPromotions).mockResolvedValue([basePromotion()]);
    vi.mocked(adminPaymentApi.listPayments).mockResolvedValue([basePaymentSummary()]);

    renderPage();

    const row = (await screen.findByText("Nimal Perera")).closest("tr")!;
    expect(within(row).getByRole("button", { name: "Verify Payment" })).toBeInTheDocument();
    expect(within(row).getByRole("button", { name: "Cancel" })).toBeInTheDocument();
  });

  it("shows Verify Payment even when the payment has no uploaded proof", async () => {
    vi.mocked(adminPromotionApi.listPromotions).mockResolvedValue([basePromotion()]);
    vi.mocked(adminPaymentApi.listPayments).mockResolvedValue([basePaymentSummary({ status: "PENDING" })]);
    vi.mocked(adminPaymentApi.getPayment).mockResolvedValue(fullPayment({ receiptUrl: null, status: "PENDING" }));

    renderPage();
    const row = (await screen.findByText("Nimal Perera")).closest("tr")!;
    await userEvent.click(within(row).getByRole("button", { name: "Verify Payment" }));

    expect(await screen.findByText("No receipt uploaded.")).toBeInTheDocument();
  });

  it("clicking Verify Payment opens the existing AdminPaymentReviewModal", async () => {
    vi.mocked(adminPromotionApi.listPromotions).mockResolvedValue([basePromotion()]);
    vi.mocked(adminPaymentApi.listPayments).mockResolvedValue([basePaymentSummary()]);
    vi.mocked(adminPaymentApi.getPayment).mockResolvedValue(fullPayment());

    renderPage();
    const row = (await screen.findByText("Nimal Perera")).closest("tr")!;
    await userEvent.click(within(row).getByRole("button", { name: "Verify Payment" }));

    expect(await screen.findByRole("dialog", { name: "Review Payment" })).toBeInTheDocument();
    expect(adminPaymentApi.getPayment).toHaveBeenCalledWith(50);
  });

  it("verifies a Cash payment and refreshes the row from the backend response", async () => {
    vi.mocked(adminPromotionApi.listPromotions)
      .mockResolvedValueOnce([basePromotion()])
      .mockResolvedValueOnce([basePromotion({ status: "ACTIVE" })]);
    vi.mocked(adminPaymentApi.listPayments)
      .mockResolvedValueOnce([basePaymentSummary()])
      .mockResolvedValueOnce([basePaymentSummary({ status: "APPROVED" })]);
    vi.mocked(adminPaymentApi.getPayment).mockResolvedValue(fullPayment());
    vi.mocked(adminPaymentApi.approvePayment).mockResolvedValue(fullPayment({ status: "APPROVED" }));

    renderPage();
    const row = (await screen.findByText("Nimal Perera")).closest("tr")!;
    await userEvent.click(within(row).getByRole("button", { name: "Verify Payment" }));
    await screen.findByRole("dialog", { name: "Review Payment" });

    await userEvent.click(screen.getByRole("button", { name: "Verify Payment (No Proof / Manual)" }));
    const methodSelect = screen.getByLabelText("Payment method") as HTMLSelectElement;
    expect(methodSelect.value).toBe("CASH");
    await userEvent.click(screen.getByRole("button", { name: "Verify & Activate" }));
    const confirmDialog = await screen.findByRole("dialog", { name: "Verify this payment?" });
    await userEvent.click(within(confirmDialog).getByRole("button", { name: "Verify & Activate" }));

    await waitFor(() =>
      expect(adminPaymentApi.approvePayment).toHaveBeenCalledWith(50, { paymentMethod: "CASH", adminNote: undefined }),
    );
    expect(await screen.findByText("Payment approved and promotion activated.")).toBeInTheDocument();
    await waitFor(() => expect(adminPromotionApi.listPromotions).toHaveBeenCalledTimes(2));
  });

  it("verifies a Bank Transfer payment without proof", async () => {
    vi.mocked(adminPromotionApi.listPromotions).mockResolvedValue([basePromotion()]);
    vi.mocked(adminPaymentApi.listPayments).mockResolvedValue([basePaymentSummary()]);
    vi.mocked(adminPaymentApi.getPayment).mockResolvedValue(fullPayment({ receiptUrl: null }));
    vi.mocked(adminPaymentApi.approvePayment).mockResolvedValue(fullPayment({ status: "APPROVED" }));

    renderPage();
    const row = (await screen.findByText("Nimal Perera")).closest("tr")!;
    await userEvent.click(within(row).getByRole("button", { name: "Verify Payment" }));
    await screen.findByRole("dialog", { name: "Review Payment" });

    await userEvent.click(screen.getByRole("button", { name: "Verify Payment (No Proof / Manual)" }));
    await userEvent.selectOptions(screen.getByLabelText("Payment method"), "BANK_TRANSFER");
    await userEvent.click(screen.getByRole("button", { name: "Verify & Activate" }));
    const confirmDialog = await screen.findByRole("dialog", { name: "Verify this payment?" });
    await userEvent.click(within(confirmDialog).getByRole("button", { name: "Verify & Activate" }));

    await waitFor(() =>
      expect(adminPaymentApi.approvePayment).toHaveBeenCalledWith(50, {
        paymentMethod: "BANK_TRANSFER",
        adminNote: undefined,
      }),
    );
  });

  it("verifies an Other-method payment with an admin note", async () => {
    vi.mocked(adminPromotionApi.listPromotions).mockResolvedValue([basePromotion()]);
    vi.mocked(adminPaymentApi.listPayments).mockResolvedValue([basePaymentSummary()]);
    vi.mocked(adminPaymentApi.getPayment).mockResolvedValue(fullPayment());
    vi.mocked(adminPaymentApi.approvePayment).mockResolvedValue(fullPayment({ status: "APPROVED" }));

    renderPage();
    const row = (await screen.findByText("Nimal Perera")).closest("tr")!;
    await userEvent.click(within(row).getByRole("button", { name: "Verify Payment" }));
    await screen.findByRole("dialog", { name: "Review Payment" });

    await userEvent.click(screen.getByRole("button", { name: "Verify Payment (No Proof / Manual)" }));
    await userEvent.selectOptions(screen.getByLabelText("Payment method"), "OTHER");
    await userEvent.type(screen.getByLabelText("Note (optional)"), "Paid via mobile wallet");
    await userEvent.click(screen.getByRole("button", { name: "Verify & Activate" }));
    const confirmDialog = await screen.findByRole("dialog", { name: "Verify this payment?" });
    await userEvent.click(within(confirmDialog).getByRole("button", { name: "Verify & Activate" }));

    await waitFor(() =>
      expect(adminPaymentApi.approvePayment).toHaveBeenCalledWith(50, {
        paymentMethod: "OTHER",
        adminNote: "Paid via mobile wallet",
      }),
    );
  });

  it("does not show Verify Payment for a PENDING_APPROVAL row", async () => {
    vi.mocked(adminPromotionApi.listPromotions).mockResolvedValue([
      basePromotion({ status: "PENDING_APPROVAL" }),
    ]);
    vi.mocked(adminPaymentApi.listPayments).mockResolvedValue([basePaymentSummary({ status: "APPROVED" })]);

    renderPage();
    const row = (await screen.findByText("Nimal Perera")).closest("tr")!;
    expect(within(row).queryByRole("button", { name: "Verify Payment" })).not.toBeInTheDocument();
    expect(within(row).getByRole("button", { name: "Approve" })).toBeInTheDocument();
    expect(within(row).getByRole("button", { name: "Reject" })).toBeInTheDocument();
  });

  it("does not show Verify Payment for an ACTIVE row", async () => {
    vi.mocked(adminPromotionApi.listPromotions).mockResolvedValue([basePromotion({ status: "ACTIVE" })]);
    vi.mocked(adminPaymentApi.listPayments).mockResolvedValue([basePaymentSummary({ status: "APPROVED" })]);

    renderPage();
    const row = (await screen.findByText("Nimal Perera")).closest("tr")!;
    expect(within(row).queryByRole("button", { name: "Verify Payment" })).not.toBeInTheDocument();
    expect(within(row).getByRole("button", { name: "Cancel" })).toBeInTheDocument();
  });

  it("leaves Cancel behavior unchanged for a PENDING_PAYMENT row", async () => {
    vi.mocked(adminPromotionApi.listPromotions).mockResolvedValue([basePromotion()]);
    vi.mocked(adminPaymentApi.listPayments).mockResolvedValue([basePaymentSummary()]);
    vi.mocked(adminPromotionApi.cancelPromotionAsAdmin).mockResolvedValue(
      basePromotion({ status: "CANCELLED" }),
    );

    renderPage();
    const row = (await screen.findByText("Nimal Perera")).closest("tr")!;
    await userEvent.click(within(row).getByRole("button", { name: "Cancel" }));

    expect(await screen.findByText("Cancel this promotion?")).toBeInTheDocument();
    await userEvent.click(screen.getByRole("button", { name: "Cancel Promotion" }));

    await waitFor(() => expect(adminPromotionApi.cancelPromotionAsAdmin).toHaveBeenCalledWith(1));
    expect(await screen.findByText("Promotion cancelled.")).toBeInTheDocument();
  });
});
