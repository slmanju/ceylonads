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

// A promotion's currentPrice of exactly 0 (e.g. a 100%-off campaign - see PromotionPricingService)
// means genuinely free, unlike an ad price of 0. Only ever applied to currentPrice/discounted
// values, never to a plan's permanent base price.
export function formatPromotionPrice(price: number): string {
  return price === 0 ? "FREE" : formatPrice(price);
}
