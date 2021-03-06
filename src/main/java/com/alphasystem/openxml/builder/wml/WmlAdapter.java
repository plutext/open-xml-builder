/**
 *
 */
package com.alphasystem.openxml.builder.wml;

import org.docx4j.Docx4J;
import org.docx4j.convert.out.FOSettings;
import org.docx4j.fonts.IdentityPlusMapper;
import org.docx4j.fonts.Mapper;
import org.docx4j.fonts.PhysicalFont;
import org.docx4j.fonts.PhysicalFonts;
import org.docx4j.jaxb.Context;
import org.docx4j.model.fields.FieldUpdater;
import org.docx4j.openpackaging.exceptions.Docx4JException;
import org.docx4j.openpackaging.exceptions.InvalidFormatException;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.docx4j.openpackaging.parts.WordprocessingML.DocumentSettingsPart;
import org.docx4j.openpackaging.parts.WordprocessingML.MainDocumentPart;
import org.docx4j.wml.*;
import org.docx4j.wml.TcPrInner.GridSpan;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.namespace.QName;
import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static com.alphasystem.openxml.builder.wml.WmlBuilderFactory.*;
import static com.alphasystem.util.IdGenerator.nextId;
import static java.lang.String.format;
import static java.lang.Thread.currentThread;
import static org.apache.commons.lang3.ArrayUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.docx4j.XmlUtils.unmarshal;
import static org.docx4j.openpackaging.parts.relationships.Namespaces.NS_WORD12;
import static org.docx4j.wml.STBorder.NIL;
import static org.docx4j.wml.STBorder.SINGLE;
import static org.docx4j.wml.STBrType.PAGE;

/**
 * @author sali
 */
public class WmlAdapter {

    public static final Text SINGLE_SPACE = getText(" ", "preserve");

    private static final Logger LOGGER = LoggerFactory.getLogger(WmlAdapter.class);

    private static final ClassLoader CLASS_LOADER = currentThread().getContextClassLoader();

    private static final AtomicInteger bookmarkCount = new AtomicInteger(0);

    static Styles loadStyles(Styles styles, String... paths) {
        if (isEmpty(paths)) {
            return styles;
        }
        for (String p : paths) {
            try {
                final List<URL> urls = readResources(p);
                if (urls.isEmpty()) {
                    return styles;
                }
                for (URL url : urls) {
                    System.out.println(format("Reading style {%s}", url));
                    try (InputStream ins = url.openStream()) {
                        final Styles otherStyles = (Styles) unmarshal(ins);
                        if (styles == null) {
                            styles = otherStyles;
                        } else {
                            styles.getStyle().addAll(otherStyles.getStyle());
                        }
                    } catch (JAXBException e) {
                        e.printStackTrace();
                    }
                }
            } catch (IOException e) {
                throw new UncheckedIOException(e.getMessage(), e);
            }
        } // end of for
        return styles;
    }

    static Numbering loadNumbering(Numbering numbering, String... paths) {
        if (isEmpty(paths)) {
            return null;
        }
        for (String p : paths) {
            try {
                final List<URL> urls = readResources(p);
                if (urls.isEmpty()) {
                    return numbering;
                }
                for (URL url : urls) {
                    System.out.println(format("Reading numbering {%s}", url));
                    try (InputStream ins = url.openStream()) {
                        final Numbering o = (Numbering) unmarshal(ins);
                        if (numbering == null) {
                            numbering = o;
                        } else {
                            numbering.getAbstractNum().addAll(o.getAbstractNum());
                            numbering.getNum().addAll(o.getNum());
                        }
                    } catch (JAXBException e) {
                        e.printStackTrace();
                    }
                }
            } catch (IOException e) {
                throw new UncheckedIOException(e.getMessage(), e);
            }
        }
        return numbering;
    }

    static List<URL> readResources(String pathName) throws IOException {
        LOGGER.info("Loading resource \"{}\".", pathName);
        List<URL> urls = new ArrayList<>();
        Path path = Paths.get(pathName);
        if (Files.exists(path)) {
            // path is a absolute file
            LOGGER.info("Path \"{}\" is an absolute file.", pathName);
            urls.add(path.toUri().toURL());
        } else {
            Enumeration<URL> resources;
            resources = CLASS_LOADER.getResources(pathName);
            if (resources == null || !resources.hasMoreElements()) {
                LOGGER.info("Resource \"{}\" does not exists.", pathName);
                pathName = format("META-INF/%s", pathName);
                resources = CLASS_LOADER.getResources(pathName);
            }
            if (resources == null || !resources.hasMoreElements()) {
                LOGGER.info("Resource \"{}\" does not exists.", pathName);
                return urls;
            }
            while (resources.hasMoreElements()) {
                urls.add(resources.nextElement());
            }
            Collections.reverse(urls);
        }
        return urls;
    }

