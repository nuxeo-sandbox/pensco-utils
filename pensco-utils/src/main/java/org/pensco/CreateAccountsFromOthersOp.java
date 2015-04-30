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
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.datademo.RandomDates;
import org.nuxeo.datademo.RandomUSZips;
import org.nuxeo.datademo.RandomUSZips.USZip;
import org.nuxeo.datademo.tools.DocumentsCallback;
import org.nuxeo.datademo.tools.DocumentsWalker;
import org.nuxeo.datademo.tools.ToolsMisc;
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
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.platform.dublincore.listener.DublinCoreListener;
import org.nuxeo.runtime.transaction.TransactionHelper;
import org.pensco.CreateStatementsOp.ChecksWalkerCallback;

/**
 * 
 */
@Operation(id = CreateAccountsFromOthersOp.ID, category = Constants.CAT_SERVICES, label = "Data Demo: Create Accounts from Others", description = "")
public class CreateAccountsFromOthersOp {

    public static final String ID = "CreateAccountsFromOthersOp";

    private static final Log log = LogFactory.getLog(CreateAccountsFromOthersOp.class);

    public static final int MODULO_FOR_COMMIT = 50;

    protected static final String[] MODIF_USERS = { "john", "john", "john",
            "jim", "kate", "kate", "Administrator" };

    protected static final int MODIF_USERS_MAX = MODIF_USERS.length - 1;

    protected DateFormat yyyyMMdd = new SimpleDateFormat("yyyy-MM-dd");

    protected DateFormat yyyy = new SimpleDateFormat("yyyy");

    protected Calendar today = Calendar.getInstance();

    protected String parentPath;
    
    protected RandomUSZips usZips;

    protected static String[] MAIN_US_STATES = { "TX", "NY", "NY", "NY", "NY", "CA", "CA", "PA", "IL",
            "OH", "MO", "MO", "MA", "MA", "FL", "FL" };

    protected static final int MAIN_US_STATES_MAX = MAIN_US_STATES.length - 1;

    // To avoid allocating an array for each document created
    protected String[] contributors = { "" };

    // Same for this one
    protected String[] accounts = { "" };
    
    @Context
    protected CoreSession session;

    @Param(name = "accountsFolderDoc", required = true)
    protected DocumentModel accountsFolderDoc;

    @Param(name = "deletePrevious", required = false, values = { "true" })
    protected boolean deletePrevious = true;

    @OperationMethod
    public void run() throws IOException {

        log.warn("Create accounts from checks and statements: Start");

        parentPath = accountsFolderDoc.getPathAsString();
        
        usZips = RandomUSZips.getInstance();
        
        if (deletePrevious) {
            log.warn("Deleting previous accounts...");
            MiscUtils.deleteWithQuery(session,
                    "SELECT * FROM Account",
                    "accounts");
        }

        TransactionHelper.commitOrRollbackTransaction();
        TransactionHelper.startTransaction();

        String nxql = "SELECT * FROM Document WHERE ac:customer_name IS NOT NULL AND ac:customer_name != ''";
        DocumentsWalker dw = new DocumentsWalker(session, nxql, 1000);
        CreateAccountsWalkerCallback cb = new CreateAccountsWalkerCallback();

        dw.runForEachDocument(cb);


        TransactionHelper.commitOrRollbackTransaction();
        TransactionHelper.startTransaction();
        log.warn("Create accounts from checks and statements: Done");
        
        RandomUSZips.release();
    }

    // -------------------- Get customer_name infos --------------------
    // NOT OPTIMIZED AT ALL, would not scale, would be quite slow for thousands
    // of values (we query for each cutsomer etc.)
    protected class CreateAccountsWalkerCallback implements DocumentsCallback {

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

            String customer = (String) inDoc.getPropertyValue("ac:customer_name");
            
            createAccountIfNeededAndUpdateDoc(customer, inDoc);
            
            if((documentCount % MODULO_FOR_COMMIT) == 0) {

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
    
    protected void createAccountIfNeededAndUpdateDoc(String inName, DocumentModel inDoc) {
        
        DocumentModel account;
        DocumentModelList docs = session.query("SELECT * FROM Account WHERE account:customer_name = \"" + inName + "\"  AND ecm:isVersion = 0 AND ecm:isProxy = 0");
        if(docs.size() == 0) {
            account = createAccount(inName);
        } else {
            account = docs.get(0);
        }
        
        String accountId = (String) account.getPropertyValue("account:account_id");
        inDoc.setPropertyValue("ac:account_main", accountId);
        inDoc.setPropertyValue("ac:account_city", account.getPropertyValue("account:city"));
        inDoc.setPropertyValue("ac:account_state", account.getPropertyValue("account:state"));
        accounts[0] = accountId;
        inDoc.setPropertyValue("ac:accounts", accounts);
        
        inDoc.putContextData(DublinCoreListener.DISABLE_DUBLINCORE_LISTENER, true);
        inDoc = session.saveDocument(inDoc);
        
    }
    
    protected DocumentModel createAccount(String inName) {
        
        DocumentModel doc = null;
        
        doc = session.createDocumentModel(parentPath, inName, "Account");

        doc.setPropertyValue("dc:title", inName);

        Calendar created = RandomDates.buildDate(null, 0, 250, true);
        doc.setPropertyValue("dc:created", created);
        doc.setPropertyValue("dc:modified", created);

        String user = MODIF_USERS[ToolsMisc.randomInt(0, MODIF_USERS_MAX)];
        doc.setPropertyValue("dc:lastContributor", user);
        contributors[0] = user;
        doc.setPropertyValue("dc:contributors", contributors);
        
        doc.setPropertyValue("account:account_id", MiscUtils.getSomeUID(10));
        doc.setPropertyValue("account:customer_name", inName);
        USZip zip;
        if (ToolsMisc.randomInt(1, 10) > 7) {
            zip = usZips.getAZip(MAIN_US_STATES[ToolsMisc.randomInt(0,
                    MAIN_US_STATES_MAX)]);
        } else {
            zip = usZips.getAZip();
        }
        doc.setPropertyValue("account:state", zip.state);
        doc.setPropertyValue("account:city", zip.city);
        
        doc.putContextData(DublinCoreListener.DISABLE_DUBLINCORE_LISTENER, true);
        doc = session.createDocument(doc);// Disable dublincore
        doc.putContextData(DublinCoreListener.DISABLE_DUBLINCORE_LISTENER, true);
        doc = session.saveDocument(doc);
        
        
        return doc;
    }
}
