import { useEffect, useState } from "react";
import { tuitionPromotionRepository } from "../tuition/promotion/api/tuitionPromotionApi";
import type { DetailPromotionContext, DetailPromotions, HomepagePromotions, SearchPromotionContext, SearchPromotions } from "../tuition/promotion/model/promotion";

const EMPTY_HOMEPAGE: HomepagePromotions = { featured: [] };
const EMPTY_SEARCH: SearchPromotions = {};
const EMPTY_DETAIL: DetailPromotions = {};

export function useHomepagePromotions(): HomepagePromotions {
  const [promotions, setPromotions] = useState<HomepagePromotions>(EMPTY_HOMEPAGE);

  useEffect(() => {
    let cancelled = false;
    tuitionPromotionRepository.getHomepagePromotions().then((data) => {
      if (!cancelled) setPromotions(data);
    });
    return () => {
      cancelled = true;
    };
  }, []);

  return promotions;
}

// `context` is re-serialized as the effect key so callers can pass a fresh object each render
// (as ClassSearchResults does for its own search effect) without refetching every render.
export function useSearchPromotions(context: SearchPromotionContext): SearchPromotions {
  const [promotions, setPromotions] = useState<SearchPromotions>(EMPTY_SEARCH);
  const key = JSON.stringify(context);

  useEffect(() => {
    let cancelled = false;
    tuitionPromotionRepository.getSearchPromotions(context).then((data) => {
      if (!cancelled) setPromotions(data);
    });
    return () => {
      cancelled = true;
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [key]);

  return promotions;
}

export function useDetailPromotions(context: DetailPromotionContext | null): DetailPromotions {
  const [promotions, setPromotions] = useState<DetailPromotions>(EMPTY_DETAIL);
  const key = context ? JSON.stringify(context) : null;

  useEffect(() => {
    if (!context) {
      setPromotions(EMPTY_DETAIL);
      return;
    }
    let cancelled = false;
    tuitionPromotionRepository.getDetailPromotions(context).then((data) => {
      if (!cancelled) setPromotions(data);
    });
    return () => {
      cancelled = true;
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [key]);

  return promotions;
}
