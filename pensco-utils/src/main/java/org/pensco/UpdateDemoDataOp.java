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

import java.io.IOException;
import java.util.Calendar;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.datademo.tools.DocumentsCallback;
import org.nuxeo.datademo.tools.DocumentsWalker;
import org.nuxeo.datademo.tools.DocumentsCallback.ReturnStatus;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.automation.core.collectors.DocumentModelCollector;
import org.nuxeo.ecm.automation.core.collectors.BlobCollector;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.platform.dublincore.listener.DublinCoreListener;
import org.nuxeo.runtime.transaction.TransactionHelper;
import org.pensco.CreateAccountsFromOthersOp.CreateAccountsWalkerCallback;

/**
 * 
 */
@Operation(id=UpdateDemoDataOp.ID, category=Constants.CAT_SERVICES, label="UpdateDemoDataOp", description="")
public class UpdateDemoDataOp {

    public static final String ID = "UpdateDemoDataOp";
    
    private static final Log log = LogFactory.getLog(UpdateDemoDataOp.class);

    public static final int MODULO_FOR_COMMIT = 50;

    protected Calendar today = Calendar.getInstance();

    @Context
    protected CoreSession session;

    @OperationMethod
    public void run() {
       
        log.warn("Update demo data: Start");
        /*
         TransactionHelper.commitOrRollbackTransaction();
        TransactionHelper.startTransaction();

        String nxql = "SELECT * FROM Document WHERE ac:customer_name IS NOT NULL AND ac:customer_name != ''";
        DocumentsWalker dw = new DocumentsWalker(session, nxql, 1000);
        CreateAccountsWalkerCallback cb = new CreateAccountsWalkerCallback();

        dw.runForEachDocument(cb);

        TransactionHelper.commitOrRollbackTransaction();
        TransactionHelper.startTransaction();
        log.warn("Create accounts from checks and statements: Done");
         */
        TransactionHelper.commitOrRollbackTransaction();
        TransactionHelper.startTransaction();
        
        String nxql = "SELECT * FROM Document WHERE ecm:primaryType IN ('Picture', 'File') AND ecm:isVersion = 0 AND ecm:isProxy = 0";
        DocumentsWalker dw = new DocumentsWalker(session, nxql, 1000);
        UpdateDataWalkerCallback cb = new UpdateDataWalkerCallback();
        
        dw.runForEachDocument(cb);
        
        TransactionHelper.commitOrRollbackTransaction();
        TransactionHelper.startTransaction();

        log.warn("Update demo data: Done");
    }
    
 // -------------------- Get customer_name infos --------------------
    // NOT OPTIMIZED AT ALL, would not scale, would be quite slow for thousands
    // of values (we query for each cutsomer etc.)
    protected class UpdateDataWalkerCallback implements DocumentsCallback {

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

            Calendar d = (Calendar) inDoc.getPropertyValue("dc:created");
            Calendar exp = (Calendar) d.clone();
            
            exp.add(Calendar.DATE, 200);
            inDoc.setPropertyValue("dc:expired", exp);
            
            inDoc.setPropertyValue("ac:is_expired", today.after(exp));
            
            inDoc.putContextData(DublinCoreListener.DISABLE_DUBLINCORE_LISTENER, true);
            inDoc = session.saveDocument(inDoc);

            if ((documentCount % MODULO_FOR_COMMIT) == 0) {

                log.warn("Updated: " + documentCount);

                TransactionHelper.commitOrRollbackTransaction();
                TransactionHelper.startTransaction();

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
