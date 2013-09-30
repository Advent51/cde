package pt.webdetails.cdf.dd.packager.input;

import pt.webdetails.cdf.dd.packager.PathOrigin;
import pt.webdetails.cpf.repository.api.IContentAccessFactory;
import pt.webdetails.cpf.repository.api.IReadAccess;
import pt.webdetails.cpf.repository.util.RepositoryHelper;

/**
 * For keeping track of paths from static plugin system folders.
 * @see PathOrigin
 */
public class StaticSystemOrigin extends PathOrigin {

  public StaticSystemOrigin( String basePath ) {
    super( basePath );
  }

  public String getUrlPrepend( String path ) {
    // plugins from a static system folder are easily accessible
    return RepositoryHelper.appendPath( basePath, path );
  }

  public IReadAccess getReader( IContentAccessFactory factory ) {
    return factory.getPluginSystemReader( basePath );
  }
}
