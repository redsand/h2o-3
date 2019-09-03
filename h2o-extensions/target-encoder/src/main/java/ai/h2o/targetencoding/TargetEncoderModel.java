package ai.h2o.targetencoding;

import hex.Model;
import hex.ModelCategory;
import hex.ModelMetrics;
import water.Futures;
import water.H2O;
import water.Job;
import water.Key;
import water.fvec.Frame;
import water.udf.CFuncRef;
import water.util.IcedHashMapGeneric;
import water.util.TwoDimTable;

import java.util.Map;

public class TargetEncoderModel extends Model<TargetEncoderModel, TargetEncoderModel.TargetEncoderParameters, TargetEncoderModel.TargetEncoderOutput> {

  protected static final String ALGO_NAME = "TargetEncoder";

  private final TargetEncoder _targetEncoder;

  public TargetEncoderModel(Key<TargetEncoderModel> selfKey, TargetEncoderParameters parms, TargetEncoderOutput output, TargetEncoder tec) {
    super(selfKey, parms, output);
    _targetEncoder = tec;
  }

  @Override
  public ModelMetrics.MetricBuilder makeMetricBuilder(String[] domain) {
    throw H2O.unimpl("No Model Metrics for TargetEncoder.");
  }

  public static class TargetEncoderParameters extends Model.Parameters {
    public boolean _blending = false;
    public BlendingParams _blending_parameters = TargetEncoder.DEFAULT_BLENDING_PARAMS;
    public TargetEncoder.DataLeakageHandlingStrategy _data_leakage_handling = TargetEncoder.DataLeakageHandlingStrategy.None;
    
    @Override
    public String algoName() {
      return ALGO_NAME;
    }

    @Override
    public String fullName() {
      return "TargetEncoder";
    }

    @Override
    public String javaName() {
      return TargetEncoderModel.class.getName();
    }

    @Override
    public long progressUnits() {
      return 0;
    }
    
  }

  public static class TargetEncoderOutput extends Model.Output {
    
    public IcedHashMapGeneric<String, Frame> _target_encoding_map;
    public TargetEncoderParameters _parms;
    public IcedHashMapGeneric<String, Integer> _column_name_to_missing_val_presence;
    public double _prior_mean;
    
    public TargetEncoderOutput(TargetEncoderBuilder b, IcedHashMapGeneric<String, Frame> teMap, double priorMean) {
      super(b);
      _target_encoding_map = teMap;
      _parms = b._parms;
      _model_summary = constructSummary();

      _column_name_to_missing_val_presence = createMissingValuesPresenceMap();
      _prior_mean = priorMean;
    }

    private IcedHashMapGeneric<String, Integer> createMissingValuesPresenceMap() {

      IcedHashMapGeneric<String, Integer> presenceOfNAMap = new IcedHashMapGeneric<>();
      for(Map.Entry<String, Frame> entry : _target_encoding_map.entrySet()) {
        String teColumn = entry.getKey();
        Frame frameWithEncodings = entry.getValue();
        presenceOfNAMap.put(teColumn, _parms.train().vec(teColumn).cardinality() < frameWithEncodings.vec(teColumn).cardinality() ? 1 : 0);
      }
      
      return presenceOfNAMap;
    }
    
    private TwoDimTable constructSummary(){
      TwoDimTable summary = new TwoDimTable("Target Encoder model summary.", "Summary for target encoder model", new String[_names.length],
              new String[]{"Original name", "Encoded column name"}, new String[]{"string", "string"}, null, null);

      for (int i = 0; i < _names.length; i++) {
        final String originalColName = _names[i];
        if(originalColName.equals(responseName())) continue;
        
        summary.set(i, 0, originalColName);
        summary.set(i, 1, originalColName + TargetEncoder.ENCODED_COLUMN_POSTFIX);
      }

      return summary;
    }

    @Override public ModelCategory getModelCategory() {
      return ModelCategory.TargetEncoder;
    }
  }

