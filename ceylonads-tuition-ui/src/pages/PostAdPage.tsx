import { PostAdWizard } from "./PostAd/PostAdWizard";
import { Seo } from "../components/Seo/Seo";

export function PostAdPage() {
  return (
    <>
      <Seo title="Post a Class" noindex />
      <PostAdWizard mode="create" />
    </>
  );
}
