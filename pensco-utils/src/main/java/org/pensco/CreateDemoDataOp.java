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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.model.Property;
import org.nuxeo.ecm.core.api.model.impl.ArrayProperty;
import org.nuxeo.ecm.core.lifecycle.LifeCycleException;
import org.nuxeo.ecm.platform.dublincore.listener.DublinCoreListener;
import org.nuxeo.ecm.platform.uidgen.UIDSequencer;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.transaction.TransactionHelper;

import java.io.Serializable;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;

/**
 * Create n Affaire/AffairePrestExt (1/2 of each), with pseudo-random values.
 * "pseudo" because we make sure some brands ("marques") and some
 * "contrevenants" are more involved than others, so showing stats looks cool
 * (not an eve repartition between each)
 * 
 * Hopefully, reading the code will be clear enough (yes, this is what you say
 * when you don't write a doc)
 * 
 * Something VERY IMPORTANT: The values must be the same as the one used in the
 * Studio project.
 */
@Operation(id = CreateDemoDataOp.ID, category = Constants.CAT_SERVICES, label = "CreateDemoDataOp", description = "")
public class CreateDemoDataOp {

    public static final String ID = "CreateDemoDataOp";

    private static final Log log = LogFactory.getLog(CreateDemoDataOp.class);

    @Context
    protected CoreSession session;

    @OperationMethod
    public void run() throws DocumentException, LifeCycleException {

        log.warn("Running...");
    }

}
