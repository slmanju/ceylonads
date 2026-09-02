const formatter = new Intl.NumberFormat("en-LK", {
  maximumFractionDigits: 0,
});

export function formatPrice(price: number): string {
  return `Rs. ${formatter.format(price)}`;
}

// Ad prices (not promotion/plan prices, which are never zero and should keep using formatPrice
// above) treat 0 as "seller hasn't fixed a price" rather than literally free.
export function formatAdPrice(price: number): string {
  return price === 0 ? "Contact for price" : formatPrice(price);
}
