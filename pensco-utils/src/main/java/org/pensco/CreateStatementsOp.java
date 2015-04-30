/*
 * (C) Copyright ${year} Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     thibaud
 */

package org.pensco;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import javax.imageio.ImageIO;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.pdfbox.exceptions.COSVisitorException;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.edit.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.graphics.xobject.PDPixelMap;
import org.apache.pdfbox.pdmodel.graphics.xobject.PDXObjectImage;
import org.nuxeo.datademo.RandomFirstLastNames;
import org.nuxeo.datademo.tools.DocumentsCallback;
import org.nuxeo.datademo.tools.DocumentsWalker;
import org.nuxeo.datademo.tools.ToolsMisc;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.dublincore.listener.DublinCoreListener;
import org.nuxeo.runtime.transaction.TransactionHelper;

/**
 * 
 */
@Operation(id = CreateStatementsOp.ID, category = Constants.CAT_SERVICES, label = "Data Demo: Create Statements", description = "Creates 3-10 statements/customer, every 14 days")
public class CreateStatementsOp {

    public static final String ID = "CreateStatementsOp";

    private static final Log log = LogFactory.getLog(CreateStatementsOp.class);

    public static final int MODULO_FOR_COMMIT = 50;

    public static final String CONVERTER_WITH_DRAW_TEXT = "modifyCheck-withDrawText";

    public static final String CONVERTER_NAME_AND_OTHERS = "modifyCheck-nameAndOthers";

    public static final int NB_CUSTOMERS = 300;

    public static final int DAYS_BETWEEN_STATEMENTS = 14;

    public static final String[] SPECIAL_CUSTOMERS = { "JOHN SMITH",
            "SUSAN JOHNSON", "DAVID JONES", "LISA WILLIAMS", "WILLIAM MOORE" };

    public static final int SPECIAL_CUSTOMERS_MAX = SPECIAL_CUSTOMERS.length - 1;

    protected static final String[] MODIF_USERS = { "john", "john", "john",
            "jim", "kate", "kate", "Administrator" };

    protected static final int MODIF_USERS_MAX = MODIF_USERS.length - 1;

    protected RandomFirstLastNames firstLastNames;

    protected DateFormat yyyyMMdd = new SimpleDateFormat("yyyy-MM-dd");

    protected DateFormat yyyy = new SimpleDateFormat("yyyy");

    protected Calendar today = Calendar.getInstance();

    protected String parentPath;

    protected ArrayList<String> customerNames;

    protected int countCreated;

    protected int statementLines;

    protected BufferedImage logoImage = null;

    // To avoid allocating an array for each document created
    protected String[] contributors = { "" };

    @Context
    protected CoreSession session;

    @Param(name = "statementsFolderDoc", required = true)
    protected DocumentModel statementsFolderDoc;

    @Param(name = "deletePrevious", required = false, values = { "true" })
    protected boolean deletePrevious = true;

    @Param(name = "nbCustomers", required = false, values = { "300" })
    protected long nbCustomers = NB_CUSTOMERS;

    @Param(name = "useCheckCustomers", required = false, values = { "true" })
    protected boolean useCheckCustomers = true;

    @Param(name = "pathToLogo", required = false)
    protected String pathToLogo;

    @OperationMethod
    public void run() throws IOException, COSVisitorException {

        log.warn("Creating statements...");

        if (nbCustomers < 1) {
            throw new RuntimeException("Less than 1 customer, really?");
        }

        parentPath = statementsFolderDoc.getPathAsString();
        firstLastNames = RandomFirstLastNames.getInstance();
        if (StringUtils.isNotBlank(pathToLogo)) {
            File f = new File(pathToLogo);
            logoImage = ImageIO.read(f);
        } else {
            logoImage = null;
        }

        if (deletePrevious) {
            log.warn("Deleting previous statements...");
            MiscUtils.deleteWithQuery(session,
                    "SELECT * FROM Document WHERE ac:kind = 'Statement'",
                    "statements");
        }

        buildCustomerNames();

        TransactionHelper.commitOrRollbackTransaction();
        TransactionHelper.startTransaction();

        countCreated = 0;
        int countCustomers = 0;
        for (String oneCustomer : customerNames) {

            createStatementsForCustomer(oneCustomer);

            countCustomers += 1;
            if ((countCustomers % 10) == 0) {
                log.warn("Created: " + countCustomers + " customers, "
                        + countCreated + " statements");
                TransactionHelper.commitOrRollbackTransaction();
                TransactionHelper.startTransaction();
            }

        }

        TransactionHelper.commitOrRollbackTransaction();
        TransactionHelper.startTransaction();

        log.warn("Creating statements: Done");

        RandomFirstLastNames.release();

    }

