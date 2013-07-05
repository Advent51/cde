
package pt.webdetails.cdf.dd.model.inst;

import pt.webdetails.cdf.dd.model.core.Entity;
import pt.webdetails.cdf.dd.model.meta.MetaObject;
import pt.webdetails.cdf.dd.model.core.validation.RequiredAttributeError;
import pt.webdetails.cdf.dd.model.core.validation.ValidationException;

/**
 * @author dcleao
 */
public abstract class Instance<TM extends MetaObject> extends Entity
{
  private final TM _meta;

  @SuppressWarnings("OverridableMethodCallInConstructor")
  protected Instance(Builder<TM> builder) throws ValidationException
  {
    super(builder);

    if(builder._meta == null)
    {
      throw new ValidationException(new RequiredAttributeError("Meta"));
    }

    this._meta = builder._meta;
  }

  public TM getMeta()
  {
    return this._meta;
  }
  
  /**
   * Class to create and modify Instance instances.
   */
  public static abstract class Builder<TM extends MetaObject> extends Entity.Builder
  {
    private TM _meta;

    public TM getMeta()
    {
      return this._meta;
    }

    public Builder setMeta(TM meta)
    {
      this._meta = meta;
      return this;
    }
  }
}
