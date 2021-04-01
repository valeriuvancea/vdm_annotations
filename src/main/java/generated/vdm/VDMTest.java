package generated.vdm;

import java.lang.Exception;
import org.overture.interpreter.values.Value;
import org.overture.interpreter.values.VoidValue;
import org.vdm.annotations.VDMJavaInterface;
import org.vdm.overture.VDMTypesHelper;

public class VDMTest extends org.vdm.test.Test implements VDMJavaInterface {
  public Value bestFunctionGenerated(Value c, Value e, Value test) throws Exception {
    super.bestFunction(VDMTypesHelper.getJavaValueFromVDMValue(c, "boolean"), VDMTypesHelper.getJavaValueFromVDMValue(e, "int"), VDMTypesHelper.getJavaValueFromVDMValue(test, "java.util.List<java.lang.Integer>"));
    return new VoidValue();
  }

  public Value bestFunction2323Generated(Value c, Value e, Value test) throws Exception {
    return VDMTypesHelper.getVDMValueFromJavaValue(super.bestFunction2323(VDMTypesHelper.getJavaValueFromVDMValue(c, "boolean"), VDMTypesHelper.getJavaValueFromVDMValue(e, "int"), VDMTypesHelper.getJavaValueFromVDMValue(test, "java.util.List<java.lang.Integer>")));
  }
}
