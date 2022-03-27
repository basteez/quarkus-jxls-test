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
import java.util.Arrays;
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
        try{
            byte[] excelBytes = getExcelByteArray();

                Response resp = Response.ok()
                        .type("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                        .header("Content-Disposition", "filename=\"export.xls\"")
                        .entity(excelBytes)
                        .build();

                return resp;
        }catch(Exception e){
            logger.errorv(e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }


    @GET
    @Path("/export-dinamico")
    public Response exportDinamico(@QueryParam("cols") String cols){
        byte[] excelByteArray = new byte[]{};
        InputStream is = null;
        ByteArrayOutputStream os = null;
        try{
            if(cols ==  null || cols.isBlank()){
                excelByteArray = getExcelByteArray();
            }else{
                List<String> headers = Arrays.asList(cols.split(","));
                List<Employee> export = getEmployees();
                is = EmployeeRS.class.getResourceAsStream("/templategrid.xls");
                os = new ByteArrayOutputStream();
                org.jxls.common.Context context = new org.jxls.common.Context();
                context.putVar("headers", headers);
                context.putVar("export", export);
                JxlsHelper.getInstance().processGridTemplateAtCell(is, os, context, cols, "Sheet1!A1");
                excelByteArray = os.toByteArray();
            }

                Response resp = Response.ok()
                    .type("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                    .header("Content-Disposition", "filename=\"export.xls\"")
                    .entity(excelByteArray)
                    .build();

                return resp;
        }catch (Exception e){
            logger.errorv(e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }finally {
            try{
                if(is != null) is.close();
                if(os != null) os.close();
            }catch (Exception e){
                //TODO gestire eccezione
            }
        }
    }

    public byte[] getExcelByteArray() throws Exception{
        List<Employee> employeeList = getEmployees();
        try(InputStream is = EmployeeRS.class.getResourceAsStream("/template.xls")) { //1
            try (ByteArrayOutputStream os = new ByteArrayOutputStream()) { //2
                Context context = new Context(); //3
                context.putVar("employees", employeeList); //4
                JxlsHelper.getInstance().processTemplate(is, os, context); //5
                return os.toByteArray();
            }
        }catch (Exception e){
            throw new Exception("Errore nell'elaborare i dati");
        }
    }
}
