/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and contributors.
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

import java.io.File;
import java.io.IOException;
import java.util.Calendar;
import java.util.UUID;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.pdfbox.exceptions.COSVisitorException;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.nuxeo.datademo.RandomDates;
import org.nuxeo.datademo.RandomUSZips;
import org.nuxeo.datademo.RandomUSZips.USZip;
import org.nuxeo.datademo.tools.ToolsMisc;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.impl.blob.FileBlob;
import org.nuxeo.ecm.platform.dublincore.listener.DublinCoreListener;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.transaction.TransactionHelper;

/**
 * 
 *
 * @since TODO
 */
public class MiscUtils {

    public static final Log log = LogFactory.getLog(MiscUtils.class);

    public static RandomUSZips usZips;

    static public void deleteWithQuery(CoreSession inSession, String inNXQL,
            String inDeleteWhat) {

        int size;
        do {
            DocumentModelList docs = inSession.query(inNXQL);
            size = docs.size();
            if (size > 0) {

                log.warn("Deleting " + size + " " + inDeleteWhat + "...");

                DocumentRef[] docRefs = new DocumentRef[size];
                for (int i = 0; i < size; i++) {
                    docRefs[i] = docs.get(i).getRef();
                }
                inSession.removeDocuments(docRefs);
                TransactionHelper.commitOrRollbackTransaction();
                TransactionHelper.startTransaction();
            }

        } while (size > 0);
    }

    public static String getSomeUID(int size) {

        return UUID.randomUUID().toString().replace("-", "").toUpperCase().substring(
                1, size);
    }

    public static FileBlob saveInTempFile(PDDocument inPdfDoc)
            throws IOException, COSVisitorException {

        FileBlob result = null;

        File tempFile = File.createTempFile("pensco-utils-", ".pdf");
        inPdfDoc.save(tempFile);
        result = new FileBlob(tempFile);
        result.setMimeType("application/pdf");
        Framework.trackFile(tempFile, result);

        return result;
    }

    public static DocumentModel createAccountIfNeededAndUpdateDoc(
            CoreSession inSession, String inParentPath, String inName,
            DocumentModel inDoc, boolean inSave, boolean inDisableDublincore)
            throws IOException {
        DocumentModel account;
        DocumentModelList docs = inSession.query("SELECT * FROM Account WHERE account:customer_name = \""
                + inName + "\"  AND ecm:isVersion = 0 AND ecm:isProxy = 0");
        if (docs.size() == 0) {
            account = MiscUtils.createAccountForCustomer(inSession,
                    inParentPath, inName);
        } else {
            account = docs.get(0);
        }

        String accountId = (String) account.getPropertyValue("account:account_id");
        inDoc.setPropertyValue("ac:account_main", accountId);
        inDoc.setPropertyValue("ac:account_city",
                account.getPropertyValue("account:city"));
        inDoc.setPropertyValue("ac:account_state",
                account.getPropertyValue("account:state"));
        String[] accounts = { accountId };
        inDoc.setPropertyValue("ac:accounts", accounts);

        if (inSave) {
            if (inDisableDublincore) {
                inDoc.putContextData(
                        DublinCoreListener.DISABLE_DUBLINCORE_LISTENER, true);
            }
            inDoc = inSession.saveDocument(inDoc);
        }

        return inDoc;
    }

    public static DocumentModel createAccountForCustomer(CoreSession inSession,
            String inParentPath, String inCustomerName) throws IOException {

        // We will not release it
        if (usZips == null) {
            usZips = RandomUSZips.getInstance();
        }

        DocumentModel doc = null;
        Calendar today = Calendar.getInstance();

        doc = inSession.createDocumentModel(inParentPath, inCustomerName,
                "Account");
        doc.setPropertyValue("dc:title", inCustomerName);

        doc.setPropertyValue("dc:created", today);
        doc.setPropertyValue("dc:modified", today);

        String user = CreateAccountsFromOthersOp.MODIF_USERS[ToolsMisc.randomInt(
                0, CreateAccountsFromOthersOp.MODIF_USERS_MAX)];
        doc.setPropertyValue("dc:lastContributor", user);
        String[] contributors = { user };
        doc.setPropertyValue("dc:contributors", contributors);

        doc.setPropertyValue("account:account_id", MiscUtils.getSomeUID(10));
        doc.setPropertyValue("account:customer_name", inCustomerName);

        USZip zip;
        if (ToolsMisc.randomInt(1, 10) > 7) {
            zip = usZips.getAZip(CreateAccountsFromOthersOp.MAIN_US_STATES[ToolsMisc.randomInt(
                    0, CreateAccountsFromOthersOp.MAIN_US_STATES_MAX)]);
        } else {
            zip = usZips.getAZip();
        }
        doc.setPropertyValue("account:state", zip.state);
        doc.setPropertyValue("account:city", zip.city);

        doc.putContextData(DublinCoreListener.DISABLE_DUBLINCORE_LISTENER, true);
        doc = inSession.createDocument(doc);// Disable dublincore
        doc.putContextData(DublinCoreListener.DISABLE_DUBLINCORE_LISTENER, true);
        doc = inSession.saveDocument(doc);

        return doc;
    }
}
