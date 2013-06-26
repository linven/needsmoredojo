package com.chrisfolger.needsmoredojo.core.amd;

import com.intellij.lang.javascript.psi.JSArrayLiteralExpression;
import com.intellij.lang.javascript.psi.JSCallExpression;
import com.intellij.lang.javascript.psi.JSExpression;
import com.intellij.lang.javascript.psi.JSRecursiveElementVisitor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import com.intellij.psi.XmlRecursiveElementVisitor;
import com.intellij.psi.search.FilenameIndex;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.xml.XmlAttribute;
import com.intellij.psi.xml.XmlTag;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SourcesAutoDetector
{
    private boolean isDojoModule(String path)
    {
        return path.startsWith("dojo") || path.startsWith("dijit") || path.startsWith("dgrid") || path.startsWith("dojox");
    }

    private boolean isInDojoSources(String directory)
    {
        return directory.contains("/dojo/") || directory.contains("/dijit/") || directory.contains("/dojox/") || directory.contains("/dgrid/") || directory.contains("/util/buildscripts/");
    }

    private Set<String> getDojoModulesInHtmlFile(PsiFile file)
    {
        final Set<String> modules = new HashSet<String>();

        file.acceptChildren(new JSRecursiveElementVisitor() {
            @Override
            public void visitJSCallExpression(JSCallExpression node) {
                if(!node.getText().startsWith("require"))
                {
                    super.visitJSCallExpression(node);
                    return;
                }

                if(node.getArguments().length > 0 && node.getArguments()[0] instanceof JSArrayLiteralExpression)
                {
                    JSArrayLiteralExpression arguments = (JSArrayLiteralExpression) node.getArguments()[0];
                    for(JSExpression literal : arguments.getExpressions())
                    {
                        String literalText = literal.getText().replaceAll("'", "").replaceAll("\"", "");

                        if(!isDojoModule(literalText))
                        {
                            modules.add(literalText);
                        }
                    }
                }

                super.visitJSCallExpression(node);
            }
        });

        file.acceptChildren(new XmlRecursiveElementVisitor() {
            @Override
            public void visitXmlTag(XmlTag tag) {
                super.visitXmlTag(tag);
            }

            @Override
            public void visitXmlAttribute(XmlAttribute attribute) {
                if(attribute.getName().equals("data-dojo-type"))
                {
                    if(!isDojoModule(attribute.getValue()))
                    {
                        modules.add(attribute.getValue());
                    }
                }

                super.visitXmlAttribute(attribute);    //To change body of overridden methods use File | Settings | File Templates.
            }
        });

        return modules;
    }

    private Set<String> getDirectoriesForDojoModules(Project project, Set<String> modules)
    {
        Set<String> possibleDirectories = new HashSet<String>();

        for(String module : modules)
        {
            String moduleParent = module;

            if(module.contains("/"))
            {
                module = module.substring(module.lastIndexOf("/") + 1);
            }

            PsiFile[] files = FilenameIndex.getFilesByName(project, module + ".js", GlobalSearchScope.projectScope(project));

            for(PsiFile file : files)
            {
                if( file.getVirtualFile().getCanonicalPath().contains(moduleParent))
                {
                    String path = file.getVirtualFile().getCanonicalPath();
                    path = path.substring(0, path.indexOf(moduleParent));
                    if(path.charAt(path.length() - 1) == '/')
                    {
                        path = path.substring(0, path.length()-1);
                    }

                    possibleDirectories.add(path);
                }
            }
        }

        return possibleDirectories;
    }

    public Set<String> getPossibleSourceRoots(Project project)
    {
        Set<String> possibleDirectories = new HashSet<String>();

        /**
         * here's how we guess where the project root is:
         * open index.html to search for amd module references
         */

        PsiFile[] files = FilenameIndex.getFilesByName(project, "index.html", GlobalSearchScope.projectScope(project));
        List<PsiFile> potentialFiles = new ArrayList<PsiFile>();

        for(PsiFile file : files)
        {
            String directory = file.getContainingDirectory().getVirtualFile().getCanonicalPath();
            if(!isInDojoSources(directory))
            {
                potentialFiles.add(file);
            }
        }

        Set<String> possibleSourceModules = new HashSet<String>();
        for(PsiFile file : potentialFiles)
        {
            possibleSourceModules.addAll(getDojoModulesInHtmlFile(file));
        }
        possibleDirectories.addAll(getDirectoriesForDojoModules(project, possibleSourceModules));

        return possibleDirectories;
    }
}