
package pt.webdetails.cdf.dd.model.meta;

import pt.webdetails.cdf.dd.model.core.validation.ValidationException;

/**
 * @author dcleao
 */
public abstract class GenericComponentType extends VisualComponentType
{
  protected GenericComponentType(Builder builder, IPropertyTypeSource propSource) throws ValidationException
  {
    super(builder, propSource);
  }
  
  /**
   * Class to create and modify GenericComponentType instances.
   */
  public static abstract class Builder extends VisualComponentType.Builder
  {
    public Builder()
    {
      super();

      this.useProperty(null, "priority");
    }
    
    @Override
    public abstract GenericComponentType build(IPropertyTypeSource propSource) throws ValidationException;
  }
}
