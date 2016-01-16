package requirejs;

import com.intellij.lang.javascript.psi.JSArgumentList;
import com.intellij.lang.javascript.psi.JSArrayLiteralExpression;
import com.intellij.lang.javascript.psi.JSCallExpression;
import com.intellij.lang.javascript.psi.JSReferenceExpression;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.psi.PsiReferenceProvider;
import com.intellij.util.ProcessingContext;
import org.jetbrains.annotations.NotNull;
import requirejs.settings.Settings;

public class RequirejsPsiReferenceProvider extends PsiReferenceProvider {

    @NotNull
    @Override
    public PsiReference[] getReferencesByElement(@NotNull PsiElement psiElement, @NotNull ProcessingContext processingContext) {
        RequirejsProjectComponent projectComponent = psiElement.getProject().getComponent(RequirejsProjectComponent.class);

        if (!projectComponent.isEnabled()) {
            return PsiReference.EMPTY_ARRAY;
        }

        // Only provided references for files inside the public directory
        String publicPath = getContentRoot(psiElement.getProject()).getPath() + '/' +
                Settings.getInstance(psiElement.getProject()).publicPath;
        VirtualFile virtualFile = psiElement.getContainingFile().getVirtualFile();
        if (virtualFile == null || virtualFile.getPath().startsWith(publicPath)) {
            String path = psiElement.getText();
            if (isRequireCall(psiElement) || isDefineFirstCollection(psiElement)) {
                PsiReference ref = new RequirejsReference(psiElement, new TextRange(1, path.length() - 1));
                return new PsiReference[] {ref};
            }
        }

        return new PsiReference[0];
    }

    public VirtualFile getContentRoot(Project project) {
        VirtualFile[] contentRoots = ProjectRootManager.getInstance(project).getContentRoots();
        if (contentRoots.length > 0) {
            return contentRoots[0];
        } else {
            return project.getBaseDir();
        }
    }

    public boolean isRequireCall(PsiElement element) {
        PsiElement prevEl = element.getParent();
        if (prevEl != null) {
            prevEl = prevEl.getParent();
        }

        if (prevEl instanceof JSCallExpression) {
            if (prevEl.getChildren().length > 1) {
                String requireFunctionName = Settings.REQUIREJS_REQUIRE_FUNCTION_NAME;
                if (prevEl.getChildren()[0].getText().toLowerCase().equals(requireFunctionName)) {
                    return true;
                }
            }
        }

        return false;
    }

    public boolean isDefineFirstCollection(PsiElement element) {
        PsiElement jsArrayLiteral = element.getParent();
        if (null != jsArrayLiteral && jsArrayLiteral instanceof JSArrayLiteralExpression) {
            PsiElement jsArgumentList = jsArrayLiteral.getParent();
            if (null != jsArgumentList && jsArgumentList instanceof JSArgumentList) {
                PsiElement jsReferenceExpression = jsArgumentList.getPrevSibling();
                if (null != jsReferenceExpression && jsReferenceExpression instanceof JSReferenceExpression) {
                    if (jsReferenceExpression.getText().equals(Settings.REQUIREJS_DEFINE_FUNCTION_NAME)) {
                        return true;
                    }
                }
            }
        }

        return false;
    }
}