    public static void addBookMark(PBuilder pBuilder, String id) {
        if (isBlank(id)) {
            return;
        }
        final long value = bookmarkCount.longValue();
        final CTBookmark bookmarkStart = getCTBookmarkBuilder().withId(value).withName(id).getObject();
        final JAXBElement<CTMarkupRange> bookmarkEnd = createCTMarkupRange(getCTBookmarkRangeBuilder().withId(value).getObject());
        pBuilder.getObject().getContent().add(0, bookmarkStart);
        pBuilder.addContent(bookmarkEnd);
    }

    public static void addBookMark(P p, String id) {
        if (isBlank(id)) {
            return;
        }
        String bookmarkId = String.valueOf(bookmarkCount.incrementAndGet());
        CTBookmark bookmarkStart = getCTBookmarkBuilder().withId(bookmarkId).withName(id).getObject();
        JAXBElement<CTMarkupRange> bookmarkEnd = createCTMarkupRange(getCTBookmarkRangeBuilder().withId(bookmarkId).getObject());
        p.getContent().add(0, bookmarkStart);
        p.getContent().add(bookmarkEnd);
    }

    public static JAXBElement<CTBookmark> createBodyBookmarkStart(
            CTBookmark bookmark) {
        return OBJECT_FACTORY.createBodyBookmarkStart(bookmark);
    }

    public static JAXBElement<CTMarkupRange> createCTMarkupRange(
            CTMarkupRange value) {
        return OBJECT_FACTORY.createBodyBookmarkEnd(value);
    }

    public static PPrBase.NumPr getNumPr(Long numId, Long level) {
        return new PPrBaseBuilder().getNumPrBuilder().withNumId(numId).withIlvl(level).getObject();
    }

    public static PPr getListParagraphProperties(long listId, long level, boolean applyNumbering) {
        return getListParagraphProperties(listId, level, "ListParagraph", applyNumbering);
    }

    public static PPr getListParagraphProperties(long listId, long level, String style, boolean applyNumbering) {
        style = isBlank(style) ? "ListParagraph" : style;
        final PPrBuilder pPrBuilder = getPPrBuilder();
        PPrBase.NumPr numPr = applyNumbering ? getNumPr(listId, level) : null;
        return pPrBuilder.withNumPr(numPr).withPStyle(style).getObject();
    }

    /**
     * @param value
     * @return
     */
    public static Color getColor(String value) {
        return getColorBuilder().withVal(value).getObject();
    }

    public static File getFile(File file, String extension) {
        String fileName = file.getName();
        if (fileName.endsWith(extension)) {
            return file;
        }
        File folder = file.getParentFile();
        int index = fileName.lastIndexOf('.');
        if (index > -1) {
            fileName = fileName.substring(0, index);
        }
        return new File(folder, format("%s.%s", fileName, extension));
    }

    /**
     * @param value
     * @return
     */
    public static GridSpan getGridSpan(long value) {
        return getTcPrInnerBuilder().getGridSpanBuilder().withVal(value).getObject();
    }

    public static CTBorder getDefaultBorder() {
        return getBorder(SINGLE, 0L, 0L, "auto");
    }

    public static CTBorder getNilBorder() {
        return getBorder(NIL, 0L, 0L, "auto");
    }

    public static CTBorder getBorder(STBorder borderType, Long size, Long space, String color) {
        return getCTBorderBuilder().withVal(borderType).withSz(size).withSpace(space).withColor(color).getObject();
    }

    public static TcPrInner.TcBorders getNilBorders() {
        return getTcPrBuilder().getTcBordersBuilder().withTop(getNilBorder()).withBottom(getNilBorder())
                .withLeft(getNilBorder()).withRight(getNilBorder()).withInsideH(getNilBorder()).withInsideV(getNilBorder())
                .getObject();
    }

    /**
     * @param value
     * @param space
     * @return
     */
    public static Text getText(String value, String space) {
        return getTextBuilder().withValue(value).withSpace(space).getObject();
    }

