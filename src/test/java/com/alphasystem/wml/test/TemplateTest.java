package com.alphasystem.wml.test;

import com.alphasystem.openxml.builder.wml.WmlAdapter;
import org.docx4j.openpackaging.exceptions.Docx4JException;
import org.testng.annotations.Test;

import static com.alphasystem.openxml.builder.wml.WmlAdapter.*;

/**
 * @author sali
 */
public abstract class TemplateTest extends CommonTest {

    @Test
    public void testTitle() {
        getMainDocumentPart().addObject(getParagraphWithStyle("Title", "Document Title"));
        getMainDocumentPart().addObject(getHorizontalLine());
    }

    @Test(dependsOnMethods = {"testTitle"})
    public void testHeading1() {
        getMainDocumentPart().addObject(getParagraphWithStyle("Heading1", "Heading Level 1"));
        getMainDocumentPart().addObject(getParagraphWithStyle(null, "A paragraph with document default font"));
    }

    @Test(dependsOnMethods = {"testHeading1"})
    public void testHeading2() {
        getMainDocumentPart().addObject(getParagraphWithStyle("Heading2", "Heading Level 2"));
        getMainDocumentPart().addObject(getEmptyPara());
    }

    @Test(dependsOnMethods = {"testHeading2"})
    public void testHeading3() {
        getMainDocumentPart().addObject(getParagraphWithStyle("Heading3", "Heading Level 3"));
        getMainDocumentPart().addObject(getEmptyPara());
    }

    @Test(dependsOnMethods = {"testHeading3"})
    public void testHeading4() {
        getMainDocumentPart().addObject(getParagraphWithStyle("Heading4", "Heading Level 4"));
        getMainDocumentPart().addObject(getEmptyPara());
    }

    @Test(dependsOnMethods = {"testHeading4"})
    public void testHeading5() {
        getMainDocumentPart().addObject(getParagraphWithStyle("Heading5", "Heading Level 5"));
        getMainDocumentPart().addObject(getEmptyPara());
    }

    @Test(dependsOnMethods = {"testHeading5"})
    public void testAnotherHeading1() {
        getMainDocumentPart().addObject(getPageBreak());
        getMainDocumentPart().addObject(getParagraphWithStyle("Heading1", "Another Heading Level 1"));
        getMainDocumentPart().addObject(getEmptyPara());
    }

    @Test(dependsOnMethods = {"testHeading5"})
    public void addTableOfContent() {
        try {
            WmlAdapter.addTableOfContent(getMainDocumentPart(), 5);
        } catch (Docx4JException e) {
            e.printStackTrace();
        }
    }
}
