from __future__ import print_function
import sys, os
import tempfile

sys.path.insert(1, os.path.join("..","..","..",".."))
import h2o
from tests import pyunit_utils
from h2o.estimators import H2OTargetEncoderEstimator

"""
This test is used to check Rapids wrapper for java TargetEncoder
"""

def test_target_encoding_fit_method():
    print("Check fit method of the TargetEncoder class")
    targetColumnName = "survived"
    foldColumnName = "kfold_column"

    teColumns = ["home.dest", "cabin", "embarked"]
    trainingFrame = h2o.import_file(pyunit_utils.locate("smalldata/gbm_test/titanic.csv"), header=1)

    trainingFrame[targetColumnName] = trainingFrame[targetColumnName].asfactor()
    trainingFrame[foldColumnName] = trainingFrame.kfold_column(n_folds=5, seed=1234)
    
    te = H2OTargetEncoderEstimator(k = 0.7, f = 0.3, data_leakage_handling = "None")
    te.train(training_frame=trainingFrame, x=teColumns, y=targetColumnName)
    print(te)
    transformed = te.transform(frame = trainingFrame)
    
    assert transformed is not None
    print(transformed.names)
    assert transformed.ncols == trainingFrame.ncols + len(teColumns)
    for te_col in teColumns:
        assert te_col + "_te" in transformed.names
    
    assert transformed.nrows == 1309
    
    # Test fold_column proper handling + kfold data leakage strategy defined
    te = H2OTargetEncoderEstimator(k=0.7, f=0.3)
    te.train(training_frame=trainingFrame, fold_column="pclass", x=teColumns, y=targetColumnName)
    transformed = te.transform(trainingFrame, data_leakage_handling="kfold", seed = 1234)

    te.train(training_frame=trainingFrame, fold_column="pclass", x=teColumns, y=targetColumnName)
    
    assert transformed is not None
    assert transformed.nrows == 1309

    # Test MOJO download
    mojo_file = te.download_mojo(tempfile.mkdtemp())
    assert os.path.isfile(mojo_file)
    assert os.path.getsize(mojo_file) > 0

    # Argument check
    te.train(training_frame=trainingFrame, fold_column="pclass", y=targetColumnName, x=teColumns)
    
testList = [
    test_target_encoding_fit_method
]

if __name__ == "__main__":
    for test in testList: pyunit_utils.standalone_test(test)
else:
    for test in testList: test()
