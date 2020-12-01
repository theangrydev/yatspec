package com.googlecode.yatspec.plugin.jdom;

import com.googlecode.yatspec.rendering.Renderer;
import org.jdom.Document;
import org.jdom.output.XMLOutputter;

import static org.apache.commons.text.StringEscapeUtils.escapeXml11;
import static org.jdom.output.Format.getPrettyFormat;

public class DocumentRenderer implements Renderer<Document> {
    public String render(Document document) {
        return escapeXml11(prettyFormat(document));
    }

    private static String prettyFormat(Document document) {
        return new XMLOutputter(getPrettyFormat()).outputString(document);
    }
}
