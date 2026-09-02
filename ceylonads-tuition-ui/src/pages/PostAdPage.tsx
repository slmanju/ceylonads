import { PostAdWizard } from "./PostAd/PostAdWizard";
import { Seo } from "../components/Seo/Seo";

export function PostAdPage() {
  return (
    <>
      <Seo title="Post a Tuition Ad" noindex />
      <PostAdWizard mode="create" />
    </>
  );
}
