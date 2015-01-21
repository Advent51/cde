/*!
* Copyright 2002 - 2015 Webdetails, a Pentaho company.  All rights reserved.
*
* This software was developed by Webdetails and is provided under the terms
* of the Mozilla Public License, Version 2.0, or any later version. You may not use
* this file except in compliance with the license. If you need a copy of the license,
* please go to  http://mozilla.org/MPL/2.0/. The Initial Developer is Webdetails.
*
* Software distributed under the Mozilla Public License is distributed on an "AS IS"
* basis, WITHOUT WARRANTY OF ANY KIND, either express or  implied. Please refer to
* the license for the specific language governing your rights and limitations.
*/

package pt.webdetails.cdf.dd.api;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.pentaho.platform.api.engine.ILogger;
import org.pentaho.platform.api.engine.IParameterProvider;
import org.pentaho.platform.api.engine.IPentahoSession;
import org.pentaho.platform.engine.core.solution.SimpleParameterProvider;
import org.pentaho.platform.engine.core.system.PentahoSessionHolder;
import org.pentaho.platform.util.logging.SimpleLogger;
import pt.webdetails.cdf.dd.CdeEngine;
import pt.webdetails.cdf.dd.DashboardManager;
import pt.webdetails.cdf.dd.ICdeEnvironment;
import pt.webdetails.cdf.dd.InterPluginBroker;
import pt.webdetails.cdf.dd.MetaModelManager;
import pt.webdetails.cdf.dd.editor.DashboardEditor;
import pt.webdetails.cdf.dd.model.core.writer.ThingWriteException;
import pt.webdetails.cdf.dd.model.inst.writer.cdfrunjs.dashboard.CdfRunJsDashboardWriteOptions;
import pt.webdetails.cdf.dd.model.inst.writer.cdfrunjs.dashboard.CdfRunJsDashboardWriteResult;
import pt.webdetails.cdf.dd.structure.DashboardWcdfDescriptor;
import pt.webdetails.cdf.dd.util.CdeEnvironment;
import pt.webdetails.cdf.dd.util.Utils;
import pt.webdetails.cpf.Util;
import pt.webdetails.cpf.audit.CpfAuditHelper;
import pt.webdetails.cpf.localization.MessageBundlesHelper;
import pt.webdetails.cpf.repository.api.IReadAccess;
import pt.webdetails.cpf.utils.MimeTypes;

@Path( "pentaho-cdf-dd/api/renderer" )
public class RenderApi {

  private static final Log logger = LogFactory.getLog( RenderApi.class );
  //  private static final String MIME_TYPE = "text/html";
  protected ICdeEnvironment privateEnviroment;

  @GET
  @Path( "/getComponentDefinitions" )
  @Produces( MimeTypes.JAVASCRIPT )
  public String getComponentDefinitions( @Context HttpServletResponse response ) throws IOException {
    // Get and output the definitions
    return MetaModelManager.getInstance().getJsDefinition();
  }

  @GET
  @Path( "/getContent" )
  @Produces( MimeTypes.JAVASCRIPT )
  public String getContent( @QueryParam( MethodParams.SOLUTION ) @DefaultValue( "" ) String solution,
                            @QueryParam( MethodParams.PATH ) @DefaultValue( "" ) String path,
                            @QueryParam( MethodParams.FILE ) @DefaultValue( "" ) String file,
                            @QueryParam( MethodParams.INFERSCHEME ) @DefaultValue( "false" ) boolean inferScheme,
                            @QueryParam( MethodParams.ROOT ) @DefaultValue( "" ) String root,
                            @QueryParam( MethodParams.ABSOLUTE ) @DefaultValue( "false" ) boolean absolute,
                            @QueryParam( MethodParams.BYPASSCACHE ) @DefaultValue( "false" ) boolean bypassCache,
                            @QueryParam( MethodParams.DEBUG ) @DefaultValue( "false" ) boolean debug,
                            @QueryParam( MethodParams.SCHEME ) @DefaultValue( "" ) String scheme,
                            @Context HttpServletRequest request,
                            @Context HttpServletResponse response ) throws IOException, ThingWriteException {

    String schemeToUse = "";
    if ( !inferScheme ) {
      schemeToUse = StringUtils.isEmpty( scheme ) ? request.getScheme() : scheme;
    }
    String filePath = getWcdfRelativePath( solution, path, file );

    CdfRunJsDashboardWriteResult dashboardWrite =
        this.loadDashboard( filePath, schemeToUse, root, absolute, bypassCache, debug, null );
    return dashboardWrite.getContent();
  }

