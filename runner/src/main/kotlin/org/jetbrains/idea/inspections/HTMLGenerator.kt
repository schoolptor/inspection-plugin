package org.jetbrains.idea.inspections

import com.intellij.openapi.application.Application
import com.intellij.openapi.editor.Document
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import org.gradle.api.reporting.SingleFileReport
import org.jetbrains.intellij.ProblemLevel

class HTMLGenerator(override val report: SingleFileReport, private val application: Application) : ReportGenerator {
    private val sb = StringBuilder()

    private val documentManager = FileDocumentManager.getInstance()

    init {
        sb.appendln("<html><body>")
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
        var document: Document? = null
        document = containingFile?.virtualFile?.let { documentManager.getDocument(it) }
        var elementToPrint = this
        while (elementToPrint.parent != null &&
               elementToPrint.parent !is PsiFile &&
               !elementToPrint.coversElement(this, document)) {

            elementToPrint = elementToPrint.parent
        }
        return this
    }

    private fun PsiElement.printSmartly(problemChild: PsiElement) {
        sb.appendln("<pre>")
        sb.appendln(findElementToPrint().text)
        sb.appendln("</pre>")
    }

    override fun report(problem: PinnedProblemDescriptor, level: ProblemLevel, inspectionClass: String) {
        sb.appendln("<p>")
        sb.appendln("In file <b>${problem.fileName}</b>:")
        sb.appendln("</p>")

        val psiElement = problem.psiElement
        application.runReadAction {
            psiElement?.findElementToPrint()?.printSmartly(psiElement)
        }
        sb.appendln("<p>")
        if (psiElement != null) {
            sb.appendln("<i>${problem.render()}</i>")
        }
        else {
            sb.appendln("<i>${problem.renderWithLocation()}</i>")
        }
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