    public static Text getText(String value) {
        String space = !value.equals(value.trim()) ? "preserve" : null;
        return getText(value, space);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static JAXBElement getWrappedFldChar(FldChar fldchar) {
        return new JAXBElement(new QName(NS_WORD12, "fldChar"), FldChar.class, fldchar);
    }

    public static P getEmptyPara() {
        return getEmptyPara(null);
    }

    public static P getEmptyParaNoSpacing() {
        return getEmptyPara("NoSpacing");
    }

    public static P getEmptyPara(String styleName) {
        String id = nextId();

        PPr ppr = null;
        if (styleName != null) {
            ppr = getPPrBuilder().withPStyle(styleName).getObject();
        }

        return getPBuilder().withPPr(ppr).withRsidP(id).withRsidR(id).withRsidRDefault(id).getObject();
    }

    public static P getParagraph(String text) {
        return getParagraphWithStyle(null, text);
    }

    public static P getParagraphWithStyle(String styleName, String text) {
        String rsidRPr = nextId();
        R r = getRBuilder().withRsidRPr(rsidRPr).addContent(getText(text)).getObject();
        return getPBuilder(getEmptyPara(styleName)).withRsidRPr(rsidRPr).addContent(r).getObject();
    }

    public static P getHorizontalLine() {
        PPrBase.PBdr pBdr = WmlBuilderFactory.getPPrBaseBuilder().getPBdrBuilder().
                withTop(getBorder(SINGLE, 6L, 1L, "auto")).getObject();
        PPr pPr = getPPrBuilder().withPBdr(pBdr).getObject();
        return getPBuilder().withPPr(pPr).getObject();
    }

    public static P getPageBreak() {
        String id = nextId();
        Br br = getBrBuilder().withType(PAGE).getObject();
        R r = getRBuilder().addContent(br).getObject();
        return getPBuilder().withRsidP(id).withRsidR(id).withRsidRDefault(id).addContent(r).getObject();
    }

    static void updateSettings(MainDocumentPart mainDocumentPart) throws InvalidFormatException {
        //Adding Print View and Setting Update Field to true
        DocumentSettingsPart dsp = mainDocumentPart.getDocumentSettingsPart();
        if (dsp == null) {
            CTSettings ct = new CTSettings();
            dsp = new DocumentSettingsPart();
            CTView ctView = Context.getWmlObjectFactory().createCTView();
            ctView.setVal(STView.PRINT);
            ct.setView(ctView);
            ct.setUpdateFields(WmlBuilderFactory.BOOLEAN_DEFAULT_TRUE_TRUE);
            dsp.setJaxbElement(ct);
            mainDocumentPart.addTargetPart(dsp);
        }
    }

    @Deprecated
    public static void addTableOfContent(final MainDocumentPart mainDocumentPart, String tocTitle,
                                         int level) throws Docx4JException {
        int index = 0;
        TocGenerator tocGenerator = new TocGenerator().index(index).level(level).tocHeading(tocTitle).mainDocumentPart(mainDocumentPart);
        tocGenerator.generateToc();
        updateSettings(mainDocumentPart);
        mainDocumentPart.getContent().add(index + 1, getPageBreak());
    }

    @Deprecated
    public static void addTableOfContent(final MainDocumentPart mainDocumentPart, String tocTitle, String instruction) throws Docx4JException {
        int index = 0;
        TocGenerator tocGenerator = new TocGenerator().index(index).tocHeading(tocTitle).instruction(instruction)
                .mainDocumentPart(mainDocumentPart);
        tocGenerator.generateToc();
        updateSettings(mainDocumentPart);
        mainDocumentPart.getContent().add(index + 1, getPageBreak());
    }

    @Deprecated
    public static void addTableOfContent(final MainDocumentPart mainDocumentPart, String tocTitle) throws Docx4JException {
        addTableOfContent(mainDocumentPart, tocTitle, 3);
    }

    @Deprecated
    public static void addTableOfContent(final MainDocumentPart mainDocumentPart) throws Docx4JException {
        addTableOfContent(mainDocumentPart, null);
    }

    @Deprecated
    public static void addTableOfContent(final MainDocumentPart mainDocumentPart, int level) throws Docx4JException {
        addTableOfContent(mainDocumentPart, null, level);
    }

    public static void save(File file, WordprocessingMLPackage wordMLPackage)
            throws Docx4JException {
        wordMLPackage.save(getFile(file, "docx"));
    }

    public static void saveAsPdf(File file, WordprocessingMLPackage wordMLPackage, boolean saveFO) throws Exception {
        File pdfFile = getFile(file, "pdf");

        PhysicalFonts.setRegex(null);

        // Refresh the values of DOCPROPERTY fields
        FieldUpdater updater = new FieldUpdater(wordMLPackage);
        updater.update(true);

        // Set up font mapper (optional)
        Mapper fontMapper = new IdentityPlusMapper();
        wordMLPackage.setFontMapper(fontMapper);

        FOSettings foSettings = Docx4J.createFOSettings();
        if (saveFO) {
            foSettings.setFoDumpFile(getFile(file, "fo"));
        }
        foSettings.setWmlPackage(wordMLPackage);

        try (OutputStream os = new java.io.FileOutputStream(pdfFile)) {
            Docx4J.toFO(foSettings, os, Docx4J.FLAG_EXPORT_PREFER_XSL);
        }
    }

    public static void saveAsPdf(File file, WordprocessingMLPackage wordMLPackage, String fontName,
                                 boolean saveFO) throws Exception {
        File pdfFile = getFile(file, "pdf");

        // Set up font mapper (optional)
        Mapper fontMapper = new IdentityPlusMapper();
        wordMLPackage.setFontMapper(fontMapper);

        PhysicalFont font = PhysicalFonts.get(fontName);
        if (font != null) {
            fontMapper.put("Times New Roman", font);
            fontMapper.put("Arial", font);
        }

        FOSettings foSettings = Docx4J.createFOSettings();
        if (saveFO) {
            foSettings.setFoDumpFile(getFile(pdfFile, "fo"));
        }
        foSettings.setWmlPackage(wordMLPackage);

        OutputStream os = new java.io.FileOutputStream(pdfFile);

        // Don't care what type of exporter you use
        Docx4J.toFO(foSettings, os, Docx4J.FLAG_EXPORT_PREFER_XSL);
    }
}
