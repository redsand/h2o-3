package hex.tree.xgboost;

import hex.Model;
import hex.ScoreKeeper;
import water.util.TwoDimTable;

import java.util.ArrayList;
import java.util.List;

public class XGBoostOutput extends Model.Output implements Model.GetNTrees {
  public XGBoostOutput(XGBoost b) {
    super(b);
    _scored_train = new ScoreKeeper[]{new ScoreKeeper(Double.NaN)};
    _scored_valid = new ScoreKeeper[]{new ScoreKeeper(Double.NaN)};
  }

  int _nums;
  int _cats;
  int[] _catOffsets;
  boolean _useAllFactorLevels;
  public boolean _sparse;

  public int _ntrees;
  public ScoreKeeper[/*ntrees+1*/] _scored_train;
  public ScoreKeeper[/*ntrees+1*/] _scored_valid;
  public ScoreKeeper[] scoreKeepers() {
    List<ScoreKeeper> skl = new ArrayList<>();
    ScoreKeeper[] ska = _validation_metrics != null ? _scored_valid : _scored_train;
    for( ScoreKeeper sk : ska )
      if (!sk.isEmpty())
        skl.add(sk);
    return skl.toArray(new ScoreKeeper[0]);
  }
  public long[/*ntrees+1*/] _training_time_ms = {System.currentTimeMillis()};
  public TwoDimTable _variable_importances; // gain
  public TwoDimTable _variable_importances_cover;
  public TwoDimTable _variable_importances_frequency;
  public XgbVarImp _varimp;
  public TwoDimTable _native_parameters;

  @Override
  public int getNTrees() {
    return _ntrees;
  }
}
