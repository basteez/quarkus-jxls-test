package com.basteez.jxlstest.service.rest;

import com.basteez.jxlstest.model.Employee;
import io.quarkus.hibernate.orm.panache.PanacheQuery;
import org.jboss.logging.Logger;
import org.jxls.common.Context;
import org.jxls.util.JxlsHelper;

import javax.inject.Singleton;
import javax.transaction.Transactional;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.List;

@Path("/employee")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Singleton
public class EmployeeRS {
    static Logger logger = Logger.getLogger(EmployeeRS.class);

    public List<Employee> getEmployees(){
        PanacheQuery<Employee> q = Employee.find("select e from Employee e order by lastName asc");
        return q.list();
    }

    @GET
    public Response getList(){
        List<Employee> employeeList = getEmployees();
        return Response.ok(employeeList).build();
    }

    @POST
    @Transactional
    public Response persist(Employee employee){
        Employee.persist(employee);
        return Response.ok(employee).build();
    }

    @GET
    @Path("/export")
    public Response exportEmployees(){
        logger.info("Export started");
        List<Employee> employeeList = getEmployees();
        try(InputStream is = EmployeeRS.class.getResourceAsStream("/template.xls")){
            try(ByteArrayOutputStream os = new ByteArrayOutputStream()){
                Context context = new Context();
                context.putVar("employees", employeeList);
                JxlsHelper.getInstance().processTemplate(is, os, context);

                Response resp = Response.ok()
//                        .type("application/vnd.ms-excel")
                        .type("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                        .header("Content-Disposition", "filename=\"export.xls\"").entity(os.toByteArray()).build();

                return resp;
            }
        }catch(Exception e){
            logger.errorv(e.getMessage());
        }
        return null;
    }
}