  @GET
  @Path( "/getHeaders" )
  @Produces( "text/plain" )
  public String getHeaders( @QueryParam( MethodParams.SOLUTION ) @DefaultValue( "" ) String solution,
                            @QueryParam( MethodParams.PATH ) @DefaultValue( "" ) String path,
                            @QueryParam( MethodParams.FILE ) @DefaultValue( "" ) String file,
                            @QueryParam( MethodParams.INFERSCHEME ) @DefaultValue( "false" ) boolean inferScheme,
                            @QueryParam( MethodParams.ROOT ) @DefaultValue( "" ) String root,
                            @QueryParam( MethodParams.ABSOLUTE ) @DefaultValue( "true" ) boolean absolute,
                            @QueryParam( MethodParams.BYPASSCACHE ) @DefaultValue( "false" ) boolean bypassCache,
                            @QueryParam( MethodParams.DEBUG ) @DefaultValue( "false" ) boolean debug,
                            @QueryParam( MethodParams.SCHEME ) @DefaultValue( "" ) String scheme,
                            @Context HttpServletRequest request,
                            @Context HttpServletResponse response ) throws IOException, ThingWriteException {

    String schemeToUse = "";
    if ( !inferScheme ) {
      schemeToUse = StringUtils.isEmpty( scheme ) ? request.getScheme() : scheme;
    }
    String filePath = getWcdfRelativePath( solution, path, file );

    CdfRunJsDashboardWriteResult dashboardWrite =
        this.loadDashboard( filePath, schemeToUse, root, absolute, bypassCache, debug, null );
    return dashboardWrite.getHeader();
  }

