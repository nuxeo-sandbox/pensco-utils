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

import org.apache.commons.lang.StringUtils;
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

/**
 * 
 */
@Operation(id = AccountHandlerOp.ID, category = Constants.CAT_DOCUMENT, label = "AccountHandlerOp", description = "")
public class AccountHandlerOp {

    public static final String ID = "AccountHandlerOp";

    @Context
    protected CoreSession session;

    @Param(name = "accountsFolderPath", required = true)
    protected String accountsFolderPath;

    @Param(name = "save", required = false, values = { "false" })
    protected boolean save = false;

    @OperationMethod(collector = DocumentModelCollector.class)
    public DocumentModel run(DocumentModel inDoc) throws IOException {

        if (!inDoc.hasSchema("ApplicationCommon")) {
            return inDoc;
        }

        String customer = (String) inDoc.getPropertyValue("ac:customer_name");

        if (StringUtils.isNotBlank(customer)) {

            MiscUtils.createAccountIfNeededAndUpdateDoc(session,
                    accountsFolderPath, customer, inDoc, save, false);
        }

        return inDoc;
    }

}
