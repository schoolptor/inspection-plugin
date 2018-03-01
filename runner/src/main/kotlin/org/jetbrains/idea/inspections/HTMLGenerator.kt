package org.jetbrains.idea.inspections

import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.openapi.application.Application
import com.intellij.openapi.editor.Document
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.psi.*
import com.intellij.psi.impl.source.tree.LeafPsiElement
import org.gradle.api.reporting.SingleFileReport
import org.jetbrains.intellij.ProblemLevel

class HTMLGenerator(override val report: SingleFileReport, private val application: Application) : ReportGenerator {
    private val sb = StringBuilder()

    private val documentManager = FileDocumentManager.getInstance()

    init {
        sb.appendln("""
<html><head><style>
error {
    background-color: red;
}
warning {
    background-color: yellow;
}
info {
    text-decoration-style: wavy;
    text-decoration: underline;
}
unused {
    background-color: lightgray;
}
keyword {
    font-weight: bold;
}
</style></head>
<body>
""")
    }

    private fun PsiElement.coversElement(problemElement: PsiElement, document: Document?): Boolean {
        if (document == null) {
            return true
        }
        val problemLine = problemElement.getLine(document)
        val line = getLine(document)
        return line < problemLine
    }

    private fun PsiElement.findElementToPrint(): PsiElement {
        val document = containingFile?.virtualFile?.let { documentManager.getDocument(it) }
        var elementToPrint = this
        while (elementToPrint.parent != null &&
               elementToPrint.parent !is PsiFile &&
               !elementToPrint.coversElement(this, document)) {

            elementToPrint = elementToPrint.parent
        }
        return elementToPrint
    }

    private fun PsiElement.printSmartly(problemChild: PsiElement, problemTag: String) {
        sb.appendln("<pre>")
        this.accept(object : PsiRecursiveElementVisitor() {
            override fun visitElement(element: PsiElement) {
                if (element === problemChild) {
                    sb.append("<$problemTag>")
                }
                super.visitElement(element)
                if (element.firstChild == null) {
                    val keyword = when (element) {
                        is PsiKeyword -> true
                        is LeafPsiElement -> element.text.isKotlinKeyword()
                        else -> false
                    }
                    if (keyword) {
                        sb.append("<keyword>")
                    }
                    sb.append(element.text)
                    if (keyword) {
                        sb.append("</keyword>")
                    }
                }
                if (element === problemChild) {
                    sb.append("</$problemTag>")
                }
            }
        })
        sb.appendln()
        sb.appendln("</pre>")
    }

    override fun report(problem: PinnedProblemDescriptor, level: ProblemLevel, inspectionClass: String) {
        sb.appendln("<p>")
        sb.appendln("    In file <b>${problem.renderLocation()}</b>:")
        sb.appendln("</p>")

        val psiElement = problem.psiElement
        application.runReadAction {
            val problemTag = when (problem.highlightType) {
                ProblemHighlightType.LIKE_UNUSED_SYMBOL -> "unused"
                else -> when (level) {
                    ProblemLevel.ERROR -> "error"
                    ProblemLevel.WARNING -> "warning"
                    ProblemLevel.WEAK_WARNING, ProblemLevel.INFORMATION -> "info"
                }
            }
            psiElement?.findElementToPrint()?.printSmartly(psiElement, problemTag)
        }
        sb.appendln("<p>")
        sb.appendln("    <i>${problem.render()}</i>")
        sb.appendln("</p>")
    }

    private fun closeHtml() {
        sb.appendln("</body></html>")
    }

    override fun generate() {
        closeHtml()

        val htmlReportFile = report.destination
        htmlReportFile.writeText(sb.toString())
    }
}