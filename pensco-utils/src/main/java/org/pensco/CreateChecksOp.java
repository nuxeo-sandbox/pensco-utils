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
 *     Thibaud Arguillere
 */
/*
 * WARNING WARNING WARNING
 * 
 *  About all is hard coded, and/orcopy/paste form other plug-ins
 *  The goals is just to quickly create some data, not to build the state-of-the-art example :->
 */

package org.pensco;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.blobholder.SimpleBlobHolder;
import org.nuxeo.ecm.core.api.impl.blob.FileBlob;
import org.nuxeo.ecm.core.convert.api.ConversionService;
import org.nuxeo.ecm.platform.dublincore.listener.DublinCoreListener;
import org.nuxeo.runtime.transaction.TransactionHelper;
import org.nuxeo.datademo.RandomDates;
import org.nuxeo.datademo.RandomFirstLastNames;
import org.nuxeo.datademo.tools.ToolsMisc;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 
 * Something VERY IMPORTANT: The values must be the same as the one used in the
 * Studio project.
 */
@Operation(id = CreateChecksOp.ID, category = Constants.CAT_SERVICES, label = "Data Demo: Create Checks", description = "")
public class CreateChecksOp {

    public static final String ID = "CreateChecksOp";

    private static final Log log = LogFactory.getLog(CreateChecksOp.class);

    public static final int MODULO_FOR_COMMIT = 50;

    public static final String CONVERTER_WITH_DRAW_TEXT = "modifyCheck-withDrawText";

    public static final String CONVERTER_NAME_AND_OTHERS = "modifyCheck-nameAndOthers";

    public static final int HOW_MANY = 2000;

    // IDs of the "Banks" vocabulary in Studio project
    public static final String[] BANKS = { "Bank of America",
            "Bank of America", "Citigroup", "Citigroup", "Citigroup",
            "JP Morgan Chase", "Wells Fargo", "Wells Fargo", "Capital One",
            "Capital One", "Capital One", "Capital One", "Capital One",
            "Barclays PLC", "Morgan Stanley", "Morgan Stanley",
            "Morgan Stanley" };

    public static final int BANKS_MAX = BANKS.length - 1;
    
    public static final String[] SPECIAL_CUSTOMERS = {"JOHN SMITH", "JOHN SMITH",
                                    "SUSAN JOHNSON", "SUSAN JOHNSON", "SUSAN JOHNSON",
                                    "DAVID JONES", "DAVID JONES",
                                    "LISA WILLIAMS",
                                    "WILLIAM MOORE"};
    
    public static final int SPECIAL_CUSTOMERS_MAX = SPECIAL_CUSTOMERS.length - 1;

    protected static final String[] MODIF_USERS = { "john", "john", "john",
            "jim", "kate", "kate", "Administrator" };

    protected static final int MODIF_USERS_MAX = MODIF_USERS.length - 1;

    protected RandomFirstLastNames firstLastNames;

    protected DateFormat yyyyMMdd = new SimpleDateFormat("yyyy-MM-dd");

    protected DateFormat yyyy = new SimpleDateFormat("yyyy");

    protected Calendar today = Calendar.getInstance();

    protected String parentPath;
    

    // To avoid allocating an array for each document created
    protected String[] contributors = { "" };

    @Context
    protected CoreSession session;

    @Context
    protected ConversionService conversionService;

    @Param(name = "pathToBaseChecks", required = true)
    protected String pathToBaseChecks = "";

    @Param(name = "checksFolderDoc", required = true)
    protected DocumentModel checksFolderDoc;

    @Param(name = "deletePrevious", required = false, values = { "true" })
    protected boolean deletePrevious = true;

    @OperationMethod
    public void run() throws IOException {

        log.warn("Creating checks...");

        parentPath = checksFolderDoc.getPathAsString();

        TransactionHelper.commitOrRollbackTransaction();
        TransactionHelper.startTransaction();

        if (deletePrevious) {
            deletePreviousChecks();
        }

        firstLastNames = RandomFirstLastNames.getInstance();

        File parentFolder = new File(pathToBaseChecks);
        File[] allFiles = parentFolder.listFiles();
        ArrayList<File> bases = new ArrayList<File>();
        for (File oneFile : allFiles) {
            if (!oneFile.isHidden()) {
                bases.add(oneFile);
            }
        }
        int maxBases = bases.size() - 1;

        for (int i = 1; i <= HOW_MANY; ++i) {

            createOneCheck(bases.get(ToolsMisc.randomInt(0, maxBases)));

            if ((i % MODULO_FOR_COMMIT) == 0) {
                log.warn("Created: " + i + "/" + HOW_MANY);
                TransactionHelper.commitOrRollbackTransaction();
                TransactionHelper.startTransaction();
            }
        }

        TransactionHelper.commitOrRollbackTransaction();
        TransactionHelper.startTransaction();

        log.warn("Creating checks: Done");

    }

