
package pt.webdetails.cdf.dd.model.inst.reader.cdfdejs;

import pt.webdetails.cdf.dd.model.core.reader.DefaultThingReadContext;
import pt.webdetails.cdf.dd.model.meta.MetaModel;
import pt.webdetails.cdf.dd.structure.WcdfDescriptor;

/**
 * @author Duarte
 */
public class CdfdeJsReadContext extends DefaultThingReadContext
{
  private final WcdfDescriptor _wcdf;
  private final MetaModel _metaModel;
  
  public CdfdeJsReadContext(CdfdeJsThingReaderFactory factory, WcdfDescriptor wcdf, MetaModel metaModel)
  {
    super(factory);
    
    if(wcdf == null) { throw new IllegalArgumentException("wcdf"); }
    if(metaModel == null) { throw new IllegalArgumentException("metaModel"); }
    
    this._wcdf = wcdf;
    this._metaModel = metaModel;
  }
  
  public final WcdfDescriptor getWcdf()
  {
    return this._wcdf;
  }
  
  public final MetaModel getMetaModel()
  {
    return this._metaModel;
  }
}
