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
import java.util.UUID;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.pdfbox.exceptions.COSVisitorException;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.impl.blob.FileBlob;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.transaction.TransactionHelper;

/**
 * 
 *
 * @since TODO
 */
public class MiscUtils {

    public static final Log log = LogFactory.getLog(MiscUtils.class);

    static public void deleteWithQuery(CoreSession inSession, String inNXQL, String inDeleteWhat) {

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
}