  @GET
  @Path( "/render" )
  @Produces( MimeTypes.HTML )
  public String render( @QueryParam( MethodParams.SOLUTION ) @DefaultValue( "" ) String solution,
                        @QueryParam( MethodParams.PATH ) @DefaultValue( "" ) String path,
                        @QueryParam( MethodParams.FILE ) @DefaultValue( "" ) String file,
                        @QueryParam( MethodParams.INFERSCHEME ) @DefaultValue( "false" ) boolean inferScheme,
                        @QueryParam( MethodParams.ROOT ) @DefaultValue( "" ) String root,
                        @QueryParam( MethodParams.ABSOLUTE ) @DefaultValue( "true" ) boolean absolute,
                        @QueryParam( MethodParams.BYPASSCACHE ) @DefaultValue( "false" ) boolean bypassCache,
                        @QueryParam( MethodParams.DEBUG ) @DefaultValue( "false" ) boolean debug,
                        @QueryParam( MethodParams.SCHEME ) @DefaultValue( "" ) String scheme,
                        @QueryParam( MethodParams.VIEWID ) @DefaultValue( "" ) String viewId,
                        @QueryParam( MethodParams.STYLE ) @DefaultValue( "" ) String style,
                        @Context HttpServletRequest request ) throws IOException {

    String schemeToUse = "";
    if ( !inferScheme ) {
      schemeToUse = StringUtils.isEmpty( scheme ) ? request.getScheme() : scheme;
    }
    String filePath = getWcdfRelativePath( solution, path, file ); //FIXME Util.joinPath
    IReadAccess readAccess = Utils.getSystemOrUserReadAccess( filePath );

    if ( readAccess == null ) {
      return "Access Denied or File Not Found.";
    }

    long start = System.currentTimeMillis();
    long end;
    ILogger iLogger = getAuditLogger();
    IParameterProvider requestParams = getParameterProvider( request.getParameterMap() );

    UUID uuid = CpfAuditHelper.startAudit( getPluginName(), filePath, getObjectName(), this.getPentahoSession(),
        iLogger, requestParams );

    try {
      logger.info( "[Timing] CDE Starting Dashboard Rendering" );
      CdfRunJsDashboardWriteResult dashboard =
          loadDashboard( filePath, schemeToUse, root, absolute, bypassCache, debug, style );
      String result =
          dashboard.render( InterPluginBroker.getCdfContext( filePath, "", viewId ) ); // TODO: check new interplugin call

      //i18n token replacement
      if ( !StringUtils.isEmpty( result ) ) {
        String msgDir = FilenameUtils.getPath( FilenameUtils.separatorsToUnix( filePath ) );
        msgDir = msgDir.startsWith( Util.SEPARATOR ) ? msgDir : Util.SEPARATOR + msgDir;
        result = new MessageBundlesHelper( msgDir, readAccess, CdeEnvironment.getPluginSystemWriter(),
          getEnv().getLocale(), getEnv().getExtApi().getPluginStaticBaseUrl() )
          .replaceParameters( result, null );
      }

      logger.info( "[Timing] CDE Finished Dashboard Rendering: " + Utils.ellapsedSeconds( start ) + "s" );

      end = System.currentTimeMillis();
      CpfAuditHelper.endAudit( getPluginName(), filePath, getObjectName(),
          this.getPentahoSession(), iLogger, start, uuid, end );

      return result;
    } catch ( Exception ex ) { //TODO: better error handling?
      String msg = "Could not load dashboard: " + ex.getMessage();
      logger.error( msg, ex );

      end = System.currentTimeMillis();
      CpfAuditHelper.endAudit( getPluginName(), filePath, getObjectName(),
          this.getPentahoSession(), iLogger, start, uuid, end );
      return msg;
    }
  }

  @GET
  @Path( "/edit" )
  @Produces( MimeTypes.HTML )
  public String edit( @QueryParam( MethodParams.SOLUTION ) @DefaultValue( "" ) String solution,
                      @QueryParam( MethodParams.PATH ) @DefaultValue( "" ) String path,
                      @QueryParam( MethodParams.FILE ) @DefaultValue( "" ) String file,
                      @QueryParam( MethodParams.DEBUG ) @DefaultValue( "false" ) boolean debug,
                      @QueryParam( "isDefault" ) @DefaultValue( "false" ) boolean isDefault,
                      @Context HttpServletRequest request,
                      @Context HttpServletResponse response ) throws Exception {

    String wcdfPath = getWcdfRelativePath( solution, path, file );
    if ( Utils.getSystemOrUserReadAccess( wcdfPath ) == null ) {
      return "Access Denied to file " + wcdfPath; //TODO: keep html?
    }

    return getEditor( wcdfPath, debug, request.getScheme(), isDefault, response );
  }

  @GET
  @Path( "/new" )
  @Produces( MimeTypes.HTML )
  public String newDashboard( //TODO: change file to path; does new ever use this arg?
                              //      @QueryParam( MethodParams.SOLUTION ) @DefaultValue( "null" ) String solution,
                              @QueryParam( MethodParams.PATH ) @DefaultValue( "" ) String path,
                              //      @QueryParam( MethodParams.FILE ) @DefaultValue( "null" ) String file,
                              @QueryParam( MethodParams.DEBUG ) @DefaultValue( "false" ) boolean debug,
                              @QueryParam( "isDefault" ) @DefaultValue( "false" ) boolean isDefault,
                              @Context HttpServletRequest request,
                              @Context HttpServletResponse response ) throws Exception {

    return getEditor( path, debug, request.getScheme(), isDefault, response );
  }

