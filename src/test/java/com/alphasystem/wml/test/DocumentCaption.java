package com.alphasystem.wml.test;

import com.alphasystem.openxml.builder.wml.AbstractListItem;
import org.docx4j.wml.NumberFormat;

import static java.lang.String.format;
import static org.docx4j.wml.NumberFormat.DECIMAL;

/**
 * @author sali
 */
public abstract class DocumentCaption extends AbstractListItem<DocumentCaption> {

    public static final DocumentCaption EXAMPLE = new DocumentCaption("ExampleTitle", DECIMAL) {

        @Override
        public String getValue(int i) {
            return format("Example %%%s.", i);
        }

        @Override
        public String getName() {
            return "EXAMPLE";
        }

    };

    DocumentCaption(String styleName, NumberFormat numberFormat) {
        super(styleName, numberFormat);
    }

    @Override
    public boolean linkStyle() {
        return true;
    }

    @Override
    public long getLeftIndent(int i) {
        return 432;
    }

    @Override
    public String getMultiLevelType() {
        return "multilevel";
    }
}