  /**
   * Transform with noise
   * @param data Data to transform
   * @param strategy A byte value corresponding to {@link ai.h2o.targetencoding.TargetEncoder.DataLeakageHandlingStrategy}
   * @param noiseLevel Level of noise applied
   * @param useBlending If false, blending is not used when the TE map is applied.If true, blending with corresponding blending parameters are used.
   * @param blendingParams Parameters for blending. If null, blending parameters from models parameters are loaded. 
   *                       If those are not set, DEFAULT_BLENDING_PARAMS from TargetEncoder class are used.
   * @param seed
   * @return An instance of {@link Frame} with transformed data, registered in DKV.
   */
  public Frame transform(Frame data, byte strategy, double noiseLevel, final boolean useBlending, BlendingParams blendingParams,
                         long seed) {
    if(blendingParams == null) blendingParams = _parms._blending_parameters != null ? _parms._blending_parameters : TargetEncoder.DEFAULT_BLENDING_PARAMS;
    
    final TargetEncoder.DataLeakageHandlingStrategy leakageHandlingStrategy = TargetEncoder.DataLeakageHandlingStrategy.fromVal(strategy);
    return _targetEncoder.applyTargetEncoding(data, _parms._response_column, this._output._target_encoding_map, leakageHandlingStrategy,
            _parms._fold_column, useBlending, noiseLevel, false, blendingParams, seed);
  }

  /**
   * Transform with default noise of 0.01 
   * @param data Data to transform
   * @param strategy A byte value corresponding to {@link ai.h2o.targetencoding.TargetEncoder.DataLeakageHandlingStrategy}
   * @param useBlending If false, blending is not used when the TE map is applied.If true, blending with corresponding blending parameters are used.
   * @param blendingParams Parameters for blending. If null, blending parameters from models parameters are loaded. 
   *                       If those are not set, DEFAULT_BLENDING_PARAMS from TargetEncoder class are used.
   * @param seed
   * @return An instance of {@link Frame} with transformed data, registered in DKV.
   */
  public Frame transform(Frame data, byte strategy, final boolean useBlending, BlendingParams blendingParams, long seed) {
    if(blendingParams == null) blendingParams = _parms._blending_parameters != null ? _parms._blending_parameters : TargetEncoder.DEFAULT_BLENDING_PARAMS;
    
    final TargetEncoder.DataLeakageHandlingStrategy leakageHandlingStrategy = TargetEncoder.DataLeakageHandlingStrategy.fromVal(strategy);
    return _targetEncoder.applyTargetEncoding(data, _parms._response_column, this._output._target_encoding_map, leakageHandlingStrategy,
            _parms._fold_column, useBlending, false, blendingParams, seed);
  }
  
  @Override
  protected double[] score0(double data[], double preds[]){
    throw new UnsupportedOperationException("TargetEncoderModel doesn't support scoring. Use `transform()` instead.");
  }

  @Override
  public Frame score(Frame fr, String destination_key, Job j, boolean computeMetrics, CFuncRef customMetricFunc) throws IllegalArgumentException {
    final BlendingParams blendingParams = _parms._blending_parameters != null ? _parms._blending_parameters : TargetEncoder.DEFAULT_BLENDING_PARAMS;
    final TargetEncoder.DataLeakageHandlingStrategy leakageHandlingStrategy = 
            _parms._data_leakage_handling != null ? _parms._data_leakage_handling : TargetEncoder.DataLeakageHandlingStrategy.None;
    
    return _targetEncoder.applyTargetEncoding(fr, _parms._response_column, this._output._target_encoding_map, leakageHandlingStrategy,
            _parms._fold_column, _parms._blending, _parms._seed,false, Key.<Frame>make(destination_key), blendingParams);
  }
  

  @Override
  public TargetEncoderMojoWriter getMojo() {
    return new TargetEncoderMojoWriter(this);
  }

  @Override
  protected Futures remove_impl(Futures fs, boolean cascade) {
    TargetEncoderFrameHelper.encodingMapCleanUp(_output._target_encoding_map);
    return super.remove_impl(fs, cascade);
  }
}