  @GET
  @Path( "/listRenderers" )
  @Produces( MimeTypes.JSON )
  public String listRenderers() {
    return "{\"result\": [\""
      + DashboardWcdfDescriptor.DashboardRendererType.BLUEPRINT.getType()
      + "\",\""
      + DashboardWcdfDescriptor.DashboardRendererType.MOBILE.getType()
      + "\",\""
      + DashboardWcdfDescriptor.DashboardRendererType.BOOTSTRAP.getType()
      + "\"]}";
  }

  @GET
  @Path( "/refresh" )
  @Produces( MimeTypes.PLAIN_TEXT )
  public String refresh( @Context HttpServletResponse servletResponse ) throws Exception {
    String msg = "Refreshed CDE Successfully";

    try {
      getDashboardManager().refreshAll();
    } catch ( Exception re ) {
      msg = "Method refresh failed while trying to execute.";

      logger.error( msg, re );
      servletResponse.sendError( HttpServletResponse.SC_INTERNAL_SERVER_ERROR, msg );
    }

    return msg;
  }

  private CdfRunJsDashboardWriteResult loadDashboard( String filePath, String scheme, String root, boolean absolute,
                                                      boolean bypassCache, boolean debug, String style )
    throws ThingWriteException {

    CdfRunJsDashboardWriteOptions options =
        new CdfRunJsDashboardWriteOptions( absolute, debug, root, scheme );
    return getDashboardManager().getDashboardCdfRunJs( filePath, options, bypassCache, style );
  }

  protected DashboardManager getDashboardManager() {
    return DashboardManager.getInstance();
  }


  private String getWcdfRelativePath( String solution, String path, String file ) {
    //TODO: change to use path instead of file
    //    if ( !StringUtils.isEmpty( solution ) || !StringUtils.isEmpty( file ) ) {
    //      logger.warn( "Use of solution/path/file is deprecated. Use just the path argument" );
    return Util.joinPath( solution, path, file );
    //    }
    //    else return path;
    //    final String filePath = "/" + solution + "/" + path + "/" + file;
    //    return filePath.replaceAll( "//+", "/" );
  }

  private IPentahoSession getPentahoSession() {
    return PentahoSessionHolder.getSession();
  }

  private String getObjectName() {
    return RenderApi.class.getName();
  }

  private String getPluginName() {
    return CdeEnvironment.getPluginId();
  }

  private ILogger getAuditLogger() {
    return new SimpleLogger( RenderApi.class.getName() );
  }

  private IParameterProvider getParameterProvider( Map<String, String> params ) {
    return new SimpleParameterProvider( params );
  }

  private String getEditor( String path, boolean debug, String scheme, boolean isDefault,
                            HttpServletResponse response ) throws Exception {
    response.setContentType( MimeTypes.HTML );
    String result = DashboardEditor.getEditor( path, debug, scheme, isDefault );

    //i18n token replacement
    if ( !StringUtils.isEmpty( result ) ) {

      /* cde editor's i18n is different; it continues on relying on pentaho-cdf-dd/lang/messages.properties */

      String msgDir = Util.SEPARATOR + "lang" + Util.SEPARATOR;
      result = new MessageBundlesHelper( msgDir, CdeEnvironment.getPluginSystemReader( null ),
        CdeEnvironment.getPluginSystemWriter(), getEnv().getLocale(),
        getEnv().getExtApi().getPluginStaticBaseUrl() ).replaceParameters( result, null );
    }

    return result;
  }

  private ICdeEnvironment getEnv() {
    if ( this.privateEnviroment != null ) {
      return this.privateEnviroment;
    }
    return CdeEngine.getEnv();
  }


  private class MethodParams {
    public static final String SOLUTION = "solution";
    public static final String PATH = "path";
    public static final String FILE = "file";
    public static final String INFERSCHEME = "inferScheme";
    public static final String ABSOLUTE = "absolute";
    public static final String ROOT = "root";
    public static final String BYPASSCACHE = "bypassCache";
    public static final String DEBUG = "debug";
    public static final String VIEWID = "viewId";
    public static final String STYLE = "style";
    public static final String SCHEME = "scheme";
  }

}