    protected void createStatementsForCustomer(String inCustomer)
            throws IOException, COSVisitorException {

        DocumentModel doc = null;

        int pos = inCustomer.indexOf(" ");
        String firstName = inCustomer.substring(0, pos);
        String lastName = inCustomer.substring(pos + 1);

        String titlePrefix = lastName.substring(0,
                lastName.length() < 8 ? lastName.length() - 1 : 8)
                + "-" + firstName.charAt(0);

        Calendar startCal = (Calendar) today.clone();
        int nbStatements = ToolsMisc.randomInt(3, 6);
        startCal.add(Calendar.DATE,
                -(nbStatements * (DAYS_BETWEEN_STATEMENTS + 2)));

        for (int i = 0; i < nbStatements; ++i) {

            int year = startCal.get(Calendar.YEAR);
            int month = startCal.get(Calendar.MONTH);
            int day = startCal.get(Calendar.DATE);

            String title = titlePrefix + "-" + year + "-"
                    + String.format("%02d", month + 1) + "-"
                    + String.format("%02d", day);

            Calendar inNDays = (Calendar) startCal.clone();
            inNDays.add(Calendar.DATE, DAYS_BETWEEN_STATEMENTS);

            doc = session.createDocumentModel(parentPath, title, "File");
            doc.setPropertyValue("dc:title", title);

            doc.setPropertyValue("dc:created", inNDays);
            doc.setPropertyValue("dc:modified", inNDays);
            String user = MODIF_USERS[ToolsMisc.randomInt(0, MODIF_USERS_MAX)];
            doc.setPropertyValue("dc:lastContributor", user);
            contributors[0] = user;
            doc.setPropertyValue("dc:contributors", contributors);

            doc.setPropertyValue("ac:kind", "Statement");
            doc.setPropertyValue("ac:customer_name", inCustomer);
            doc.setPropertyValue("ac:deal_id", MiscUtils.getSomeUID(6));

            doc.setPropertyValue("ac:st_start", (Serializable) startCal.clone());
            doc.setPropertyValue("ac:st_end", (Serializable) inNDays.clone());

            Blob thePdf = buildPDF(inCustomer, startCal, inNDays);
            thePdf.setFilename(title + ".pdf");
            thePdf.setMimeType("application/pdf");
            doc.setPropertyValue("file:content", (Serializable) thePdf);

            doc.setPropertyValue("ac:st_lines", statementLines);

            doc.putContextData(DublinCoreListener.DISABLE_DUBLINCORE_LISTENER,
                    true);
            doc = session.createDocument(doc);// Disable dublincore
            doc.putContextData(DublinCoreListener.DISABLE_DUBLINCORE_LISTENER,
                    true);
            doc = session.saveDocument(doc);

            // Prepare next loop
            startCal.add(Calendar.DATE, DAYS_BETWEEN_STATEMENTS + 1);
            countCreated += 1;
        }
    }

