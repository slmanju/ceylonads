import type {
  DetailPromotionContext,
  DetailPromotions,
  HomepagePromotions,
  ProfilePromotionContext,
  SearchPromotionContext,
  SearchPromotions,
  TuitionPromotion,
} from "../model/promotion";

// The UI must not know whether tuition promotion data comes from the mock provider or a future
// real API - both MockTuitionPromotionRepository and a later HttpTuitionPromotionRepository
// implement this same contract. See tuitionPromotionApi.ts for the single composition point that
// picks between them.
export interface TuitionPromotionRepository {
  getHomepagePromotions(): Promise<HomepagePromotions>;
  getSearchPromotions(context: SearchPromotionContext): Promise<SearchPromotions>;
  getDetailPromotions(context: DetailPromotionContext): Promise<DetailPromotions>;
  /** Promotions eligible to appear ON a given tutor/institute's own profile - implemented for
   *  when a dedicated tutor/institute profile route ships; ceylonads-tuition-ui has none today. */
  getProfilePromotions(context: ProfilePromotionContext): Promise<TuitionPromotion[]>;
}
