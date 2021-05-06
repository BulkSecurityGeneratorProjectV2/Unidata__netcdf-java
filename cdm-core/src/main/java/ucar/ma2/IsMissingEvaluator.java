package ucar.ma2;

/**
 * A mix-in interface for evaluating if a value is missing.
 * 
 * @deprecated use ucar.array.IsMissingEvaluator.
 */
@Deprecated
public interface IsMissingEvaluator {
  /** true if there may be missing data */
  boolean hasMissing();

  /** Test if val is a missing data value */
  boolean isMissing(double val);
}