    protected Blob buildPDF(String inCustomer, Calendar inStart, Calendar inEnd)
            throws IOException, COSVisitorException {

        Blob result = null;

        PDDocument pdfDoc = new PDDocument();
        PDPage page = new PDPage();
        pdfDoc.addPage(page);
        PDRectangle rect = page.getMediaBox();
        float rectH = rect.getHeight();

        PDFont font = PDType1Font.HELVETICA;
        PDFont fontBold = PDType1Font.HELVETICA_BOLD;
        PDFont fontOblique = PDType1Font.HELVETICA_OBLIQUE;
        PDPageContentStream contentStream = new PDPageContentStream(pdfDoc,
                page);

        int line = 0;

        contentStream.beginText();
        contentStream.setFont(fontOblique, 10);
        contentStream.moveTextPositionByAmount(230, 20);
        contentStream.drawString("(Statement randomly generated)");
        contentStream.endText();

        line += 3;
        contentStream.beginText();
        contentStream.setFont(fontBold, 12);
        contentStream.moveTextPositionByAmount(300, rectH - 20 * (++line));
        contentStream.drawString(inCustomer);
        contentStream.endText();

        contentStream.beginText();
        contentStream.setFont(fontBold, 12);
        contentStream.moveTextPositionByAmount(300, rectH - 20 * (++line));
        contentStream.drawString("Statement from "
                + yyyyMMdd.format(inStart.getTime()) + " to "
                + yyyyMMdd.format(inEnd.getTime()));
        contentStream.endText();

        line += 3;
        statementLines = ToolsMisc.randomInt(3, 9);
        boolean isDebit = false;
        for (int i = 1; i <= statementLines; ++i) {
            contentStream.beginText();
            contentStream.setFont(font, 12);
            contentStream.moveTextPositionByAmount(100, rectH - 20 * line);
            contentStream.drawString("" + i);
            contentStream.endText();

            contentStream.beginText();
            contentStream.setFont(font, 12);
            contentStream.moveTextPositionByAmount(120, rectH - 20 * line);
            isDebit = ToolsMisc.randomInt(0, 10) > 7;
            if (isDebit) {
                contentStream.drawString("Withdraw Funds to account "
                        + MiscUtils.getSomeUID(6));
            } else {
                contentStream.drawString("Add Funds to account "
                        + MiscUtils.getSomeUID(6));
            }
            contentStream.endText();

            contentStream.beginText();
            if (isDebit) {
                contentStream.setFont(fontOblique, 12);
                contentStream.moveTextPositionByAmount(350, rectH - 20 * line);
            } else {
                contentStream.setFont(font, 12);
                contentStream.moveTextPositionByAmount(450, rectH - 20 * line);
            }
            contentStream.drawString("" + ToolsMisc.randomInt(1000, 9000) + "."
                    + ToolsMisc.randomInt(10, 90));
            contentStream.endText();

            line += 1;
        }
        contentStream.close();
        contentStream = null;

        if (logoImage != null) {
            PDXObjectImage ximage = new PDPixelMap(pdfDoc, logoImage);

            contentStream = new PDPageContentStream(pdfDoc, page, true, true);
            contentStream.endMarkedContentSequence();
            contentStream.drawXObject(ximage, 10, rectH - 20 - ximage.getHeight(), ximage.getWidth(),
                    ximage.getHeight());
            contentStream.close();
            contentStream = null;
        }

        result = MiscUtils.saveInTempFile(pdfDoc);
        pdfDoc.close();

        return result;

    }

    protected void buildCustomerNames() {
        log.warn("Building " + nbCustomers + " customer names...");

        customerNames = new ArrayList<String>();
        if (useCheckCustomers) {
            String nxql = "SELECT * FROM Picture WHERE ac:kind = 'Check' AND ecm:isVersion = 0 AND ecm:isProxy = 0";
            DocumentsWalker dw = new DocumentsWalker(session, nxql, 1000);
            ChecksWalkerCallback cb = new ChecksWalkerCallback();

            dw.runForEachDocument(cb);
        }

        if (customerNames.size() < nbCustomers) {
            int count = ((int) nbCustomers) - customerNames.size();
            for (int i = 0; i < count; ++i) {
                customerNames.add(firstLastNames.getAFirstName() + " "
                        + firstLastNames.getALastName());
            }
        }

        // Add our special customers (as in the checks)
        for (String specialCust : SPECIAL_CUSTOMERS) {
            if (!customerNames.contains(specialCust)) {
                customerNames.add(specialCust);
            }
        }

    }

    // -------------------- Get checks infos --------------------
    protected class ChecksWalkerCallback implements DocumentsCallback {

        long documentCount = 0;

        ReturnStatus lastReturnStatus;

        @Override
        public ReturnStatus callback(List<DocumentModel> inDocs) {

            throw new RuntimeException(
                    "Should not be here. We are walking doc by doc");
        }

        @Override
        public ReturnStatus callback(DocumentModel inDoc) {

            documentCount += 1;

            if (customerNames.size() > nbCustomers) {
                return ReturnStatus.STOP;
            } else {
                String customer = (String) inDoc.getPropertyValue("ac:customer_name");
                if (!customerNames.contains(customer)) {
                    customerNames.add(customer);
                }
            }
            return ReturnStatus.CONTINUE;
        }

        @Override
        public void init() {
            // Unused here
        }

        @Override
        public void end(ReturnStatus inLastReturnStatus) {
            lastReturnStatus = inLastReturnStatus;
        }

        public long getDocumentCount() {
            return documentCount;
        }

    }

}
