import { describe, expect, it, vi, beforeEach } from "vitest";
import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { MemoryRouter, useLocation } from "react-router-dom";
import { Header } from "./Header";

const mockLogout = vi.fn();
let mockAuthState: {
  isAuthenticated: boolean;
  username: string | null;
  role: "CUSTOMER" | "ADMIN" | null;
};

vi.mock("../../auth/AuthContext", () => ({
  useAuth: () => ({
    isAuthenticated: mockAuthState.isAuthenticated,
    username: mockAuthState.username,
    role: mockAuthState.role,
    logout: mockLogout,
  }),
}));

function setViewport(isMobile: boolean) {
  window.matchMedia = vi.fn().mockImplementation((query: string) => ({
    matches: isMobile,
    media: query,
    onchange: null,
    addEventListener: vi.fn(),
    removeEventListener: vi.fn(),
    addListener: vi.fn(),
    removeListener: vi.fn(),
    dispatchEvent: vi.fn(),
  })) as unknown as typeof window.matchMedia;
}

function LocationDisplay() {
  const location = useLocation();
  return <div data-testid="location">{location.pathname}</div>;
}

function renderHeader() {
  return render(
    <MemoryRouter initialEntries={["/my-ads"]}>
      <Header />
      <LocationDisplay />
    </MemoryRouter>,
  );
}

function setAuthenticatedCustomer() {
  mockAuthState = { isAuthenticated: true, username: "kamal", role: "CUSTOMER" };
}

function setGuest() {
  mockAuthState = { isAuthenticated: false, username: null, role: null };
}

beforeEach(() => {
  mockLogout.mockReset();
  setViewport(false);
});

describe("Header - authenticated desktop", () => {
  beforeEach(setAuthenticatedCustomer);

  it("does not show the username as a standalone top-level nav item", () => {
    renderHeader();
    expect(screen.queryByText("kamal")).not.toBeInTheDocument();
  });

  it("does not show a standalone top-level Logout control", () => {
    renderHeader();
    expect(screen.queryByRole("button", { name: "Logout" })).not.toBeInTheDocument();
  });

  it("shows the Account trigger", () => {
    renderHeader();
    expect(screen.getByRole("button", { name: /account/i })).toBeInTheDocument();
  });

  it("opens the Account dropdown and displays the current username", async () => {
    const user = userEvent.setup();
    renderHeader();

    await user.click(screen.getByRole("button", { name: /account/i }));

    expect(screen.getByRole("menu")).toBeInTheDocument();
    expect(screen.getByText("kamal")).toBeInTheDocument();
  });

  it("navigates to My Ads from the dropdown and closes it", async () => {
    const user = userEvent.setup();
    renderHeader();

    await user.click(screen.getByRole("button", { name: /account/i }));
    await user.click(screen.getByRole("menuitem", { name: "My Ads" }));

    expect(screen.queryByRole("menu")).not.toBeInTheDocument();
    expect(screen.getByTestId("location")).toHaveTextContent("/my-ads");
  });

  it("navigates to My Promotions from the dropdown", async () => {
    const user = userEvent.setup();
    renderHeader();

    await user.click(screen.getByRole("button", { name: /account/i }));
    await user.click(screen.getByRole("menuitem", { name: "My Promotions" }));

    expect(screen.getByTestId("location")).toHaveTextContent("/my-promotions");
  });

  it("navigates to My Payments from the dropdown", async () => {
    const user = userEvent.setup();
    renderHeader();

    await user.click(screen.getByRole("button", { name: /account/i }));
    await user.click(screen.getByRole("menuitem", { name: "My Payments" }));

    expect(screen.getByTestId("location")).toHaveTextContent("/my-payments");
  });

  it("logs out from the dropdown, clears auth, and navigates home", async () => {
    const user = userEvent.setup();
    renderHeader();

    await user.click(screen.getByRole("button", { name: /account/i }));
    await user.click(screen.getByRole("menuitem", { name: "Logout" }));

    expect(mockLogout).toHaveBeenCalledTimes(1);
    expect(screen.queryByRole("menu")).not.toBeInTheDocument();
    expect(screen.getByTestId("location")).toHaveTextContent("/");
  });

  it("closes the dropdown when clicking outside", async () => {
    const user = userEvent.setup();
    renderHeader();

    await user.click(screen.getByRole("button", { name: /account/i }));
    expect(screen.getByRole("menu")).toBeInTheDocument();

    await user.click(document.body);

    expect(screen.queryByRole("menu")).not.toBeInTheDocument();
  });

  it("keeps Post Free Ad visible and prominent", () => {
    renderHeader();
    expect(screen.getByRole("button", { name: /post free ad/i })).toBeInTheDocument();
  });
});

describe("Header - guest", () => {
  beforeEach(setGuest);

  it("does not show the Account trigger or authenticated actions", () => {
    renderHeader();

    expect(screen.queryByRole("button", { name: /account/i })).not.toBeInTheDocument();
    expect(screen.queryByText(/my ads/i)).not.toBeInTheDocument();
    expect(screen.queryByText(/my promotions/i)).not.toBeInTheDocument();
    expect(screen.queryByText(/my payments/i)).not.toBeInTheDocument();
    expect(screen.queryByRole("button", { name: "Logout" })).not.toBeInTheDocument();
  });

  it("still shows Login and Register", () => {
    renderHeader();
    expect(screen.getByRole("link", { name: "Login" })).toBeInTheDocument();
    expect(screen.getByRole("link", { name: "Register" })).toBeInTheDocument();
  });
});

describe("Header - authenticated mobile", () => {
  beforeEach(() => {
    setAuthenticatedCustomer();
    setViewport(true);
  });

  it("exposes account actions directly in the mobile nav instead of the desktop dropdown", () => {
    renderHeader();

    expect(screen.queryByRole("button", { name: /account/i })).not.toBeInTheDocument();
    expect(screen.getByRole("link", { name: "My Ads" })).toBeInTheDocument();
    expect(screen.getByRole("link", { name: "My Promotions" })).toBeInTheDocument();
    expect(screen.getByRole("link", { name: "My Payments" })).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Logout" })).toBeInTheDocument();
    expect(screen.getByRole("button", { name: /post free ad/i })).toBeInTheDocument();
  });

  it("logs out from the mobile nav", async () => {
    const user = userEvent.setup();
    renderHeader();

    await user.click(screen.getByRole("button", { name: "Logout" }));

    expect(mockLogout).toHaveBeenCalledTimes(1);
    expect(screen.getByTestId("location")).toHaveTextContent("/");
  });
});
