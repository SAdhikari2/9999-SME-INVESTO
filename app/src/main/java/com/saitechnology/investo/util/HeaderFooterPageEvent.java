package com.saitechnology.investo.util;

import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Font;
import com.itextpdf.text.pdf.BaseFont;
import com.itextpdf.text.pdf.PdfContentByte;
import com.itextpdf.text.pdf.PdfPageEventHelper;
import com.itextpdf.text.pdf.PdfWriter;

import java.io.IOException;

public class HeaderFooterPageEvent extends PdfPageEventHelper {

    private final Font headerFont;
    private final Font footerFont;
    private BaseFont baseFont;

    public HeaderFooterPageEvent(Font headerFont, Font footerFont) {
        this.headerFont = headerFont;
        this.footerFont = footerFont;
        try {
            baseFont = BaseFont.createFont(BaseFont.HELVETICA, BaseFont.WINANSI, BaseFont.NOT_EMBEDDED);
        } catch (DocumentException | IOException e) {
            e.getMessage();
        }
    }

    @Override
    public void onEndPage(PdfWriter writer, Document document) {
        PdfContentByte canvas = writer.getDirectContent();

        // Add Header
        String headerText = "-----Account Statement-----";
        float headerWidth = baseFont.getWidthPoint(headerText, headerFont.getSize());
        float headerX = (document.right() - document.left() - headerWidth) / 2 + document.leftMargin();
        float headerY = document.top() + 10; // Adjust as needed
        canvas.beginText();
        canvas.setFontAndSize(baseFont, headerFont.getSize());
        canvas.setTextMatrix(headerX, headerY);
        canvas.showText(headerText);
        canvas.endText();

        // Add Footer
        String footerText = "Developed by mPOS Team";
        float footerWidth = baseFont.getWidthPoint(footerText, footerFont.getSize());
        float footerX = (document.right() - document.left() - footerWidth) / 2 + document.leftMargin();
        float footerY = document.bottom() - 10; // Adjust as needed
        canvas.beginText();
        canvas.setFontAndSize(baseFont, footerFont.getSize());
        canvas.setTextMatrix(footerX, footerY);
        canvas.showText(footerText);
        canvas.endText();
    }
}
