package pt.webdetails.cdf.dd.api;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONException;
import org.pentaho.platform.api.engine.IPluginResourceLoader;
import org.pentaho.platform.engine.core.system.PentahoSystem;

import pt.webdetails.cdf.dd.util.CdeEnvironment;
import pt.webdetails.cdf.dd.util.GenericBasicFileFilter;
import pt.webdetails.cdf.dd.util.Utils;
import pt.webdetails.cpf.repository.api.FileAccess;
import pt.webdetails.cpf.repository.api.IBasicFile;
import pt.webdetails.cpf.repository.api.IReadAccess;
import pt.webdetails.cpf.repository.util.RepositoryHelper;
import pt.webdetails.cpf.utils.MimeTypes;

/**
 * Created with IntelliJ IDEA. User: diogomariano Date: 07/10/13
 */

@Path( "pentaho-cdf-dd/api/resources" )
public class ResourcesApi {
  private static final Log logger = LogFactory.getLog( ResourcesApi.class );

  @GET
  @Path( "/get" )
  public void getResource( @QueryParam( "path" ) @DefaultValue( "" ) String path,
      @QueryParam( "resource" ) @DefaultValue( "" ) String resource, @Context HttpServletResponse response )
    throws IOException {
    if ( path.split( "/" ).length > 2 ) {
      resource = path.replaceAll( "^/.*?/", "" );
    }
    if ( StringUtils.isEmpty( resource ) ) {
      response.sendError( HttpServletResponse.SC_NOT_FOUND );
      return;
    }
    resource = StringUtils.strip( resource, "/" );

    String[] pathSplit = resource.split( "/" );
    String[] fileName = pathSplit[pathSplit.length - 1].split( "\\." );

    String mimeType;
    try {
      final MimeTypes.FileType fileType = MimeTypes.FileType.valueOf( fileName[fileName.length - 1].toUpperCase() );
      mimeType = MimeTypes.getMimeType( fileType );
    } catch ( java.lang.IllegalArgumentException ex ) {
      mimeType = "";
    } catch ( EnumConstantNotPresentException ex ) {
      mimeType = "";
    }

    try {
      IPluginResourceLoader resLoader = PentahoSystem.get( IPluginResourceLoader.class, null );
      String maxAge = resLoader.getPluginSetting( this.getClass(), "max-age" );

      response.setHeader( "Content-Type", mimeType );
      response.setHeader( "content-disposition", "inline; filename=\"" + pathSplit[pathSplit.length - 1] + "\"" );

      if ( maxAge != null && response != null ) {
        response.setHeader( "Cache-Control", "max-age=" + maxAge );
      }

      IBasicFile file = Utils.getFileViaAppropriateReadAccess( resource );
      if ( file == null ) {
        logger.error( "resource not found:" + resource );
        response.sendError( HttpServletResponse.SC_INTERNAL_SERVER_ERROR );
        return;
      }

      IOUtils.write( IOUtils.toString( file.getContents() ), response.getOutputStream() );
    } catch ( SecurityException e ) {
      response.sendError( HttpServletResponse.SC_FORBIDDEN );
    }
  }

  @GET
  @Path( "/getCss" )
  @Produces( "text/css" )
  public void getCssResource( @QueryParam( "path" ) @DefaultValue( "" ) String path,
      @QueryParam( "resource" ) @DefaultValue( "" ) String resource, @Context HttpServletResponse response )
    throws IOException {
    getResource( path, resource, response );
  }

  @GET
  @Path( "/getJs" )
  @Produces( "text/javascript" )
  public void getJsResource( @QueryParam( "path" ) @DefaultValue( "" ) String path,
      @QueryParam( "resource" ) @DefaultValue( "" ) String resource, @Context HttpServletResponse response )
    throws IOException {
    getResource( path, resource, response );
  }

  @GET
  @Path( "/getUntyped" )
  @Produces( "text/plain" )
  public void getUntypedResource( @QueryParam( "path" ) @DefaultValue( "" ) String path,
      @QueryParam( "resource" ) @DefaultValue( "" ) String resource, @Context HttpServletResponse response )
    throws IOException {
    response.setHeader( "content-disposition", "inline" );

    getResource( path, resource, response );
  }

  @GET
  @Path( "/getImg" )
  public void getImage( @QueryParam( "path" ) @DefaultValue( "" ) String path,
      @QueryParam( "resource" ) @DefaultValue( "" ) String resource, @Context HttpServletResponse response )
    throws IOException {
    getResource( path, resource, response );
  }

  @GET
  @Path( "/res" )
  public void res( @QueryParam( "path" ) @DefaultValue( "" ) String path,
      @QueryParam( "resource" ) @DefaultValue( "" ) String resource, @Context HttpServletResponse response )
    throws Exception {
    getResource( path, resource, response );
  }

  @GET
  @Path( "/explore" )
  @Produces( "text/javascript" )
  public void exploreFolder( @QueryParam( "dir" ) String folder, @QueryParam( "fileExtensions" ) String fileExtensions,
      @QueryParam( "access" ) String access, @QueryParam( "outputType" ) String outputType,
      @Context HttpServletResponse response ) throws IOException {

    if ( outputType != null && outputType.equals( "json" ) ) {
      try {
        String result = RepositoryHelper.toJSON( folder, getFileList( folder, fileExtensions, access ) );
        IOUtils.write( result, response.getOutputStream() );
      } catch ( JSONException e ) {
        logger.error( "exploreFolder" + folder, e );
        IOUtils.write( "Error getting files in folder " + folder, response.getOutputStream() );
      }
    } else {
      String result = RepositoryHelper.toJQueryFileTree( folder, getFileList( folder, fileExtensions, access ) );
      IOUtils.write( result, response.getOutputStream() );
    }
  }

  public IBasicFile[] getFileList( String dir, final String fileExtensions, String permission ) {

    ArrayList<String> extensionsList = new ArrayList<String>();
    String[] extensions = StringUtils.split( fileExtensions, "." );
    if ( extensions != null ) {
      for ( String extension : extensions ) {
        // For some reason, in 4.5 filebased rep started to report a leading dot in extensions
        // Adding both just to be sure we don't break stuff
        extensionsList.add( "." + extension );
        extensionsList.add( extension );
      }
    }

    FileAccess fileAccess = FileAccess.parse( permission );
    if ( fileAccess == null ) {
      fileAccess = FileAccess.READ;
    }

    GenericBasicFileFilter fileFilter =
        new GenericBasicFileFilter( null, extensionsList.toArray( new String[extensionsList.size()] ), true );

    List<IBasicFile> fileList =
        CdeEnvironment.getUserContentAccess().listFiles( dir, fileFilter, IReadAccess.DEPTH_ALL );

    if ( fileList != null && fileList.size() > 0 ) {
      return fileList.toArray( new IBasicFile[fileList.size()] );
    }

    return new IBasicFile[] {};
  }

}