    protected void createOneCheck(File inBasePict) {

        DocumentModel doc = null;

        String checkNumber = getSomeUID(15);

        doc = session.createDocumentModel(parentPath, checkNumber, "Picture");

        doc.setPropertyValue("dc:title", checkNumber);

        Calendar created = RandomDates.buildDate(null, 0, 250, true);
        doc.setPropertyValue("dc:created", created);
        doc.setPropertyValue("dc:modified", created);

        String user = MODIF_USERS[ToolsMisc.randomInt(0, MODIF_USERS_MAX)];
        doc.setPropertyValue("dc:lastContributor", user);
        contributors[0] = user;
        doc.setPropertyValue("dc:contributors", contributors);
        
        doc.setPropertyValue("ac:kind", "Check");
        
        String customer;
        if(ToolsMisc.randomInt(1, 10) > 9) {
            customer = SPECIAL_CUSTOMERS[ToolsMisc.randomInt(0, SPECIAL_CUSTOMERS_MAX)];
        } else {
            customer = firstLastNames.getAFirstName() + " " + firstLastNames.getALastName();
        }
        doc.setPropertyValue("ac:customer_name", customer);

        doc.setPropertyValue("ac:deal_id", getSomeUID(6));
        
        // Setup the check
        String amountStr = "" + ToolsMisc.randomInt(1000, 5000) + "." + ToolsMisc.randomInt(0, 99);
        double amount = Double.valueOf(amountStr);
        String bank = BANKS[ToolsMisc.randomInt(0, BANKS_MAX)];
        String creationDateStr = yyyyMMdd.format(created.getTime());
        
        doc.setPropertyValue("ac:check_amount", amount);
        doc.setPropertyValue("ac:check_bank", bank);
        doc.setPropertyValue("ac:check_date", created);
        doc.setPropertyValue("ac:check_number", checkNumber);
        
        Blob checkPict = buildCheckPicture(inBasePict, creationDateStr, amountStr, bank, checkNumber, customer);
        doc.setPropertyValue("file:content", (Serializable) checkPict);
        

        doc.putContextData(DublinCoreListener.DISABLE_DUBLINCORE_LISTENER, true);
        doc = session.createDocument(doc);// Disable dublincore
        doc.putContextData(DublinCoreListener.DISABLE_DUBLINCORE_LISTENER, true);
        doc = session.saveDocument(doc);

    }
    
    protected String getSomeUID(int size) {
        
        return UUID.randomUUID().toString().replace("-", "").toUpperCase().substring(1, size);
    }
    
    protected Blob buildCheckPicture(File inBase, String inDate, String inAmount, String inBank, String inNumber, String inCustomer) {
        
        Blob result = null;
        BlobHolder holder;
        
        String fileName = inBase.getName();
        int pos = fileName.lastIndexOf('.');
        String ext = fileName.substring(pos + 1);
        
        fileName = inNumber + "." + ext;
        
        Map<String, Serializable> params = new HashMap<>();
        
        FileBlob fb = new FileBlob(inBase);
        String mimeType = fb.getMimeType();
        
        // Date
        params.put("textValue", "text 130,45 '" + inDate + "'");
        params.put("targetFileName", fileName);
        holder = conversionService.convert(CONVERTER_WITH_DRAW_TEXT, new SimpleBlobHolder(fb), params);
        result = holder.getBlob();
        result.setFilename(fileName);
        result.setMimeType(mimeType);
        
        // Amount
        params.clear();
        params.put("textValue", "text 60,100 '" + inAmount + "'");
        params.put("targetFileName", fileName);
        holder = conversionService.convert(CONVERTER_WITH_DRAW_TEXT, new SimpleBlobHolder(result), params);
        result = holder.getBlob();
        result.setFilename(fileName);
        result.setMimeType(mimeType);
        
        // Bank
        params.clear();
        params.put("textValue", "text 445,180 '" + inBank + "'");
        params.put("targetFileName", fileName);
        holder = conversionService.convert(CONVERTER_WITH_DRAW_TEXT, new SimpleBlobHolder(result), params);
        result = holder.getBlob();
        result.setFilename(fileName);
        result.setMimeType(mimeType);
        
        // Order
        params.clear();
        params.put("textValue", "text 380,100 'PENSCO'");
        params.put("targetFileName", fileName);
        holder = conversionService.convert(CONVERTER_WITH_DRAW_TEXT, new SimpleBlobHolder(result), params);
        result = holder.getBlob();
        result.setFilename(fileName);
        result.setMimeType(mimeType);
        
        // Customer name, checknumber, ...
        params.clear();
        params.put("textValue", inCustomer + "\\n\\n" + inNumber + " • " + getSomeUID(10));
        params.put("targetFileName", fileName);
        holder = conversionService.convert(CONVERTER_NAME_AND_OTHERS, new SimpleBlobHolder(result), params);
        result = holder.getBlob();
        result.setFilename(fileName);
        result.setMimeType(mimeType);
        
        return result;
    }
    
    protected void deletePreviousChecks() {

        log.warn("Creating checks: Deleting existing Checks");

        String nxql = "SELECT * FROM Document WHERE ac:kind = 'Check'";
        int size;
        do {
            DocumentModelList docs = session.query(nxql);
            size = docs.size();
            if (size > 0) {

                log.warn("Deleting " + size + " 'Checks'...");
                deleteTheseDocs(docs);
            }

        } while (size > 0);
        
    }

    protected void deleteTheseDocs(DocumentModelList inDocs) {

        int size = inDocs.size();
        if (size > 0) {
            DocumentRef[] docRefs = new DocumentRef[size];
            for (int i = 0; i < size; i++) {
                docRefs[i] = inDocs.get(i).getRef();
            }
            session.removeDocuments(docRefs);
            TransactionHelper.commitOrRollbackTransaction();
            TransactionHelper.startTransaction();            
        }

    }

}
