/*
 * Copyright 2015-2016 Jeeva Kandasamy (jkandasa@gmail.com)
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.mycontroller.standalone.api.jaxrs;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

import java.util.HashMap;
import java.util.List;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.mycontroller.standalone.api.jaxrs.json.Query;
import org.mycontroller.standalone.api.jaxrs.json.QueryResponse;
import org.mycontroller.standalone.api.jaxrs.utils.RestUtils;
import org.mycontroller.standalone.db.DaoUtils;
import org.mycontroller.standalone.db.tables.OperationTable;
import org.mycontroller.standalone.operation.OperationUtils;
import org.mycontroller.standalone.operation.OperationUtils.OPERATION_TYPE;
import org.mycontroller.standalone.operation.model.Operation;

/**
 * @author Jeeva Kandasamy (jkandasa)
 * @since 0.0.3
 */

@Path("/rest/operations")
@Produces(APPLICATION_JSON)
@Consumes(APPLICATION_JSON)
@RolesAllowed({ "Admin" })
public class OperationHandler extends AccessEngine {

    @GET
    @Path("/{id}")
    public Response get(@PathParam("id") int id) {
        return RestUtils.getResponse(Status.OK, DaoUtils.getOperationDao().getById(id));
    }

    @GET
    @Path("/")
    public Response getAll(
            @QueryParam(OperationTable.KEY_NAME) List<String> name,
            @QueryParam(OperationTable.KEY_PUBLIC_ACCESS) Boolean publicAccess,
            @QueryParam(OperationTable.KEY_TYPE) String type,
            @QueryParam(OperationTable.KEY_ENABLED) Boolean enabled,
            @QueryParam(Query.PAGE_LIMIT) Long pageLimit,
            @QueryParam(Query.PAGE) Long page,
            @QueryParam(Query.ORDER_BY) String orderBy,
            @QueryParam(Query.ORDER) String order) {
        HashMap<String, Object> filters = new HashMap<String, Object>();

        filters.put(OperationTable.KEY_NAME, name);
        filters.put(OperationTable.KEY_TYPE, OPERATION_TYPE.fromString(type));
        filters.put(OperationTable.KEY_PUBLIC_ACCESS, publicAccess);
        filters.put(OperationTable.KEY_ENABLED, enabled);

        QueryResponse queryResponse = DaoUtils.getOperationDao().getAll(
                Query.builder()
                        .order(order != null ? order : Query.ORDER_ASC)
                        .orderBy(orderBy != null ? orderBy : OperationTable.KEY_ID)
                        .filters(filters)
                        .pageLimit(pageLimit != null ? pageLimit : Query.MAX_ITEMS_PER_PAGE)
                        .page(page != null ? page : 1L)
                        .build());
        return RestUtils.getResponse(Status.OK, queryResponse);
    }

    @POST
    @Path("/")
    public Response add(Operation operation) {
        operation.setUser(getUser());
        DaoUtils.getOperationDao().create(operation.getOperationTable());
        return RestUtils.getResponse(Status.CREATED);
    }

    @PUT
    @Path("/")
    public Response update(Operation operation) {
        operation.setUser(getUser());
        if (!operation.getEnabled()) {
            OperationUtils.unloadOperationTimerJobs(operation.getOperationTable());
        }
        DaoUtils.getOperationDao().update(operation.getOperationTable());
        return RestUtils.getResponse(Status.NO_CONTENT);
    }

    @POST
    @Path("/delete")
    public Response deleteIds(List<Integer> ids) {
        OperationUtils.unloadNotificationTimerJobs(ids);
        DaoUtils.getOperationDao().deleteByIds(ids);
        return RestUtils.getResponse(Status.NO_CONTENT);
    }

    @POST
    @Path("/enable")
    public Response enableIds(List<Integer> ids) {
        for (OperationTable operationTable : DaoUtils.getOperationDao().getAll(ids)) {
            operationTable.setEnabled(true);
            DaoUtils.getOperationDao().update(operationTable);
        }
        return RestUtils.getResponse(Status.NO_CONTENT);
    }

    @POST
    @Path("/disable")
    public Response disableIds(List<Integer> ids) {
        OperationUtils.unloadNotificationTimerJobs(ids);
        for (OperationTable operationTable : DaoUtils.getOperationDao().getAll(ids)) {
            operationTable.setEnabled(false);
            DaoUtils.getOperationDao().update(operationTable);
        }
        return RestUtils.getResponse(Status.NO_CONTENT);
    }

}