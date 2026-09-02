const formatter = new Intl.NumberFormat("en-LK", {
  maximumFractionDigits: 0,
});

export function formatPrice(price: number): string {
  return `Rs. ${formatter.format(price)}`;
}

// Ad prices treat 0 as "seller hasn't fixed a fee" rather than literally free.
export function formatAdPrice(price: number): string {
  return price === 0 ? "Contact for fee" : `${formatPrice(price)} / month`;
}
